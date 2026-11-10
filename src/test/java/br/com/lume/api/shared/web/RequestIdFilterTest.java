package br.com.lume.api.shared.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@DisplayName("RequestIdFilter - Testes Unitários")
class RequestIdFilterTest {

    private RequestIdFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private static final String ID_65_CHARS = "a".repeat(65);

    @BeforeEach
    void setUp() {
        filter = new RequestIdFilter();
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        MDC.clear();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Nested
    @DisplayName("doFilterInternal(HttpServletRequest, HttpServletResponse, FilterChain)")
    class DoFilterInternal {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t"})
        @DisplayName("Deve gerar novo UUID para X-Request-Id quando cabeçalho estiver ausente ou em branco")
        void deveGerarUuidQuandoHeaderAusenteOuEmBranco(String headerInvalido) throws ServletException, IOException {
            if (headerInvalido != null) {
                request.addHeader(RequestIdFilter.HEADER_NAME, headerInvalido);
            }
            AtomicReference<String> mdcDuranteExecucao = new AtomicReference<>();
            FilterChain filterChain = (req, res) -> mdcDuranteExecucao.set(MDC.get(RequestIdFilter.MDC_KEY));

            filter.doFilterInternal(request, response, filterChain);

            String responseHeader = response.getHeader(RequestIdFilter.HEADER_NAME);
            assertThat(responseHeader).isNotBlank();
            assertThat(UUID.fromString(responseHeader)).isNotNull();
            assertThat(mdcDuranteExecucao.get()).isEqualTo(responseHeader);
            assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
        }

        @ParameterizedTest
        @ValueSource(strings = {"id com espaco", "id@invalido#$", "id\nnewline", "id\r\ninjection"})
        @DisplayName("Deve gerar novo UUID para X-Request-Id quando cabeçalho contiver caracteres inválidos")
        void deveGerarNovoUuidQuandoHeaderInvalido(String headerInvalido) throws ServletException, IOException {
            request.addHeader(RequestIdFilter.HEADER_NAME, headerInvalido);
            AtomicReference<String> mdcDuranteExecucao = new AtomicReference<>();
            FilterChain filterChain = (req, res) -> mdcDuranteExecucao.set(MDC.get(RequestIdFilter.MDC_KEY));

            filter.doFilterInternal(request, response, filterChain);

            String responseHeader = response.getHeader(RequestIdFilter.HEADER_NAME);
            assertThat(responseHeader).isNotBlank();
            assertThat(responseHeader).isNotEqualTo(headerInvalido);
            assertThat(UUID.fromString(responseHeader)).isNotNull();
        }

        @Test
        @DisplayName("Deve gerar novo UUID quando cabeçalho exceder 64 caracteres")
        void deveGerarNovoUuidQuandoHeaderExceder64Caracteres() throws ServletException, IOException {
            request.addHeader(RequestIdFilter.HEADER_NAME, ID_65_CHARS);
            filter.doFilterInternal(request, response, (req, res) -> {});

            String responseHeader = response.getHeader(RequestIdFilter.HEADER_NAME);
            assertThat(responseHeader).isNotEqualTo(ID_65_CHARS);
            assertThat(UUID.fromString(responseHeader)).isNotNull();
        }

        @Test
        @DisplayName("Deve preservar X-Request-Id enviado na requisição no MDC e no cabeçalho de resposta")
        void devePreservarRequestIdQuandoEnviadoNoRequest() throws ServletException, IOException {
            String requestIdExistente = "req-custom-id-98765";
            request.addHeader(RequestIdFilter.HEADER_NAME, requestIdExistente);
            AtomicReference<String> mdcDuranteExecucao = new AtomicReference<>();
            FilterChain filterChain = (req, res) -> mdcDuranteExecucao.set(MDC.get(RequestIdFilter.MDC_KEY));

            filter.doFilterInternal(request, response, filterChain);

            assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo(requestIdExistente);
            assertThat(mdcDuranteExecucao.get()).isEqualTo(requestIdExistente);
            assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
        }

        @Test
        @DisplayName("Deve remover requestId do MDC no bloco finally mesmo quando FilterChain lançar exceção")
        void deveRemoverRequestIdDoMdcQuandoFilterChainLancarExcecao() {
            String requestIdExistente = "req-error-trace-123";
            request.addHeader(RequestIdFilter.HEADER_NAME, requestIdExistente);
            AtomicReference<String> mdcAntesDaFalha = new AtomicReference<>();
            FilterChain filterChain = (req, res) -> {
                mdcAntesDaFalha.set(MDC.get(RequestIdFilter.MDC_KEY));
                throw new ServletException("Falha simulada na cadeia de filtros");
            };

            assertThatThrownBy(() -> filter.doFilterInternal(request, response, filterChain))
                    .isInstanceOf(ServletException.class)
                    .hasMessage("Falha simulada na cadeia de filtros");

            assertThat(mdcAntesDaFalha.get()).isEqualTo(requestIdExistente);
            assertThat(response.getHeader(RequestIdFilter.HEADER_NAME)).isEqualTo(requestIdExistente);
            assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
        }
    }
}
