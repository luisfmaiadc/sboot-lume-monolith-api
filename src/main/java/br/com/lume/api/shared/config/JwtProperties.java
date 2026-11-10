package br.com.lume.api.shared.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        @NotBlank String secret,
        @Positive @Max(value = 900_000, message = "A expiração do access token não pode exceder 900.000 ms (15 minutos).") long expiration,
        @Positive long refreshExpiration,
        @NotBlank String issuer
) {
    @AssertTrue(message = "A chave secreta JWT deve conter no mínimo 32 bytes (256 bits).")
    public boolean isSecretValido() {
        return secret != null && secret.getBytes(StandardCharsets.UTF_8).length >= 32;
    }
}
