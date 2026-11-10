package br.com.lume.api.shared.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "lume.security.crypto")
public record CryptoProperties(
        @NotBlank String cpfHmacKey,
        @NotBlank String cpfAesKey
) {
    @AssertTrue(message = "A chave HMAC do CPF deve conter no mínimo 32 bytes (256 bits).")
    public boolean isHmacKeyValida() {
        return cpfHmacKey != null && cpfHmacKey.getBytes(StandardCharsets.UTF_8).length >= 32;
    }

    @AssertTrue(message = "A chave AES do CPF deve conter exatamente 32 bytes (256 bits).")
    public boolean isAesKeyValida() {
        return cpfAesKey != null && cpfAesKey.getBytes(StandardCharsets.UTF_8).length == 32;
    }
}