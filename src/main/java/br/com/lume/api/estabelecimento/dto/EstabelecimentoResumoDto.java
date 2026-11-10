package br.com.lume.api.estabelecimento.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record EstabelecimentoResumoDto(
        UUID id,
        Integer categoriaId,
        String categoriaNome,
        String nome,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        double latitude,
        double longitude,
        BigDecimal scoreReputacao,
        boolean verificado,
        boolean ativo
) {
}
