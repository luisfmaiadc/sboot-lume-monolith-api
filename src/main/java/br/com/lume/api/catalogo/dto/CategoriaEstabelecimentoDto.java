package br.com.lume.api.catalogo.dto;

public record CategoriaEstabelecimentoDto(
        Integer id,
        String codigo,
        String nome,
        boolean ativo
) {
}
