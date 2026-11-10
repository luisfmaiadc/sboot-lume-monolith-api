package br.com.lume.api.shared.outbox;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.modulith.events.IncompleteEventPublications;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResubmitIncompleteEventsJob - Testes Unitários")
class ResubmitIncompleteEventsJobTest {

    @Mock
    private IncompleteEventPublications incompleteEventPublications;

    @InjectMocks
    private ResubmitIncompleteEventsJob job;

    @Nested
    @DisplayName("resubmitIncompleteEvents()")
    class ResubmitIncompleteEvents {

        @Test
        @DisplayName("Deve delegar o reenvio de publicações incompletas com idade mínima de 1 minuto")
        void deveDelegarReenvioComDuracaoDeUmMinuto() {
            job.resubmitIncompleteEvents();

            verify(incompleteEventPublications, times(1))
                    .resubmitIncompletePublicationsOlderThan(Duration.ofMinutes(1));
            verifyNoMoreInteractions(incompleteEventPublications);
        }
    }
}
