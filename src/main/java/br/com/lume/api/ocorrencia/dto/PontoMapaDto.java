package br.com.lume.api.ocorrencia.dto;

import br.com.lume.api.ocorrencia.domain.FaixaHorario;
import java.time.LocalDate;

public record PontoMapaDto(
        double latitude,
        double longitude,
        String tipoViolenciaCodigo,
        String tipoViolenciaNome,
        int pesoGravidade,
        FaixaHorario faixaHorario,
        LocalDate dataOcorrencia,
        int totalValidacoes
) {}
