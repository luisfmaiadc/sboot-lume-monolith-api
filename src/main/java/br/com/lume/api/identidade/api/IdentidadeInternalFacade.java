package br.com.lume.api.identidade.api;

import br.com.lume.api.identidade.dto.UsuarioResumoDto;
import java.util.Optional;
import java.util.UUID;

public interface IdentidadeInternalFacade {

    Optional<UsuarioResumoDto> buscarResumoPorId(UUID usuarioId);

    boolean usuarioPossuiPerfil(UUID usuarioId, String nomePerfil);

    void adquirirLockParaUsuario(UUID usuarioId);
}
