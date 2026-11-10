package br.com.lume.api.ocorrencia.dto;

import br.com.lume.api.ocorrencia.domain.FaixaHorario;
import br.com.lume.api.ocorrencia.domain.TipoAmbiente;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record MinhaOcorrenciaDto(
        UUID id,
        UUID estabelecimentoId,
        String estabelecimentoNome,
        String logradouro,
        String bairro,
        String cidade,
        String estado,
        LocalDate dataOcorrencia,
        FaixaHorario faixaHorario,
        String tipoViolenciaNome,
        TipoAmbiente tipoAmbiente,
        Integer totalValidacoes,
        String status,
        boolean permiteExclusao,
        OffsetDateTime criadoEm
) {
}
