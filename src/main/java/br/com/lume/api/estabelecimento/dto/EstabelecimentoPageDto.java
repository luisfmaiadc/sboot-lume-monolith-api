package br.com.lume.api.estabelecimento.dto;

import java.util.List;

public record EstabelecimentoPageDto(
        List<EstabelecimentoResumoDto> content,
        long totalElements,
        int totalPages,
        int page,
        int size
) {
    public EstabelecimentoPageDto {
        content = content != null ? List.copyOf(content) : List.of();
    }
}
