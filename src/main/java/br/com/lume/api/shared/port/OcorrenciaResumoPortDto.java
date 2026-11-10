package br.com.lume.api.shared.port;

import java.time.LocalDate;
import java.util.UUID;

public record OcorrenciaResumoPortDto(
        UUID id,
        String logradouro,
        String bairro,
        String cidade,
        LocalDate dataOcorrencia,
        String faixaHorario,
        String tipoViolenciaNome,
        Integer pesoGravidade,
        String marcadorNome,
        Integer totalValidacoes
) {
}
