package br.com.lume.api.ocorrencia.command;

import br.com.lume.api.ocorrencia.domain.StatusOcorrencia;

public record AtualizarStatusOcorrenciaCommand(
        StatusOcorrencia status
) {
}
