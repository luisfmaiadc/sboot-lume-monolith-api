package br.com.lume.api.shared.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lume.auth.lockout")
public record LockoutProperties(
        @Positive int maxTentativas,
        @Positive int minutosBloqueio,
        @Positive int tentativasAposDesbloqueio
) {
}
