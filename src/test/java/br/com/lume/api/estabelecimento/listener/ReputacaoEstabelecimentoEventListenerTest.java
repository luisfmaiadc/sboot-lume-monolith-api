package br.com.lume.api.estabelecimento.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import br.com.lume.api.estabelecimento.domain.Estabelecimento;
import br.com.lume.api.estabelecimento.repository.EstabelecimentoRepository;
import br.com.lume.api.shared.event.ReputacaoEstabelecimentoRecalculadaEvent;
import br.com.lume.api.shared.port.OcorrenciaEstabelecimentoQueryPort;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReputacaoEstabelecimentoEventListener - Testes Unitários")
class ReputacaoEstabelecimentoEventListenerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final OffsetDateTime AGORA = OffsetDateTime.now(FIXED_CLOCK);

    @Mock
    private EstabelecimentoRepository estabelecimentoRepository;

    @Mock
    private OcorrenciaEstabelecimentoQueryPort ocorrenciaEstabelecimentoQueryPort;

    private ReputacaoEstabelecimentoEventListener listener;

    @BeforeEach
    void setUp() {
        listener = new ReputacaoEstabelecimentoEventListener(
                estabelecimentoRepository,
                ocorrenciaEstabelecimentoQueryPort,
                FIXED_CLOCK
        );
    }

    @Nested
    @DisplayName("on(ReputacaoEstabelecimentoRecalculadaEvent)")
    class OnEvent {

        @Test
        @DisplayName("Deve atualizar score de reputação do estabelecimento e persistir no repositório")
        void deveAtualizarScoreQuandoEstabelecimentoExistir() {
            Estabelecimento estabelecimento = Estabelecimento.criar(
                    1,
                    "Bar do Zé",
                    null,
                    "Rua Augusta",
                    "1200",
                    "Consolação",
                    "São Paulo",
                    "SP",
                    -23.55052,
                    -46.633308,
                    UUID.randomUUID(),
                    AGORA.minusDays(1)
            );
            ReputacaoEstabelecimentoRecalculadaEvent event = new ReputacaoEstabelecimentoRecalculadaEvent(
                    estabelecimento.getId(),
                    FIXED_INSTANT
            );
            when(estabelecimentoRepository.findByIdWithLock(estabelecimento.getId()))
                    .thenReturn(Optional.of(estabelecimento));
            when(ocorrenciaEstabelecimentoQueryPort.calcularScoreReputacao(estabelecimento.getId()))
                    .thenReturn(new BigDecimal("4.25"));

            listener.on(event);

            assertThat(estabelecimento.getScoreReputacao()).isEqualByComparingTo(new BigDecimal("4.25"));
            assertThat(estabelecimento.getAtualizadoEm()).isEqualTo(AGORA);
            verify(estabelecimentoRepository).findByIdWithLock(estabelecimento.getId());
            verify(estabelecimentoRepository).save(estabelecimento);
            verifyNoMoreInteractions(estabelecimentoRepository);
        }

        @Test
        @DisplayName("Deve ignorar silenciosamente quando evento ou estabelecimentoId forem nulos")
        void deveIgnorarQuandoEventoOuIdNulos() {
            listener.on(null);
            listener.on(new ReputacaoEstabelecimentoRecalculadaEvent(null, FIXED_INSTANT));

            verifyNoInteractions(estabelecimentoRepository);
        }

        @Test
        @DisplayName("Não deve lançar exceção nem salvar quando estabelecimento não for encontrado")
        void naoDeveFalharQuandoEstabelecimentoNaoEncontrado() {
            UUID idInexistente = UUID.randomUUID();
            ReputacaoEstabelecimentoRecalculadaEvent event = new ReputacaoEstabelecimentoRecalculadaEvent(
                    idInexistente,
                    FIXED_INSTANT
            );
            when(estabelecimentoRepository.findByIdWithLock(idInexistente)).thenReturn(Optional.empty());

            listener.on(event);

            verify(estabelecimentoRepository).findByIdWithLock(idInexistente);
            verifyNoMoreInteractions(estabelecimentoRepository);
        }
    }
}
