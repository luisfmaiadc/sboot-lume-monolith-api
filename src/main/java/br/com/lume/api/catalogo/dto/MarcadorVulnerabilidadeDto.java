package br.com.lume.api.catalogo.dto;

public record MarcadorVulnerabilidadeDto(
        Integer id,
        String codigo,
        String nome,
        String descricao,
        boolean ativo
) {
}
