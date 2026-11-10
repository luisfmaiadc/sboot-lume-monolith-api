package br.com.lume.api.ocorrencia.domain;

import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ConflictException;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "validacoes_ocorrencias")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ValidacaoOcorrencia {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "ocorrencia_id", nullable = false, updatable = false)
    private UUID ocorrenciaId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_validacao", nullable = false, columnDefinition = "enum_tipo_validacao")
    private TipoValidacao tipoValidacao;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @Column(name = "criado_por", nullable = false, updatable = false)
    private UUID criadoPor;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private OffsetDateTime atualizadoEm;

    @LastModifiedBy
    @Column(name = "atualizado_por")
    private UUID atualizadoPor;

    public static ValidacaoOcorrencia criar(
            Ocorrencia ocorrencia,
            UUID usuarioValidadorId,
            TipoValidacao tipoValidacao,
            OffsetDateTime agora
    ) {
        if (ocorrencia == null) {
            throw new BusinessRuleException("Ocorrência a ser validada é obrigatória.");
        }
        if (usuarioValidadorId == null) {
            throw new BusinessRuleException("Identificador do usuário validador é obrigatório.");
        }
        if (tipoValidacao == null) {
            throw new BusinessRuleException("Tipo de validação comunitária é obrigatório.");
        }
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória.");
        }
        if (!ocorrencia.isPublicada()) {
            throw new BusinessRuleException("Não é possível validar uma ocorrência removida.");
        }
        if (!ocorrencia.permiteValidacao(agora)) {
            throw new BusinessRuleException("Ocorrências só podem receber validações comunitárias em até 30 dias após seu registro.");
        }
        if (ocorrencia.getCriadoPor().equals(usuarioValidadorId)) {
            throw new ConflictException("Operação negada: o autor não pode validar sua própria ocorrência.");
        }

        ValidacaoOcorrencia validacao = new ValidacaoOcorrencia();
        validacao.id = UUID.randomUUID();
        validacao.ocorrenciaId = ocorrencia.getId();
        validacao.usuarioId = usuarioValidadorId;
        validacao.tipoValidacao = tipoValidacao;
        validacao.ativo = true;
        validacao.criadoEm = agora;
        validacao.criadoPor = usuarioValidadorId;
        return validacao;
    }

    public void inativar(UUID usuarioAtualizadorId, OffsetDateTime agora) {
        if (!this.ativo) {
            throw new BusinessRuleException("Validação comunitária já está inativa.");
        }
        if (usuarioAtualizadorId == null) {
            throw new BusinessRuleException("Identificador do usuário atualizador é obrigatório.");
        }
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória.");
        }
        this.ativo = false;
        this.atualizadoPor = usuarioAtualizadorId;
        this.atualizadoEm = agora;
    }

    public void reativar(TipoValidacao novoTipo, UUID usuarioAtualizadorId, OffsetDateTime agora) {
        if (novoTipo == null) {
            throw new BusinessRuleException("Tipo de validação comunitária é obrigatório.");
        }
        if (usuarioAtualizadorId == null) {
            throw new BusinessRuleException("Identificador do usuário atualizador é obrigatório.");
        }
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória.");
        }
        this.tipoValidacao = novoTipo;
        this.ativo = true;
        this.atualizadoPor = usuarioAtualizadorId;
        this.atualizadoEm = agora;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ValidacaoOcorrencia that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}