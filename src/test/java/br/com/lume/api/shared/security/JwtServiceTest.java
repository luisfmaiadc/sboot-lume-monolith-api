package br.com.lume.api.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.lume.api.shared.config.JwtProperties;
import br.com.lume.api.shared.exception.UnauthorizedException;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

@DisplayName("JwtService - Testes Unitários")
class JwtServiceTest {

    private static final String SECRET = "chave-secreta-super-segura-para-testes-unitarios-lume-2026";
    private static final long EXPIRATION_MS = 900_000L;
    private static final long REFRESH_EXPIRATION_MS = 604_800_000L;
    private static final String ISSUER = "lume-api";
    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);

    private JwtProperties jwtProperties;
    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        jwtProperties = new JwtProperties(SECRET, EXPIRATION_MS, REFRESH_EXPIRATION_MS, ISSUER);
        jwtService = new JwtService(jwtProperties, FIXED_CLOCK);
    }

    @Nested
    @DisplayName("Validação de JwtProperties")
    class ValidacaoJwtProperties {

        @Test
        @DisplayName("Deve validar com sucesso quando a chave tiver 32 bytes ou mais")
        void deveValidarChaveComPeloMenos32Bytes() {
            JwtProperties props = new JwtProperties(SECRET, EXPIRATION_MS, REFRESH_EXPIRATION_MS, ISSUER);
            assertThat(props.isSecretValido()).isTrue();
        }

        @Test
        @DisplayName("Deve invalidar quando a chave tiver menos de 32 bytes")
        void deveInvalidarChaveComMenosDe32Bytes() {
            JwtProperties propsCurta = new JwtProperties("chave-curta", EXPIRATION_MS, REFRESH_EXPIRATION_MS, ISSUER);
            assertThat(propsCurta.isSecretValido()).isFalse();
        }
    }

    @Nested
    @DisplayName("gerarAccessToken e decodificarToken")
    class GerarEDecodificarAccessToken {

        @Test
        @DisplayName("Deve gerar e decodificar accessToken com claims corretas e tempo determinístico")
        void deveGerarEDecodificarAccessTokenComSucesso() {
            UUID usuarioId = UUID.fromString("11111111-2222-3333-4444-555555555555");
            String email = "cidadao@lume.com.br";
            List<String> roles = List.of("ROLE_CIDADAO", "ROLE_GESTOR");

            String token = jwtService.gerarAccessToken(usuarioId, email, roles);
            Jwt jwt = jwtService.decodificarToken(token);

            assertThat(token).isNotBlank();
            assertThat(jwt.getSubject()).isEqualTo(usuarioId.toString());
            assertThat(jwt.getIssuedAt()).isEqualTo(FIXED_INSTANT);
            assertThat(jwt.getExpiresAt()).isEqualTo(FIXED_INSTANT.plusMillis(EXPIRATION_MS));
            assertThat(jwt.getClaimAsString("iss")).isEqualTo(ISSUER);
            assertThat(jwt.getClaimAsString("email")).isEqualTo(email);
            assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ROLE_CIDADAO", "ROLE_GESTOR");
            assertThat(jwt.getClaimAsString("type")).isEqualTo("access");
        }

        @Test
        @DisplayName("Deve gerar accessToken com lista de roles vazia quando roles for nulo")
        void deveGerarAccessTokenComRolesVaziaQuandoNulo() {
            UUID usuarioId = UUID.fromString("11111111-2222-3333-4444-555555555555");

            String token = jwtService.gerarAccessToken(usuarioId, "cidadao@lume.com.br", null);
            Jwt jwt = jwtService.decodificarToken(token);

            assertThat(jwt.getClaimAsStringList("roles")).isEmpty();
        }

        @Test
        @DisplayName("Deve rejeitar token expirado lançando UnauthorizedException")
        void deveRejeitarTokenExpirado() {
            UUID usuarioId = UUID.randomUUID();
            String token = jwtService.gerarAccessToken(usuarioId, "cidadao@lume.com.br", List.of("ROLE_CIDADAO"));

            Clock clockNoFuturo = Clock.fixed(FIXED_INSTANT.plusMillis(EXPIRATION_MS + 1000L), ZoneOffset.UTC);
            JwtService jwtServiceNoFuturo = new JwtService(jwtProperties, clockNoFuturo);

            assertThatThrownBy(() -> jwtServiceNoFuturo.decodificarToken(token))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Token JWT inválido ou expirado.");
        }

        @Test
        @DisplayName("Deve rejeitar token malformado ou assinado com outra chave lançando UnauthorizedException")
        void deveRejeitarTokenInvalidoOuAssinaturaIncorreta() {
            JwtProperties outraPropriedade = new JwtProperties(
                    "outra-chave-secreta-completamente-diferente-para-teste-123",
                    EXPIRATION_MS,
                    REFRESH_EXPIRATION_MS,
                    ISSUER
            );
            JwtService outroJwtService = new JwtService(outraPropriedade, FIXED_CLOCK);
            String tokenOutraChave = outroJwtService.gerarAccessToken(
                    UUID.randomUUID(), "teste@lume.com.br", List.of("ROLE_CIDADAO")
            );

            assertThatThrownBy(() -> jwtService.decodificarToken("token-invalido"))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Token JWT inválido ou expirado.");

            assertThatThrownBy(() -> jwtService.decodificarToken(tokenOutraChave))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Token JWT inválido ou expirado.");
        }

        @Test
        @DisplayName("Deve rejeitar token com issuer divergente lançando UnauthorizedException")
        void deveRejeitarTokenComIssuerDivergente() {
            JwtProperties outroIssuerProps = new JwtProperties(SECRET, EXPIRATION_MS, REFRESH_EXPIRATION_MS, "outro-issuer");
            JwtService outroIssuerService = new JwtService(outroIssuerProps, FIXED_CLOCK);
            String tokenOutroIssuer = outroIssuerService.gerarAccessToken(
                    UUID.randomUUID(), "teste@lume.com.br", List.of("ROLE_CIDADAO")
            );

            assertThatThrownBy(() -> jwtService.decodificarToken(tokenOutroIssuer))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Token JWT inválido ou expirado.");
        }
    }

    @Nested
    @DisplayName("gerarRefreshToken e validarRefreshToken")
    class GerarEValidarRefreshToken {

        @Test
        @DisplayName("Deve gerar e validar refreshToken retornando o UUID do usuário")
        void deveGerarEValidarRefreshTokenComSucesso() {
            UUID usuarioId = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
            String email = "gestor@lume.com.br";

            String refreshToken = jwtService.gerarRefreshToken(usuarioId, email);
            UUID resultadoId = jwtService.validarRefreshToken(refreshToken);
            Jwt jwt = jwtService.decodificarToken(refreshToken);

            assertThat(resultadoId).isEqualTo(usuarioId);
            assertThat(jwt.getClaimAsString("type")).isEqualTo("refresh");
            assertThat(jwt.getClaimAsString("email")).isEqualTo(email);
            assertThat(jwt.getExpiresAt()).isEqualTo(FIXED_INSTANT.plusMillis(REFRESH_EXPIRATION_MS));
        }

        @Test
        @DisplayName("Deve rejeitar accessToken quando passado para validarRefreshToken")
        void deveRejeitarAccessTokenEmValidarRefreshToken() {
            UUID usuarioId = UUID.randomUUID();
            String accessToken = jwtService.gerarAccessToken(usuarioId, "user@lume.com.br", List.of("ROLE_CIDADAO"));

            assertThatThrownBy(() -> jwtService.validarRefreshToken(accessToken))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Token informado não é um refresh token válido.");
        }

        @Test
        @DisplayName("Deve rejeitar refreshToken com subject que não seja um UUID válido")
        void deveRejeitarRefreshTokenComSubjectInvalido() {
            SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(ISSUER)
                    .subject("subject-nao-uuid")
                    .issuedAt(FIXED_INSTANT)
                    .expiresAt(FIXED_INSTANT.plusMillis(REFRESH_EXPIRATION_MS))
                    .claim("type", "refresh")
                    .build();
            String tokenComSubjectInvalido = encoder.encode(
                    JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)
            ).getTokenValue();

            assertThatThrownBy(() -> jwtService.validarRefreshToken(tokenComSubjectInvalido))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Identificador do usuário inválido no refresh token.");
        }

        @Test
        @DisplayName("Deve rejeitar refreshToken com subject nulo ou em branco")
        void deveRejeitarRefreshTokenComSubjectNuloOuVazio() {
            SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            NimbusJwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(ISSUER)
                    .issuedAt(FIXED_INSTANT)
                    .expiresAt(FIXED_INSTANT.plusMillis(REFRESH_EXPIRATION_MS))
                    .claim("type", "refresh")
                    .build();
            String tokenSemSubject = encoder.encode(
                    JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)
            ).getTokenValue();

            assertThatThrownBy(() -> jwtService.validarRefreshToken(tokenSemSubject))
                    .isInstanceOf(UnauthorizedException.class)
                    .hasMessage("Identificador do usuário inválido no refresh token.");
        }
    }

    @Nested
    @DisplayName("getExpirationSeconds()")
    class GetExpirationSeconds {

        @Test
        @DisplayName("Deve retornar o tempo de expiração convertido em segundos")
        void deveRetornarExpiracaoEmSegundos() {
            assertThat(jwtService.getExpirationSeconds()).isEqualTo(900L);
        }
    }
}
