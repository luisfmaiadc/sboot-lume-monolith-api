package br.com.lume.api.estabelecimento.dto;

import br.com.lume.api.catalogo.dto.CategoriaEstabelecimentoDto;
import br.com.lume.api.shared.port.ContagemItemDto;
import br.com.lume.api.shared.port.DistribuicaoTurnoDto;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record EstabelecimentoDetalheDto(
        UUID id,
        String nome,
        CategoriaEstabelecimentoDto categoria,
        String cnpj,
        String logradouro,
        String numero,
        String bairro,
        String cidade,
        String estado,
        double latitude,
        double longitude,
        BigDecimal scoreReputacao,
        boolean verificado,
        boolean ativo,
        int totalOcorrenciasHistoricas,
        DistribuicaoTurnoDto distribuicaoTurno,
        List<ContagemItemDto> distribuicaoTipoViolencia
) {
    public EstabelecimentoDetalheDto {
        distribuicaoTipoViolencia = distribuicaoTipoViolencia != null ? List.copyOf(distribuicaoTipoViolencia) : List.of();
    }
}
