package br.com.lume.api.catalogo.dto;

public record TipoViolenciaDto(
        Integer id,
        String codigo,
        String nome,
        int pesoGravidade,
        Integer ordemExibicao,
        boolean ativo
) {
}
