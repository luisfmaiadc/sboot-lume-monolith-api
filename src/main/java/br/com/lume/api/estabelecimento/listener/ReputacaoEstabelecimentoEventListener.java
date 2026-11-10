package br.com.lume.api.estabelecimento.listener;

import br.com.lume.api.estabelecimento.domain.Estabelecimento;
import br.com.lume.api.estabelecimento.repository.EstabelecimentoRepository;
import br.com.lume.api.shared.event.ReputacaoEstabelecimentoRecalculadaEvent;
import br.com.lume.api.shared.port.OcorrenciaEstabelecimentoQueryPort;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReputacaoEstabelecimentoEventListener {

    private final EstabelecimentoRepository estabelecimentoRepository;
    private final OcorrenciaEstabelecimentoQueryPort ocorrenciaEstabelecimentoQueryPort;
    private final Clock clock;

    @ApplicationModuleListener
    public void on(ReputacaoEstabelecimentoRecalculadaEvent event) {
        if (event == null || event.estabelecimentoId() == null) {
            return;
        }

        log.info("Evento ReputacaoEstabelecimentoRecalculadaEvent recebido: estabelecimentoId={}", event.estabelecimentoId());

        Optional<Estabelecimento> optional = estabelecimentoRepository.findByIdWithLock(event.estabelecimentoId());
        if (optional.isEmpty()) {
            log.warn("Estabelecimento não encontrado ao processar evento de reputação: estabelecimentoId={}", event.estabelecimentoId());
            return;
        }

        BigDecimal novoScore = ocorrenciaEstabelecimentoQueryPort.calcularScoreReputacao(event.estabelecimentoId());
        Estabelecimento estabelecimento = optional.get();
        estabelecimento.atualizarScoreReputacao(novoScore, OffsetDateTime.now(clock));
        estabelecimentoRepository.save(estabelecimento);

        log.info("Score de reputação atualizado com sucesso: estabelecimentoId={}, novoScore={}",
                estabelecimento.getId(), estabelecimento.getScoreReputacao());
    }
}