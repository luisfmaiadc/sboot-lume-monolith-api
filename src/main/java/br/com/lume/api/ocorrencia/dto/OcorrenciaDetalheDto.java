package br.com.lume.api.ocorrencia.dto;

import br.com.lume.api.catalogo.dto.MarcadorVulnerabilidadeDto;
import br.com.lume.api.catalogo.dto.TipoViolenciaDto;
import br.com.lume.api.ocorrencia.domain.FaixaHorario;
import br.com.lume.api.ocorrencia.domain.TipoAmbiente;
import java.time.LocalDate;
import java.util.UUID;

public record OcorrenciaDetalheDto(
        UUID id,
        UUID estabelecimentoId,
        String estabelecimentoNome,
        double latitude,
        double longitude,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        LocalDate dataOcorrencia,
        FaixaHorario faixaHorario,
        TipoViolenciaDto tipoViolencia,
        MarcadorVulnerabilidadeDto marcadorVulnerabilidade,
        TipoAmbiente tipoAmbiente,
        int totalValidacoes,
        LocalDate dataRegistro
) {}
