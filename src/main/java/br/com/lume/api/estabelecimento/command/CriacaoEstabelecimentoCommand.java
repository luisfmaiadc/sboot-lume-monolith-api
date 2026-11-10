package br.com.lume.api.estabelecimento.command;

public record CriacaoEstabelecimentoCommand(
        String nome,
        Integer categoriaId,
        String cnpj,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        Double latitude,
        Double longitude
) {
}
