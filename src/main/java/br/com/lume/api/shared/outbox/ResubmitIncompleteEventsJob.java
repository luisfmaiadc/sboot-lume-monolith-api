package br.com.lume.api.shared.outbox;

import java.time.Duration;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.IncompleteEventPublications;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResubmitIncompleteEventsJob {

    private static final Duration MIN_AGE = Duration.ofMinutes(1);

    private final IncompleteEventPublications incompleteEventPublications;

    @Scheduled(fixedDelayString = "${lume.outbox.resubmit-delay-ms:60000}")
    @SchedulerLock(name = "ResubmitIncompleteEventsJob", lockAtMostFor = "PT5M", lockAtLeastFor = "PT30S")
    public void resubmitIncompleteEvents() {
        log.debug("Verificando publicações de eventos incompletas: minAge={}", MIN_AGE);
        incompleteEventPublications.resubmitIncompletePublicationsOlderThan(MIN_AGE);
    }
}