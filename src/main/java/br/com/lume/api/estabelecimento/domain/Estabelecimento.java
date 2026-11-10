package br.com.lume.api.estabelecimento.domain;

import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.util.CnpjValidator;
import br.com.lume.api.shared.util.GeoUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "estabelecimentos")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Estabelecimento {

    private static final BigDecimal SCORE_MINIMO = new BigDecimal("0.00");
    private static final BigDecimal SCORE_MAXIMO = new BigDecimal("5.00");
    private static final Pattern PADRAO_UF = Pattern.compile("^[A-Z]{2}$");

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "categoria_id", nullable = false)
    private Integer categoriaId;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "cnpj", unique = true, length = 14)
    private String cnpj;

    @Column(name = "logradouro", nullable = false, length = 150)
    private String logradouro;

    @Column(name = "numero", length = 20)
    private String numero;

    @Column(name = "bairro", nullable = false, length = 100)
    private String bairro;

    @Column(name = "cidade", nullable = false, length = 100)
    private String cidade;

    @Column(name = "estado", nullable = false, length = 2)
    private String estado;

    @Column(name = "localizacao", nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point localizacao;

    @Column(name = "score_reputacao", nullable = false, precision = 3, scale = 2)
    private BigDecimal scoreReputacao;

    @Column(name = "verificado", nullable = false)
    private boolean verificado;

    @Column(name = "ativo", nullable = false)
    private boolean ativo;

    @CreatedDate
    @Column(name = "criado_em", nullable = false, updatable = false)
    private OffsetDateTime criadoEm;

    @CreatedBy
    @Column(name = "criado_por", updatable = false)
    private UUID criadoPor;

    @LastModifiedDate
    @Column(name = "atualizado_em")
    private OffsetDateTime atualizadoEm;

    @LastModifiedBy
    @Column(name = "atualizado_por")
    private UUID atualizadoPor;

    public static Estabelecimento criar(
            Integer categoriaId,
            String nome,
            String cnpj,
            String logradouro,
            String numero,
            String bairro,
            String cidade,
            String estado,
            double latitude,
            double longitude,
            UUID criadoPor,
            OffsetDateTime agora
    ) {
        validarCategoriaId(categoriaId);
        String nomeValidado = validarNome(nome);
        String cnpjValidado = validarCnpjOpcional(cnpj);
        String logradouroValidado = validarLogradouro(logradouro);
        String numeroValidado = validarNumeroOpcional(numero);
        String bairroValidado = validarBairro(bairro);
        String cidadeValidada = validarCidade(cidade);
        String estadoValidado = validarEstado(estado);
        Point ponto = GeoUtils.criarPonto(latitude, longitude);
        validarAgora(agora);

        Estabelecimento estabelecimento = new Estabelecimento();
        estabelecimento.id = UUID.randomUUID();
        estabelecimento.categoriaId = categoriaId;
        estabelecimento.nome = nomeValidado;
        estabelecimento.cnpj = cnpjValidado;
        estabelecimento.logradouro = logradouroValidado;
        estabelecimento.numero = numeroValidado;
        estabelecimento.bairro = bairroValidado;
        estabelecimento.cidade = cidadeValidada;
        estabelecimento.estado = estadoValidado;
        estabelecimento.localizacao = ponto;
        estabelecimento.scoreReputacao = SCORE_MAXIMO;
        estabelecimento.verificado = false;
        estabelecimento.ativo = true;
        estabelecimento.criadoEm = agora;
        estabelecimento.criadoPor = criadoPor;
        return estabelecimento;
    }

    public double getLatitude() {
        return GeoUtils.extrairLatitude(this.localizacao);
    }

    public double getLongitude() {
        return GeoUtils.extrairLongitude(this.localizacao);
    }

    public void atualizarDadosCadastrais(
            String nome,
            Integer categoriaId,
            String cnpj,
            String logradouro,
            String numero,
            String bairro,
            String cidade,
            String estado,
            Boolean ativo,
            UUID atualizadoPor,
            OffsetDateTime agora
    ) {
        validarAgora(agora);
        validarCategoriaId(categoriaId);
        this.nome = validarNome(nome);
        this.categoriaId = categoriaId;
        this.cnpj = validarCnpjOpcional(cnpj);
        this.logradouro = validarLogradouro(logradouro);
        this.numero = validarNumeroOpcional(numero);
        this.bairro = validarBairro(bairro);
        this.cidade = validarCidade(cidade);
        this.estado = validarEstado(estado);
        if (ativo != null) {
            this.ativo = ativo;
        }
        this.atualizadoPor = atualizadoPor;
        this.atualizadoEm = agora;
    }

    public void atualizarScoreReputacao(BigDecimal novoScore, OffsetDateTime agora) {
        if (novoScore == null) {
            throw new BusinessRuleException("Novo score de reputação é obrigatório.");
        }
        validarAgora(agora);

        BigDecimal scoreArredondado = novoScore.setScale(2, RoundingMode.HALF_UP);
        if (scoreArredondado.compareTo(SCORE_MINIMO) < 0 || scoreArredondado.compareTo(SCORE_MAXIMO) > 0) {
            throw new BusinessRuleException("Score de reputação deve estar entre 0.00 e 5.00.");
        }

        this.scoreReputacao = scoreArredondado;
        this.atualizadoEm = agora;
    }

    public void marcarComoVerificado(UUID atualizadoPor, OffsetDateTime agora) {
        validarAgora(agora);
        this.verificado = true;
        this.atualizadoPor = atualizadoPor;
        this.atualizadoEm = agora;
    }

    private static void validarCategoriaId(Integer categoriaId) {
        if (categoriaId == null || categoriaId <= 0) {
            throw new BusinessRuleException("Categoria do estabelecimento é obrigatória e deve ser positiva.");
        }
    }

    private static String validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new BusinessRuleException("Nome do estabelecimento é obrigatório.");
        }
        String nomeTrim = nome.trim();
        if (nomeTrim.length() < 3 || nomeTrim.length() > 150) {
            throw new BusinessRuleException("Nome do estabelecimento deve ter entre 3 e 150 caracteres.");
        }
        return nomeTrim;
    }

    private static String validarCnpjOpcional(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return null;
        }
        String cnpjNormalizado = cnpj.trim().toUpperCase(Locale.ROOT);
        CnpjValidator.validar(cnpjNormalizado);
        return cnpjNormalizado;
    }

    private static String validarLogradouro(String logradouro) {
        if (logradouro == null || logradouro.isBlank()) {
            throw new BusinessRuleException("Logradouro do estabelecimento é obrigatório.");
        }
        String logradouroTrim = logradouro.trim();
        if (logradouroTrim.length() < 2 || logradouroTrim.length() > 150) {
            throw new BusinessRuleException("Logradouro do estabelecimento deve ter entre 2 e 150 caracteres.");
        }
        return logradouroTrim;
    }

    private static String validarNumeroOpcional(String numero) {
        if (numero == null || numero.isBlank()) {
            return null;
        }
        String numeroTrim = numero.trim();
        if (numeroTrim.length() > 20) {
            throw new BusinessRuleException("Número do estabelecimento deve ter no máximo 20 caracteres.");
        }
        return numeroTrim;
    }

    private static String validarBairro(String bairro) {
        if (bairro == null || bairro.isBlank()) {
            throw new BusinessRuleException("Bairro do estabelecimento é obrigatório.");
        }
        String bairroTrim = bairro.trim();
        if (bairroTrim.length() > 100) {
            throw new BusinessRuleException("Bairro do estabelecimento deve ter entre 1 e 100 caracteres.");
        }
        return bairroTrim;
    }

    private static String validarCidade(String cidade) {
        if (cidade == null || cidade.isBlank()) {
            throw new BusinessRuleException("Cidade do estabelecimento é obrigatória.");
        }
        String cidadeTrim = cidade.trim();
        if (cidadeTrim.length() > 100) {
            throw new BusinessRuleException("Cidade do estabelecimento deve ter entre 1 e 100 caracteres.");
        }
        return cidadeTrim;
    }

    private static String validarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            throw new BusinessRuleException("Estado (UF) do estabelecimento é obrigatório.");
        }
        String estadoTrim = estado.trim();
        if (!PADRAO_UF.matcher(estadoTrim).matches()) {
            throw new BusinessRuleException("Estado (UF) deve conter exatamente 2 letras maiúsculas.");
        }
        return estadoTrim;
    }

    private static void validarAgora(OffsetDateTime agora) {
        if (agora == null) {
            throw new BusinessRuleException("Referência temporal é obrigatória.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Estabelecimento that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}