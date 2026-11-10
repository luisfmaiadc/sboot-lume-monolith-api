package br.com.lume.api.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.lume.api.model.StandardError;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import tools.jackson.databind.ObjectMapper;

@DisplayName("SecurityHandlers (CustomAuthenticationEntryPoint e CustomAccessDeniedHandler) - Testes Unitários")
class SecurityHandlersTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final OffsetDateTime EXPECTED_TIMESTAMP = OffsetDateTime.now(FIXED_CLOCK);

    private ObjectMapper objectMapper;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Nested
    @DisplayName("CustomAuthenticationEntryPoint")
    class AuthenticationEntryPointTests {

        @Test
        @DisplayName("Deve serializar StandardError com status 401 UNAUTHORIZED e timestamp determinístico")
        void deveSerializarStandardError401NoEntryPoint() throws IOException {
            CustomAuthenticationEntryPoint entryPoint = new CustomAuthenticationEntryPoint(objectMapper, FIXED_CLOCK);
            request.setRequestURI("/api/v1/ocorrencias");
            InsufficientAuthenticationException authException =
                    new InsufficientAuthenticationException("Full authentication is required");

            entryPoint.commence(request, response, authException);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
            assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());

            StandardError error = objectMapper.readValue(response.getContentAsByteArray(), StandardError.class);
            assertThat(error.getTimestamp()).isEqualTo(EXPECTED_TIMESTAMP);
            assertThat(error.getStatus()).isEqualTo(401);
            assertThat(error.getError()).isEqualTo("Unauthorized");
            assertThat(error.getMessage()).isEqualTo("Não autenticado: token de acesso ausente, inválido ou expirado.");
            assertThat(error.getPath()).isEqualTo("/api/v1/ocorrencias");
            assertThat(error.getFields()).isEmpty();
        }
    }

    @Nested
    @DisplayName("CustomAccessDeniedHandler")
    class AccessDeniedHandlerTests {

        @Test
        @DisplayName("Deve serializar StandardError com status 403 FORBIDDEN e timestamp determinístico")
        void deveSerializarStandardError403NoAccessDeniedHandler() throws IOException {
            CustomAccessDeniedHandler accessDeniedHandler = new CustomAccessDeniedHandler(objectMapper, FIXED_CLOCK);
            request.setRequestURI("/api/v1/admin/usuarios");
            AccessDeniedException accessDeniedException = new AccessDeniedException("Access is denied");

            accessDeniedHandler.handle(request, response, accessDeniedException);

            assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
            assertThat(response.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
            assertThat(response.getCharacterEncoding()).isEqualTo(StandardCharsets.UTF_8.name());

            StandardError error = objectMapper.readValue(response.getContentAsByteArray(), StandardError.class);
            assertThat(error.getTimestamp()).isEqualTo(EXPECTED_TIMESTAMP);
            assertThat(error.getStatus()).isEqualTo(403);
            assertThat(error.getError()).isEqualTo("Forbidden");
            assertThat(error.getMessage()).isEqualTo("Acesso negado: permissão insuficiente para realizar esta operação.");
            assertThat(error.getPath()).isEqualTo("/api/v1/admin/usuarios");
            assertThat(error.getFields()).isEmpty();
        }
    }
}
