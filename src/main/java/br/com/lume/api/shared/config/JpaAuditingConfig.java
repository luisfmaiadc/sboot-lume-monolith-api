package br.com.lume.api.shared.config;

import br.com.lume.api.shared.security.AuthenticatedUserProvider;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider", auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock clock) {
        return () -> Optional.of(OffsetDateTime.now(clock));
    }

    @Bean
    public AuditorAware<UUID> auditorAware(AuthenticatedUserProvider authenticatedUserProvider) {
        return authenticatedUserProvider::findUsuarioIdAutenticado;
    }
}