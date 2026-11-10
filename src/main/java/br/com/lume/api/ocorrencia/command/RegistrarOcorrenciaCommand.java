package br.com.lume.api.ocorrencia.command;

import br.com.lume.api.ocorrencia.domain.FaixaHorario;
import br.com.lume.api.ocorrencia.domain.TipoAmbiente;
import java.time.LocalDate;
import java.util.UUID;

public record RegistrarOcorrenciaCommand(
        UUID estabelecimentoId,
        Double latitude,
        Double longitude,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        LocalDate dataOcorrencia,
        FaixaHorario faixaHorario,
        Integer tipoViolenciaId,
        Integer marcadorVulnerabilidadeId,
        TipoAmbiente tipoAmbiente
) {
}
