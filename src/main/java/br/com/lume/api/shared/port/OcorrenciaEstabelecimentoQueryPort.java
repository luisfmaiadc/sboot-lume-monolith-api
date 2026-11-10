package br.com.lume.api.shared.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface OcorrenciaEstabelecimentoQueryPort {

    OcorrenciaResumoPagePortDto listarPorEstabelecimento(
            UUID estabelecimentoId,
            Integer tipoViolenciaId,
            Integer marcadorId,
            String faixaHorario,
            int page,
            int size
    );

    EstatisticasOcorrenciaEstabelecimentoDto obterEstatisticasPorEstabelecimento(UUID estabelecimentoId);

    BigDecimal calcularScoreReputacao(UUID estabelecimentoId);
}
