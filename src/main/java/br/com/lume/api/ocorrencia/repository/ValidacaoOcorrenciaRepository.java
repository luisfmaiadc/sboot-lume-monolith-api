package br.com.lume.api.ocorrencia.repository;

import br.com.lume.api.ocorrencia.domain.ValidacaoOcorrencia;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ValidacaoOcorrenciaRepository extends JpaRepository<ValidacaoOcorrencia, UUID> {

    boolean existsByOcorrenciaIdAndUsuarioId(UUID ocorrenciaId, UUID usuarioId);

    boolean existsByOcorrenciaIdAndUsuarioIdAndAtivoTrue(UUID ocorrenciaId, UUID usuarioId);

    Optional<ValidacaoOcorrencia> findByOcorrenciaIdAndUsuarioId(UUID ocorrenciaId, UUID usuarioId);

    Optional<ValidacaoOcorrencia> findByOcorrenciaIdAndUsuarioIdAndAtivoTrue(UUID ocorrenciaId, UUID usuarioId);
}
