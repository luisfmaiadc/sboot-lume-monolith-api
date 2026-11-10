package br.com.lume.api.estabelecimento.api;

import br.com.lume.api.estabelecimento.dto.EstabelecimentoResumoDto;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EstabelecimentoInternalFacade {

    EstabelecimentoResumoDto buscarAtivoOuFalhar(UUID id);

    EstabelecimentoResumoDto buscarPorIdOuFalhar(UUID id);

    Optional<EstabelecimentoResumoDto> buscarPorId(UUID id);

    List<EstabelecimentoResumoDto> listarPorIds(Collection<UUID> ids);

    boolean existeAtivoPorId(UUID id);
}
