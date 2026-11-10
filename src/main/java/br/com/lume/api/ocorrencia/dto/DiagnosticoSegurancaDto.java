package br.com.lume.api.ocorrencia.dto;

import br.com.lume.api.ocorrencia.domain.NivelRisco;
import br.com.lume.api.shared.port.ContagemItemDto;
import br.com.lume.api.shared.port.DistribuicaoTurnoDto;
import java.util.List;

public record DiagnosticoSegurancaDto(
        Double latitude,
        Double longitude,
        int raioMetros,
        NivelRisco nivelRisco,
        Double scoreRisco,
        int totalOcorrencias,
        DistribuicaoTurnoDto distribuicaoTurno,
        List<ContagemItemDto> distribuicaoViolencia,
        List<ContagemItemDto> distribuicaoMarcadores
) {
    public DiagnosticoSegurancaDto {
        distribuicaoViolencia = distribuicaoViolencia != null ? List.copyOf(distribuicaoViolencia) : List.of();
        distribuicaoMarcadores = distribuicaoMarcadores != null ? List.copyOf(distribuicaoMarcadores) : List.of();
    }
}
