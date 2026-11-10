package br.com.lume.api.shared.security;

import br.com.lume.api.shared.config.JwtProperties;
import br.com.lume.api.shared.exception.UnauthorizedException;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final JwtProperties jwtProperties;
    private final Clock clock;

    public String gerarAccessToken(UUID usuarioId, String email, Collection<String> roles) {
        Instant agora = Instant.now(clock);
        Instant expiracao = agora.plusMillis(jwtProperties.expiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(usuarioId.toString())
                .issuedAt(agora)
                .expiresAt(expiracao)
                .claim("email", email)
                .claim("roles", roles != null ? List.copyOf(roles) : List.of())
                .claim("type", "access")
                .build();

        return encode(claims);
    }

    public String gerarRefreshToken(UUID usuarioId, String email) {
        Instant agora = Instant.now(clock);
        Instant expiracao = agora.plusMillis(jwtProperties.refreshExpiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .subject(usuarioId.toString())
                .issuedAt(agora)
                .expiresAt(expiracao)
                .claim("email", email)
                .claim("type", "refresh")
                .build();

        return encode(claims);
    }

    public Jwt decodificarToken(String token) {
        try {
            return buildDecoder().decode(token);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Token JWT inválido ou expirado.", ex);
        }
    }

    public UUID validarRefreshToken(String refreshToken) {
        Jwt jwt = decodificarToken(refreshToken);
        String type = jwt.getClaimAsString("type");
        if (!"refresh".equals(type)) {
            throw new UnauthorizedException("Token informado não é um refresh token válido.");
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new UnauthorizedException("Identificador do usuário inválido no refresh token.");
        }
        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new UnauthorizedException("Identificador do usuário inválido no refresh token.", ex);
        }
    }

    public long getExpirationSeconds() {
        return jwtProperties.expiration() / 1000L;
    }

    private String encode(JwtClaimsSet claims) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(buildSecretKey()));
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    private NimbusJwtDecoder buildDecoder() {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(buildSecretKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
        timestampValidator.setClock(clock);

        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                timestampValidator,
                new JwtIssuerValidator(jwtProperties.issuer())
        );
        decoder.setJwtValidator(validator);
        return decoder;
    }

    private SecretKey buildSecretKey() {
        return new SecretKeySpec(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8),
                HMAC_ALGORITHM
        );
    }
}