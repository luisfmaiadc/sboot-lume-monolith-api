package br.com.lume.api.shared.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lume.reputacao")
public record ReputacaoProperties(
        @NotNull @Positive BigDecimal fatorDias30,
        @NotNull @Positive BigDecimal fatorDias90,
        @NotNull @Positive BigDecimal fatorDias365,
        @NotNull @Positive BigDecimal fatorResidual,
        @NotNull @Positive BigDecimal multiplicadorValidacao
) {
}
