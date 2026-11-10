package br.com.lume.api.ocorrencia.domain;

import br.com.lume.api.shared.exception.BusinessRuleException;
import br.com.lume.api.shared.exception.ForbiddenOperationException;
import br.com.lume.api.shared.util.GeoUtils;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@Entity
@Table(name = "ocorrencias")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Ocorrencia {

    private static final int DIAS_PRAZO_EXCLUSAO = 30;
    private static final int DIAS_PRAZO_VALIDACAO = 30;
    private static final ZoneId ZONA_NEGOCIO_PADRAO = ZoneId.of("America/Sao_Paulo");
    private static final Pattern PADRAO_UF = Pattern.compile("^[A-Z]{2}$");

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "estabelecimento_id")
    private UUID estabelecimentoId;

    @Column(name = "localizacao", nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point localizacao;

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

    @Column(name = "data_ocorrencia", nullable = false)
    private LocalDate dataOcorrencia;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "faixa_horario", nullable = false, columnDefinition = "enum_faixa_horario")
    private FaixaHorario faixaHorario;

    @Column(name = "tipo_violencia_id", nullable = false)
    private Integer tipoViolenciaId;

    @Column(name = "marcador_vulnerabilidade_id")
    private Integer marcadorVulnerabilidadeId;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo_ambiente", nullable = false, columnDefinition = "enum_tipo_ambiente")
    private TipoAmbiente tipoAmbiente;

    @Column(name = "total_validacoes", nullable = false, updatable = false)
    private int totalValidacoes;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status", nullable = false, columnDefinition = "enum_status_ocorrencia")
    private StatusOcorrencia status;

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

    public static Ocorrencia registrar(
            UUID estabelecimentoId,
            double latitude,
            double longitude,
            String logradouro,
            String numero,
            String bairro,
            String cidade,
            String estado,
            LocalDate dataOcorrencia,
            FaixaHorario faixaHorario,
            Integer tipoViolenciaId,
            Integer marcadorVulnerabilidadeId,
            TipoAmbiente tipoAmbiente,
            UUID criadoPor,
            OffsetDateTime agora
    ) {
        return registrar(
                estabelecimentoId,
                latitude,
                longitude,
                logradouro,
                numero,
                bairro,
                cidade,
                estado,
                dataOcorrencia,
                faixaHorario,
                tipoViolenciaId,
                marcadorVulnerabilidadeId,
                tipoAmbiente,
                criadoPor,
                agora,
                ZONA_NEGOCIO_PADRAO
        );
    }

    public static Ocorrencia registrar(
            UUID estabelecimentoId,
            double latitude,
            double longitude,
            String logradouro,
            String numero,
            String bairro,
            String cidade,
            String estado,
            LocalDate dataOcorrencia,
            FaixaHorario faixaHorario,
            Integer tipoViolenciaId,
            Integer marcadorVulnerabilidadeId,
            TipoAmbiente tipoAmbiente,
            UUID criadoPor,
            OffsetDateTime agora,
            ZoneId zonaNegocio
    ) {
        validarAgora(agora);
        if (criadoPor == null) {
            throw new BusinessRuleException("Autor da ocorrência é obrigatório.");
        }
        Point ponto = GeoUtils.criarPonto(latitude, longitude);
        String logradouroValidado = validarLogradouro(logradouro);
        String numeroValidado = validarNumeroOpcional(numero);
        String bairroValidado = validarBairro(bairro);
        String cidadeValidada = validarCidade(cidade);
        String estadoValidado = validarEstado(estado);
        validarDataOcorrencia(dataOcorrencia, agora, zonaNegocio);
        if (faixaHorario == null) {
            throw new BusinessRuleException("Faixa de horário da ocorrência é obrigatória.");
        }
        if (tipoViolenciaId == null || tipoViolenciaId <= 0) {
            throw new BusinessRuleException("Tipo de violência é obrigatório e deve ser positivo.");
        }
        if (marcadorVulnerabilidadeId != null && marcadorVulnerabilidadeId <= 0) {
            throw new BusinessRuleException("Marcador de vulnerabilidade deve ser positivo quando informado.");
        }
        if (tipoAmbiente == null) {
            throw new BusinessRuleException("Tipo de ambiente da ocorrência é obrigatório.");
        }

        Ocorrencia ocorrencia = new Ocorrencia();
        ocorrencia.id = UUID.randomUUID();
        ocorrencia.estabelecimentoId = estabelecimentoId;
        ocorrencia.localizacao = ponto;
        ocorrencia.logradouro = logradouroValidado;
        ocorrencia.numero = numeroValidado;
        ocorrencia.bairro = bairroValidado;
        ocorrencia.cidade = cidadeValidada;
        ocorrencia.estado = estadoValidado;
        ocorrencia.dataOcorrencia = dataOcorrencia;
        ocorrencia.faixaHorario = faixaHorario;
        ocorrencia.tipoViolenciaId = tipoViolenciaId;
        ocorrencia.marcadorVulnerabilidadeId = marcadorVulnerabilidadeId;
        ocorrencia.tipoAmbiente = tipoAmbiente;
        ocorrencia.totalValidacoes = 0;
        ocorrencia.status = StatusOcorrencia.PUBLICADA;
        ocorrencia.criadoEm = agora;
        ocorrencia.criadoPor = criadoPor;
        return ocorrencia;
    }

    public double getLatitude() {
        return GeoUtils.extrairLatitude(this.localizacao);
    }

    public double getLongitude() {
        return GeoUtils.extrairLongitude(this.localizacao);
    }

    public boolean isPublicada() {
        return this.status == StatusOcorrencia.PUBLICADA;
    }

    public boolean permiteExclusao(OffsetDateTime agora) {
        return this.status == StatusOcorrencia.PUBLICADA
                && agora != null
                && !agora.isAfter(this.criadoEm.plusDays(DIAS_PRAZO_EXCLUSAO));
    }

    public boolean permiteValidacao(OffsetDateTime agora) {
        return this.status == StatusOcorrencia.PUBLICADA
                && agora != null
                && (this.criadoEm == null || !agora.isAfter(this.criadoEm.plusDays(DIAS_PRAZO_VALIDACAO)));
    }

    public void excluirLogicamente(UUID usuarioSolicitanteId, OffsetDateTime agora) {
        if (usuarioSolicitanteId == null) {
            throw new BusinessRuleException("Identificador do usuário solicitante é obrigatório.");
        }
        validarAgora(agora);

        if (!this.criadoPor.equals(usuarioSolicitanteId)) {
            throw new ForbiddenOperationException("Operação negada: apenas o autor original pode excluir a ocorrência.");
        }
        if (this.status == StatusOcorrencia.REMOVIDA) {
            throw new BusinessRuleException("A ocorrência já foi removida anteriormente.");
        }
        if (this.status != StatusOcorrencia.PUBLICADA) {
            throw new BusinessRuleException("Apenas ocorrências com status PUBLICADA podem ser excluídas pelo autor.");
        }
        if (agora.isAfter(this.criadoEm.plusDays(DIAS_PRAZO_EXCLUSAO))) {
            throw new BusinessRuleException("Prazo de exclusão expirado: a ocorrência só pode ser excluída em até 30 dias após sua publicação.");
        }

        this.status = StatusOcorrencia.REMOVIDA;
        this.atualizadoPor = usuarioSolicitanteId;
        this.atualizadoEm = agora;
    }

    public void atualizarStatusPorModerador(StatusOcorrencia novoStatus, UUID moderadorId, OffsetDateTime agora) {
        if (moderadorId == null) {
            throw new BusinessRuleException("Identificador do moderador é obrigatório.");
        }
        if (novoStatus == null) {
            throw new BusinessRuleException("Novo status da ocorrência é obrigatório.");
        }
        validarAgora(agora);

        if (this.status == StatusOcorrencia.REMOVIDA) {
            throw new ForbiddenOperationException("Ocorrência removida não pode ser restaurada por moderadores.");
        }
        if (this.status == novoStatus) {
            return;
        }

        this.status = novoStatus;
        this.atualizadoPor = moderadorId;
        this.atualizadoEm = agora;
    }

    public void atualizarStatusPorAdmin(StatusOcorrencia novoStatus, UUID adminId, OffsetDateTime agora) {
        if (adminId == null) {
            throw new BusinessRuleException("Identificador do administrador é obrigatório.");
        }
        if (novoStatus == null) {
            throw new BusinessRuleException("Novo status da ocorrência é obrigatório.");
        }
        validarAgora(agora);

        if (this.status == novoStatus) {
            return;
        }

        this.status = novoStatus;
        this.atualizadoPor = adminId;
        this.atualizadoEm = agora;
    }

    private static String validarLogradouro(String logradouro) {
        if (logradouro == null || logradouro.isBlank()) {
            throw new BusinessRuleException("Logradouro da ocorrência é obrigatório.");
        }
        String logradouroTrim = logradouro.trim();
        if (logradouroTrim.length() < 2 || logradouroTrim.length() > 150) {
            throw new BusinessRuleException("Logradouro da ocorrência deve ter entre 2 e 150 caracteres.");
        }
        return logradouroTrim;
    }

    private static String validarNumeroOpcional(String numero) {
        if (numero == null || numero.isBlank()) {
            return null;
        }
        String numeroTrim = numero.trim();
        if (numeroTrim.length() > 20) {
            throw new BusinessRuleException("Número do endereço deve ter no máximo 20 caracteres.");
        }
        return numeroTrim;
    }

    private static String validarBairro(String bairro) {
        if (bairro == null || bairro.isBlank()) {
            throw new BusinessRuleException("Bairro da ocorrência é obrigatório.");
        }
        String bairroTrim = bairro.trim();
        if (bairroTrim.length() > 100) {
            throw new BusinessRuleException("Bairro da ocorrência deve ter entre 1 e 100 caracteres.");
        }
        return bairroTrim;
    }

    private static String validarCidade(String cidade) {
        if (cidade == null || cidade.isBlank()) {
            throw new BusinessRuleException("Cidade da ocorrência é obrigatória.");
        }
        String cidadeTrim = cidade.trim();
        if (cidadeTrim.length() > 100) {
            throw new BusinessRuleException("Cidade da ocorrência deve ter entre 1 e 100 caracteres.");
        }
        return cidadeTrim;
    }

    private static String validarEstado(String estado) {
        if (estado == null || estado.isBlank()) {
            throw new BusinessRuleException("Estado (UF) da ocorrência é obrigatório.");
        }
        String estadoTrim = estado.trim();
        if (!PADRAO_UF.matcher(estadoTrim).matches()) {
            throw new BusinessRuleException("Estado (UF) deve conter exatamente 2 letras maiúsculas.");
        }
        return estadoTrim;
    }

    private static void validarDataOcorrencia(LocalDate dataOcorrencia, OffsetDateTime agora, ZoneId zonaNegocio) {
        if (dataOcorrencia == null) {
            throw new BusinessRuleException("Data da ocorrência é obrigatória.");
        }
        ZoneId zonaEfetiva = zonaNegocio != null ? zonaNegocio : ZONA_NEGOCIO_PADRAO;
        LocalDate hojeNoFusoNegocio = agora.atZoneSameInstant(zonaEfetiva).toLocalDate();
        if (dataOcorrencia.isAfter(hojeNoFusoNegocio)) {
            throw new BusinessRuleException("A data da ocorrência não pode ser futura.");
        }
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
        if (!(o instanceof Ocorrencia that)) {
            return false;
        }
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}