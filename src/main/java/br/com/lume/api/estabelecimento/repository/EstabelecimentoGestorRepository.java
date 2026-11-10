package br.com.lume.api.estabelecimento.repository;

import br.com.lume.api.estabelecimento.domain.EstabelecimentoGestor;
import br.com.lume.api.estabelecimento.domain.EstabelecimentoGestorId;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EstabelecimentoGestorRepository extends JpaRepository<EstabelecimentoGestor, EstabelecimentoGestorId> {

    Optional<EstabelecimentoGestor> findFirstByIdUsuarioIdAndAtivoTrueOrderByVinculadoEmDesc(UUID usuarioId);

    boolean existsByIdEstabelecimentoIdAndIdUsuarioIdAndAtivoTrue(UUID estabelecimentoId, UUID usuarioId);
}
