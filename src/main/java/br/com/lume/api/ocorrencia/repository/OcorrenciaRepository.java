package br.com.lume.api.ocorrencia.repository;

import br.com.lume.api.ocorrencia.domain.FaixaHorario;
import br.com.lume.api.ocorrencia.domain.Ocorrencia;
import br.com.lume.api.ocorrencia.domain.StatusOcorrencia;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OcorrenciaRepository extends JpaRepository<Ocorrencia, UUID> {

    Optional<Ocorrencia> findByIdAndStatus(UUID id, StatusOcorrencia status);

    List<Ocorrencia> findByEstabelecimentoIdAndStatusOrderByDataOcorrenciaDescCriadoEmDesc(
            UUID estabelecimentoId,
            StatusOcorrencia status
    );

    Page<Ocorrencia> findByCriadoPorOrderByCriadoEmDesc(UUID criadoPor, Pageable pageable);

    long countByCriadoPorAndCriadoEmAfter(UUID criadoPor, OffsetDateTime instanteMinimo);

    @Query("""
            SELECT o FROM Ocorrencia o
            WHERE o.estabelecimentoId = :estabelecimentoId
              AND o.status = br.com.lume.api.ocorrencia.domain.StatusOcorrencia.PUBLICADA
              AND (:tipoViolenciaId IS NULL OR o.tipoViolenciaId = :tipoViolenciaId)
              AND (:marcadorId IS NULL OR o.marcadorVulnerabilidadeId = :marcadorId)
              AND (:faixaHorario IS NULL OR o.faixaHorario = :faixaHorario)
            ORDER BY o.dataOcorrencia DESC, o.criadoEm DESC
            """)
    Page<Ocorrencia> listarPublicadasPorEstabelecimento(
            @Param("estabelecimentoId") UUID estabelecimentoId,
            @Param("tipoViolenciaId") Integer tipoViolenciaId,
            @Param("marcadorId") Integer marcadorId,
            @Param("faixaHorario") FaixaHorario faixaHorario,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT
                        o.tipo_violencia_id AS tipoViolenciaId,
                        CAST(o.faixa_horario AS text) AS faixaHorario,
                        o.marcador_vulnerabilidade_id AS marcadorVulnerabilidadeId,
                        o.data_ocorrencia AS dataOcorrencia,
                        LEAST(o.total_validacoes, 10) AS validacoesComputadas,
                        CAST(COUNT(o.id) AS integer) AS quantidade
                    FROM ocorrencias o
                    WHERE o.status = 'PUBLICADA'
                      AND o.data_ocorrencia >= :dataMinima
                      AND ST_DWithin(
                          o.localizacao,
                          CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                          :raioMetros
                      )
                    GROUP BY
                        o.tipo_violencia_id,
                        o.faixa_horario,
                        o.marcador_vulnerabilidade_id,
                        o.data_ocorrencia,
                        LEAST(o.total_validacoes, 10)
                    """,
            nativeQuery = true
    )
    List<AgrupamentoDiagnosticoProjection> agruparPublicadasNoRaio(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("raioMetros") double raioMetros,
            @Param("dataMinima") LocalDate dataMinima
    );

    @Query(
            value = """
                    SELECT o.* FROM ocorrencias o
                    WHERE o.status = 'PUBLICADA'
                      AND o.data_ocorrencia >= :dataMinima
                      AND (:tipoViolenciaId IS NULL OR o.tipo_violencia_id = :tipoViolenciaId)
                      AND (:marcadorId IS NULL OR o.marcador_vulnerabilidade_id = :marcadorId)
                      AND (CAST(:faixaHorario AS text) IS NULL OR o.faixa_horario = CAST(:faixaHorario AS enum_faixa_horario))
                      AND ST_DWithin(
                          o.localizacao,
                          CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                          :raioMetros
                      )
                    ORDER BY o.data_ocorrencia DESC, o.criado_em DESC
                    LIMIT :limite
                    """,
            nativeQuery = true
    )
    List<Ocorrencia> listarPublicadasParaMapa(
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("raioMetros") double raioMetros,
            @Param("dataMinima") LocalDate dataMinima,
            @Param("tipoViolenciaId") Integer tipoViolenciaId,
            @Param("marcadorId") Integer marcadorId,
            @Param("faixaHorario") String faixaHorario,
            @Param("limite") int limite
    );

    @Modifying
    @Query("UPDATE Ocorrencia o SET o.totalValidacoes = o.totalValidacoes + 1, o.atualizadoEm = :agora WHERE o.id = :id")
    void incrementarTotalValidacoes(@Param("id") UUID id, @Param("agora") OffsetDateTime agora);

    @Modifying
    @Query("UPDATE Ocorrencia o SET o.totalValidacoes = CASE WHEN o.totalValidacoes > 0 THEN o.totalValidacoes - 1 ELSE 0 END, o.atualizadoEm = :agora WHERE o.id = :id")
    void decrementarTotalValidacoes(@Param("id") UUID id, @Param("agora") OffsetDateTime agora);
}
