package br.com.lume.api.shared.event;

import java.time.Instant;
import java.util.UUID;

public record ReputacaoEstabelecimentoRecalculadaEvent(
        UUID estabelecimentoId,
        Instant ocorridoEm
) {
}
