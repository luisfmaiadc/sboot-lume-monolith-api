package br.com.lume.api.identidade.domain;

import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.UnauthorizedException;
import br.com.lume.api.shared.util.CpfValidator;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "usuarios")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Usuario {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "nome_completo", nullable = false, length = 150)
    private String nomeCompleto;

    @Column(name = "cpf_hash", nullable = false, unique = true, length = 64, updatable = false)
    private String cpfHash;

    @Convert(converter = CpfCryptoConverter.class)
    @Column(name = "cpf_cifrado", nullable = false, length = 512, updatable = false)
    private String cpf;

    @Column(name = "telefone", nullable = false, unique = true, length = 20)
    private String telefone;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "genero", nullable = false, columnDefinition = "enum_genero")
    private Genero genero;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "raca_etnia", nullable = false, columnDefinition = "enum_raca_etnia")
    private RacaEtnia racaEtnia;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "orientacao_sexual", nullable = false, columnDefinition = "enum_orientacao_sexual")
    private OrientacaoSexual orientacaoSexual;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @CreatedBy
    @Column(name = "criado_por")
    private UUID criadoPor;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private OffsetDateTime atualizadoEm;

    @LastModifiedBy
    @Column(name = "atualizado_por")
    private UUID atualizadoPor;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Set<UsuarioPerfil> perfis = new HashSet<>();

    public static Usuario criar(
            String nomeCompleto,
            String cpf,
            String cpfHash,
            String telefone,
            Genero genero,
            RacaEtnia racaEtnia,
            OrientacaoSexual orientacaoSexual,
            OffsetDateTime agora
    ) {
        String nomeValidado = validarNomeCompleto(nomeCompleto);
        CpfValidator.validar(cpf);
        String telefoneValidado = normalizarTelefone(telefone);
        if (cpfHash == null || cpfHash.isBlank()) {
            throw new BusinessRuleException("Hash do CPF é obrigatório.");
        }
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória para criação do usuário.");
        }

        Usuario usuario = new Usuario();
        usuario.id = UUID.randomUUID();
        usuario.nomeCompleto = nomeValidado;
        usuario.cpf = cpf;
        usuario.cpfHash = cpfHash;
        usuario.telefone = telefoneValidado;
        usuario.ativo = true;
        usuario.genero = genero != null ? genero : Genero.PREFIRO_NAO_INFORMAR;
        usuario.racaEtnia = racaEtnia != null ? racaEtnia : RacaEtnia.PREFIRO_NAO_INFORMAR;
        usuario.orientacaoSexual = orientacaoSexual != null ? orientacaoSexual : OrientacaoSexual.PREFIRO_NAO_INFORMAR;
        usuario.criadoEm = agora;
        usuario.criadoPor = usuario.id;
        return usuario;
    }

    public void adicionarPerfil(Perfil perfil, OffsetDateTime agora, UUID atribuidoPor) {
        if (perfil == null) {
            throw new BusinessRuleException("Perfil não pode ser nulo.");
        }
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória para atribuição de perfil.");
        }
        if (!possuiPerfil(perfil.getNome())) {
            this.perfis.add(UsuarioPerfil.vincular(this, perfil, agora, atribuidoPor));
        }
    }

    public List<String> getNomesPerfis() {
        return perfis.stream()
                .map(up -> up.getPerfil().getNome())
                .sorted()
                .toList();
    }

    public boolean possuiPerfil(String nomePerfil) {
        if (nomePerfil == null || nomePerfil.isBlank()) {
            return false;
        }
        return perfis.stream()
                .anyMatch(up -> up.getPerfil() != null && nomePerfil.equals(up.getPerfil().getNome()));
    }

    public void atualizarDadosCadastrais(
            String nomeCompleto,
            String telefone,
            Genero genero,
            RacaEtnia racaEtnia,
            OrientacaoSexual orientacaoSexual,
            OffsetDateTime agora
    ) {
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória para atualização cadastral.");
        }
        if (nomeCompleto != null) {
            this.nomeCompleto = validarNomeCompleto(nomeCompleto);
        }
        if (telefone != null) {
            this.telefone = normalizarTelefone(telefone);
        }
        if (genero != null) {
            this.genero = genero;
        }
        if (racaEtnia != null) {
            this.racaEtnia = racaEtnia;
        }
        if (orientacaoSexual != null) {
            this.orientacaoSexual = orientacaoSexual;
        }
        this.atualizadoEm = agora;
        this.atualizadoPor = this.id;
    }

    public void validarContaAtiva() {
        if (!this.ativo) {
            throw new UnauthorizedException("Conta de usuário está inativa.");
        }
    }

    private static String validarNomeCompleto(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.isBlank()) {
            throw new BusinessRuleException("Nome completo é obrigatório.");
        }
        String nomeTrim = nomeCompleto.trim();
        if (nomeTrim.length() < 3 || nomeTrim.length() > 150) {
            throw new BusinessRuleException("Nome completo deve ter entre 3 e 150 caracteres.");
        }
        return nomeTrim;
    }

    public static String normalizarTelefone(String telefone) {
        if (telefone == null || telefone.isBlank()) {
            throw new BusinessRuleException("Telefone é obrigatório.");
        }
        String digitos = telefone.replaceAll("\\D", "");
        if (digitos.length() < 10 || digitos.length() > 11) {
            throw new BusinessRuleException("Telefone deve conter entre 10 e 11 dígitos numéricos.");
        }
        return digitos;
    }
}