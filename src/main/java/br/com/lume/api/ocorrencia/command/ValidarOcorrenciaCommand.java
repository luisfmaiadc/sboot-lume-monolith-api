package br.com.lume.api.ocorrencia.command;

import br.com.lume.api.ocorrencia.domain.TipoValidacao;

public record ValidarOcorrenciaCommand(
        TipoValidacao tipoValidacao
) {
}
