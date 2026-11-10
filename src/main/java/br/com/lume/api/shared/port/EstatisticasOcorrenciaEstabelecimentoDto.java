package br.com.lume.api.shared.port;

import java.util.Map;

public record EstatisticasOcorrenciaEstabelecimentoDto(
        int totalOcorrenciasHistoricas,
        int madrugada,
        int manha,
        int tarde,
        int noite,
        Map<Integer, Integer> distribuicaoTipoViolencia
) {
    public EstatisticasOcorrenciaEstabelecimentoDto(int totalOcorrenciasHistoricas, int madrugada, int manha, int tarde, int noite) {
        this(totalOcorrenciasHistoricas, madrugada, manha, tarde, noite, Map.of());
    }
}
