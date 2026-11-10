package br.com.lume.api.estabelecimento.repository;

import br.com.lume.api.estabelecimento.domain.Estabelecimento;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EstabelecimentoRepository extends JpaRepository<Estabelecimento, UUID> {

    boolean existsByCnpj(String cnpj);

    boolean existsByCnpjAndIdNot(String cnpj, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Estabelecimento e WHERE e.id = :id")
    Optional<Estabelecimento> findByIdWithLock(@Param("id") UUID id);

    @Query("""
            SELECT e FROM Estabelecimento e
            WHERE e.ativo = true
              AND LOWER(e.nome) LIKE LOWER(CONCAT('%', :termo, '%'))
              AND (cast(:cidade as String) IS NULL OR LOWER(e.cidade) = LOWER(cast(:cidade as String)))
            ORDER BY e.verificado DESC, e.scoreReputacao DESC, e.nome ASC
            """)
    List<Estabelecimento> buscarAutocomplete(
            @Param("termo") String termo,
            @Param("cidade") String cidade,
            Pageable pageable
    );

    @Query("""
            SELECT e FROM Estabelecimento e
            WHERE e.ativo = true
              AND (cast(:termo as String) IS NULL OR LOWER(e.nome) LIKE LOWER(CONCAT('%', cast(:termo as String), '%'))
                   OR LOWER(e.logradouro) LIKE LOWER(CONCAT('%', cast(:termo as String), '%')))
              AND (:categoriaId IS NULL OR e.categoriaId = :categoriaId)
              AND (cast(:cidade as String) IS NULL OR LOWER(e.cidade) = LOWER(cast(:cidade as String)))
              AND (cast(:bairro as String) IS NULL OR LOWER(e.bairro) = LOWER(cast(:bairro as String)))
            ORDER BY e.verificado DESC, e.scoreReputacao DESC, e.nome ASC
            """)
    Page<Estabelecimento> listarComFiltros(
            @Param("termo") String termo,
            @Param("categoriaId") Integer categoriaId,
            @Param("cidade") String cidade,
            @Param("bairro") String bairro,
            Pageable pageable
    );

    @Query(
            value = """
                    SELECT e.* FROM estabelecimentos e
                    WHERE e.ativo = true
                      AND (CAST(:termo AS text) IS NULL OR e.nome ILIKE CONCAT('%', CAST(:termo AS text), '%')
                           OR e.logradouro ILIKE CONCAT('%', CAST(:termo AS text), '%'))
                      AND (:categoriaId IS NULL OR e.categoria_id = :categoriaId)
                      AND (CAST(:cidade AS text) IS NULL OR LOWER(e.cidade) = LOWER(CAST(:cidade AS text)))
                      AND (CAST(:bairro AS text) IS NULL OR LOWER(e.bairro) = LOWER(CAST(:bairro AS text)))
                      AND ST_DWithin(
                          e.localizacao,
                          CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                          :raioMetros
                      )
                    ORDER BY ST_Distance(
                          e.localizacao,
                          CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography)
                      ) ASC, e.score_reputacao DESC
                    """,
            countQuery = """
                    SELECT COUNT(*) FROM estabelecimentos e
                    WHERE e.ativo = true
                      AND (CAST(:termo AS text) IS NULL OR e.nome ILIKE CONCAT('%', CAST(:termo AS text), '%')
                           OR e.logradouro ILIKE CONCAT('%', CAST(:termo AS text), '%'))
                      AND (:categoriaId IS NULL OR e.categoria_id = :categoriaId)
                      AND (CAST(:cidade AS text) IS NULL OR LOWER(e.cidade) = LOWER(CAST(:cidade AS text)))
                      AND (CAST(:bairro AS text) IS NULL OR LOWER(e.bairro) = LOWER(CAST(:bairro AS text)))
                      AND ST_DWithin(
                          e.localizacao,
                          CAST(ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326) AS geography),
                          :raioMetros
                      )
                    """,
            nativeQuery = true
    )
    Page<Estabelecimento> listarComFiltrosEspaciais(
            @Param("termo") String termo,
            @Param("categoriaId") Integer categoriaId,
            @Param("cidade") String cidade,
            @Param("bairro") String bairro,
            @Param("latitude") double latitude,
            @Param("longitude") double longitude,
            @Param("raioMetros") double raioMetros,
            Pageable pageable
    );
}