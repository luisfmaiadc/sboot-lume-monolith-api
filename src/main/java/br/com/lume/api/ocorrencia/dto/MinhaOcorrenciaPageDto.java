package br.com.lume.api.ocorrencia.dto;

import java.util.List;

public record MinhaOcorrenciaPageDto(
        List<MinhaOcorrenciaDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public MinhaOcorrenciaPageDto {
        content = content != null ? List.copyOf(content) : List.of();
    }
}
