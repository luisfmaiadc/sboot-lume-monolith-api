package br.com.lume.api.identidade.domain;

import br.com.lume.api.shared.exception.AccountLockedException;
import br.com.lume.api.shared.exception.BusinessRuleException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "credenciais")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Credencial {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "hash_senha", nullable = false, length = 255)
    private String hashSenha;

    @Column(name = "tentativas_falhas", nullable = false)
    private int tentativasFalhas;

    @Column(name = "bloqueado_ate")
    private OffsetDateTime bloqueadoAte;

    @Column(name = "ultimo_login_em")
    private OffsetDateTime ultimoLoginEm;

    @Column(name = "requer_redefinicao_senha", nullable = false)
    private boolean requerRedefinicaoSenha;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private OffsetDateTime atualizadoEm;

    public static Credencial criar(
            Usuario usuario,
            String email,
            String hashSenha,
            OffsetDateTime agora
    ) {
        if (usuario == null) {
            throw new BusinessRuleException("Usuário é obrigatório para criação da credencial.");
        }
        if (email == null || email.isBlank()) {
            throw new BusinessRuleException("E-mail é obrigatório.");
        }
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        int atIndex = emailNormalizado.indexOf('@');
        if (atIndex <= 0 || atIndex == emailNormalizado.length() - 1 || emailNormalizado.length() > 255) {
            throw new BusinessRuleException("E-mail informado é inválido.");
        }
        if (hashSenha == null || hashSenha.isBlank()) {
            throw new BusinessRuleException("Hash de senha é obrigatório.");
        }
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória para criação da credencial.");
        }

        Credencial credencial = new Credencial();
        credencial.id = UUID.randomUUID();
        credencial.usuario = usuario;
        credencial.email = emailNormalizado;
        credencial.hashSenha = hashSenha;
        credencial.tentativasFalhas = 0;
        credencial.requerRedefinicaoSenha = false;
        credencial.criadoEm = agora;
        return credencial;
    }

    public boolean isBloqueada(OffsetDateTime agora) {
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória para verificação de bloqueio.");
        }
        return bloqueadoAte != null && agora.isBefore(bloqueadoAte);
    }

    public void verificarBloqueio(OffsetDateTime agora) {
        if (isBloqueada(agora)) {
            throw new AccountLockedException("Conta temporariamente bloqueada por excesso de tentativas falhas.");
        }
    }

    public void registrarFalhaLogin(
            OffsetDateTime agora,
            int maxTentativas,
            int minutosBloqueio,
            int tentativasAposDesbloqueio
    ) {
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória para registrar falha de login.");
        }
        if (maxTentativas <= 0 || minutosBloqueio <= 0 || tentativasAposDesbloqueio <= 0) {
            throw new BusinessRuleException("Parâmetros de política de bloqueio devem ser positivos.");
        }

        if (bloqueadoAte != null && !agora.isBefore(bloqueadoAte)) {
            this.bloqueadoAte = null;
            this.tentativasFalhas = Math.max(0, maxTentativas - tentativasAposDesbloqueio);
        }

        this.tentativasFalhas++;
        if (this.tentativasFalhas >= maxTentativas) {
            this.bloqueadoAte = agora.plusMinutes(minutosBloqueio);
        }
        this.atualizadoEm = agora;
    }

    public void registrarSucessoLogin(OffsetDateTime agora) {
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória para registrar sucesso de login.");
        }
        verificarBloqueio(agora);
        this.tentativasFalhas = 0;
        this.bloqueadoAte = null;
        this.ultimoLoginEm = agora;
        this.atualizadoEm = agora;
    }
}