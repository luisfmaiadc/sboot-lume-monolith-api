package br.com.lume.api.shared.port;

public record ContagemItemDto(
        Integer id,
        String codigo,
        String nome,
        Integer total
) {}
