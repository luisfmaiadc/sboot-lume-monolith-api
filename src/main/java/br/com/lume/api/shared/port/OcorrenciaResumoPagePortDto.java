package br.com.lume.api.shared.port;

import java.util.List;

public record OcorrenciaResumoPagePortDto(
        List<OcorrenciaResumoPortDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public OcorrenciaResumoPagePortDto {
        content = content != null ? List.copyOf(content) : List.of();
    }
}
