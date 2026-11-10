package br.com.lume.api.shared.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.lume.api.model.FieldErrorDetail;
import br.com.lume.api.model.StandardError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.core.MethodParameter;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@DisplayName("GlobalExceptionHandler - Testes Unitários")
class GlobalExceptionHandlerTest {

    private static final Instant FIXED_INSTANT = Instant.parse("2026-05-10T14:30:00Z");
    private static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC);
    private static final OffsetDateTime EXPECTED_TIMESTAMP = OffsetDateTime.now(FIXED_CLOCK);
    private static final String REQUEST_URI = "/api/v1/ocorrencias";

    private GlobalExceptionHandler handler;
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(FIXED_CLOCK);
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.setRequestURI(REQUEST_URI);
        request = mockRequest;
    }

    @Nested
    @DisplayName("Exceções de Domínio e Segurança (401, 403, 404, 409, 422, 423)")
    class ExcecoesDominioESeguranca {

        @Test
        @DisplayName("Deve retornar 404 NOT_FOUND ao tratar ResourceNotFoundException")
        void deveRetornar404ParaResourceNotFoundException() {
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    "Ocorrência não encontrada.", new RuntimeException("causa"));

            ResponseEntity<StandardError> response = handler.handleResourceNotFound(ex, request);

            assertStandardError(response, HttpStatus.NOT_FOUND, "Ocorrência não encontrada.");
        }

        @Test
        @DisplayName("Deve retornar 422 UNPROCESSABLE_CONTENT ao tratar BusinessRuleException")
        void deveRetornar422ParaBusinessRuleException() {
            BusinessRuleException ex = new BusinessRuleException(
                    "Transição de status inválida.", new RuntimeException("causa"));

            ResponseEntity<StandardError> response = handler.handleBusinessRule(ex, request);

            assertStandardError(response, HttpStatus.UNPROCESSABLE_CONTENT, "Transição de status inválida.");
        }

        @Test
        @DisplayName("Deve retornar 409 CONFLICT ao tratar ConflictException")
        void deveRetornar409ParaConflictException() {
            ConflictException ex = new ConflictException(
                    "E-mail já cadastrado.", new RuntimeException("causa"));

            ResponseEntity<StandardError> response = handler.handleConflict(ex, request);

            assertStandardError(response, HttpStatus.CONFLICT, "E-mail já cadastrado.");
        }

        @Test
        @DisplayName("Deve retornar 423 LOCKED ao tratar AccountLockedException")
        void deveRetornar423ParaAccountLockedException() {
            AccountLockedException ex = new AccountLockedException(
                    "Conta temporariamente bloqueada.", new RuntimeException("causa"));

            ResponseEntity<StandardError> response = handler.handleAccountLocked(ex, request);

            assertStandardError(response, HttpStatus.LOCKED, "Conta temporariamente bloqueada.");
        }

        @Test
        @DisplayName("Deve retornar 403 FORBIDDEN com mensagem própria ao tratar ForbiddenOperationException")
        void deveRetornar403ParaForbiddenOperationException() {
            ForbiddenOperationException ex = new ForbiddenOperationException(
                    "Apenas o autor pode excluir este comentário.", new RuntimeException("causa"));

            ResponseEntity<StandardError> response = handler.handleForbidden(ex, request);

            assertStandardError(response, HttpStatus.FORBIDDEN, "Apenas o autor pode excluir este comentário.");
        }

        @Test
        @DisplayName("Deve retornar 403 FORBIDDEN com mensagem padrão ao tratar AccessDeniedException")
        void deveRetornar403ParaAccessDeniedException() {
            AccessDeniedException ex = new AccessDeniedException("Access is denied");

            ResponseEntity<StandardError> response = handler.handleForbidden(ex, request);

            assertStandardError(
                    response,
                    HttpStatus.FORBIDDEN,
                    "Acesso negado: permissão insuficiente para realizar esta operação."
            );
        }

        @Test
        @DisplayName("Deve retornar 401 UNAUTHORIZED com mensagem própria ao tratar UnauthorizedException")
        void deveRetornar401ParaUnauthorizedException() {
            UnauthorizedException ex = new UnauthorizedException(
                    "Token de atualização expirado.", new RuntimeException("causa"));

            ResponseEntity<StandardError> response = handler.handleUnauthorized(ex, request);

            assertStandardError(response, HttpStatus.UNAUTHORIZED, "Token de atualização expirado.");
        }

        @Test
        @DisplayName("Deve retornar 401 UNAUTHORIZED com mensagem padrão ao tratar AuthenticationException")
        void deveRetornar401ParaAuthenticationException() {
            BadCredentialsException ex = new BadCredentialsException("Bad credentials");

            ResponseEntity<StandardError> response = handler.handleUnauthorized(ex, request);

            assertStandardError(
                    response,
                    HttpStatus.UNAUTHORIZED,
                    "Não autenticado ou credenciais inválidas."
            );
        }
    }

    @Nested
    @DisplayName("Exceções de Validação e Requisição (400, 404, 405)")
    class ExcecoesValidacaoERequisicao {

        @Test
        @DisplayName("Deve retornar 400 BAD_REQUEST com mensagem própria ao tratar InvalidRequestException")
        void deveRetornar400ParaInvalidRequestException() {
            InvalidRequestException ex = new InvalidRequestException(
                    "Parâmetro de consulta inválido.", new RuntimeException("causa"));

            ResponseEntity<StandardError> response = handler.handleInvalidRequest(ex, request);

            assertStandardError(response, HttpStatus.BAD_REQUEST, "Parâmetro de consulta inválido.");
        }

        @Test
        @DisplayName("Deve retornar 400 BAD_REQUEST com lista de fields ao tratar MethodArgumentNotValidException")
        void deveRetornar400ComFieldsParaMethodArgumentNotValidException() {
            BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "ocorrenciaRequest");
            bindingResult.addError(new FieldError("ocorrenciaRequest", "titulo", "O título é obrigatório."));
            bindingResult.addError(new FieldError("ocorrenciaRequest", "latitude", "A latitude é inválida."));
            MethodParameter parameter = mock(MethodParameter.class);
            MethodArgumentNotValidException ex = new MethodArgumentNotValidException(parameter, bindingResult);

            ResponseEntity<StandardError> response = handler.handleMethodArgumentNotValid(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            StandardError body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getTimestamp()).isEqualTo(EXPECTED_TIMESTAMP);
            assertThat(body.getStatus()).isEqualTo(400);
            assertThat(body.getError()).isEqualTo("Bad Request");
            assertThat(body.getMessage()).isEqualTo("Dados fornecidos na requisição são inválidos.");
            assertThat(body.getPath()).isEqualTo(REQUEST_URI);
            assertThat(body.getFields())
                    .hasSize(2)
                    .extracting(FieldErrorDetail::getField, FieldErrorDetail::getMessage)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("titulo", "O título é obrigatório."),
                            org.assertj.core.groups.Tuple.tuple("latitude", "A latitude é inválida.")
                    );
        }

        @Test
        @DisplayName("Deve retornar 400 BAD_REQUEST extraindo nome do parâmetro ao tratar ConstraintViolationException")
        @SuppressWarnings("unchecked")
        void deveRetornar400ComFieldsParaConstraintViolationException() {
            ConstraintViolation<Object> violationComPonto = mock(ConstraintViolation.class);
            Path pathComPonto = mock(Path.class);
            when(pathComPonto.toString()).thenReturn("listarOcorrencias.raioMetros");
            when(violationComPonto.getPropertyPath()).thenReturn(pathComPonto);
            when(violationComPonto.getMessage()).thenReturn("Deve ser maior ou igual a 100.");

            ConstraintViolation<Object> violationSemPonto = mock(ConstraintViolation.class);
            Path pathSemPonto = mock(Path.class);
            when(pathSemPonto.toString()).thenReturn("pagina");
            when(violationSemPonto.getPropertyPath()).thenReturn(pathSemPonto);
            when(violationSemPonto.getMessage()).thenReturn("Não pode ser negativo.");

            ConstraintViolation<Object> violationPathNulo = mock(ConstraintViolation.class);
            when(violationPathNulo.getPropertyPath()).thenReturn(null);
            when(violationPathNulo.getMessage()).thenReturn("Parâmetro inválido.");

            ConstraintViolationException ex = new ConstraintViolationException(
                    Set.of(violationComPonto, violationSemPonto, violationPathNulo)
            );

            ResponseEntity<StandardError> response = handler.handleConstraintViolation(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            StandardError body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getMessage()).isEqualTo("Parâmetros fornecidos na requisição são inválidos.");
            assertThat(body.getFields())
                    .hasSize(3)
                    .extracting(FieldErrorDetail::getField, FieldErrorDetail::getMessage)
                    .containsExactlyInAnyOrder(
                            org.assertj.core.groups.Tuple.tuple("raioMetros", "Deve ser maior ou igual a 100."),
                            org.assertj.core.groups.Tuple.tuple("pagina", "Não pode ser negativo."),
                            org.assertj.core.groups.Tuple.tuple("param", "Parâmetro inválido.")
                    );
        }

        @Test
        @DisplayName("Deve retornar 400 BAD_REQUEST com fields ao tratar HandlerMethodValidationException")
        void deveRetornar400ComFieldsParaHandlerMethodValidationException() {
            HandlerMethodValidationException ex = mock(HandlerMethodValidationException.class);

            MethodParameter methodParam1 = mock(MethodParameter.class);
            when(methodParam1.getParameterName()).thenReturn("filtro");
            ParameterValidationResult resultComFieldError = mock(ParameterValidationResult.class);
            when(resultComFieldError.getMethodParameter()).thenReturn(methodParam1);
            FieldError fieldError = new FieldError("filtro", "categoriaId", "Categoria inválida.");
            when(resultComFieldError.getResolvableErrors()).thenReturn(List.of(fieldError));

            MethodParameter methodParam2 = mock(MethodParameter.class);
            when(methodParam2.getParameterName()).thenReturn(null);
            ParameterValidationResult resultSemFieldError = mock(ParameterValidationResult.class);
            when(resultSemFieldError.getMethodParameter()).thenReturn(methodParam2);
            MessageSourceResolvable resolvableError = mock(MessageSourceResolvable.class);
            when(resolvableError.getDefaultMessage()).thenReturn("Valor obrigatório.");
            when(resultSemFieldError.getResolvableErrors()).thenReturn(List.of(resolvableError));

            when(ex.getParameterValidationResults()).thenReturn(List.of(resultComFieldError, resultSemFieldError));

            ResponseEntity<StandardError> response = handler.handleHandlerMethodValidation(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            StandardError body = response.getBody();
            assertThat(body).isNotNull();
            assertThat(body.getMessage()).isEqualTo("Dados fornecidos na requisição são inválidos.");
            assertThat(body.getFields())
                    .hasSize(2)
                    .extracting(FieldErrorDetail::getField, FieldErrorDetail::getMessage)
                    .containsExactly(
                            org.assertj.core.groups.Tuple.tuple("categoriaId", "Categoria inválida."),
                            org.assertj.core.groups.Tuple.tuple("param", "Valor obrigatório.")
                    );
        }

        @Test
        @DisplayName("Deve retornar 400 BAD_REQUEST para exceções de requisição malformada")
        void deveRetornar400ParaBadRequestExceptions() {
            HttpMessageNotReadableException notReadableEx = new HttpMessageNotReadableException(
                    "JSON parse error", new MockHttpInputMessage(new byte[0]));
            MissingServletRequestParameterException missingParamEx =
                    new MissingServletRequestParameterException("latitude", "Double");
            MethodArgumentTypeMismatchException typeMismatchEx = new MethodArgumentTypeMismatchException(
                    "abc", UUID_CLASS, "id", null, new IllegalArgumentException("Invalid UUID"));
            IllegalArgumentException illegalArgEx = new IllegalArgumentException("Argumento inválido");

            assertStandardError(
                    handler.handleHttpMessageNotReadable(notReadableEx, request),
                    HttpStatus.BAD_REQUEST,
                    "Formato da requisição ou parâmetros informados são inválidos."
            );
            assertStandardError(
                    handler.handleBadRequestExceptions(missingParamEx, request),
                    HttpStatus.BAD_REQUEST,
                    "Formato da requisição ou parâmetros informados são inválidos."
            );
            assertStandardError(
                    handler.handleBadRequestExceptions(typeMismatchEx, request),
                    HttpStatus.BAD_REQUEST,
                    "Formato da requisição ou parâmetros informados são inválidos."
            );
            assertStandardError(
                    handler.handleBadRequestExceptions(illegalArgEx, request),
                    HttpStatus.BAD_REQUEST,
                    "Formato da requisição ou parâmetros informados são inválidos."
            );
        }

        @Test
        @DisplayName("Deve retornar 400 BAD_REQUEST com detalhe do campo ao tratar HttpMessageNotReadableException com InvalidFormatException")
        void deveRetornar400ComDetalheDeCampoParaInvalidFormatException() {
            tools.jackson.databind.exc.InvalidFormatException cause = tools.jackson.databind.exc.InvalidFormatException.from(
                    null,
                    "Cannot deserialize value of type `br.com.lume.api.identidade.domain.OrientacaoSexual` from String \"INVALIDO\"",
                    "INVALIDO",
                    br.com.lume.api.identidade.domain.OrientacaoSexual.class
            );
            cause.prependPath(new Object(), "orientacaoSexual");

            HttpMessageNotReadableException ex = new HttpMessageNotReadableException(
                    "JSON parse error", cause, new MockHttpInputMessage(new byte[0]));

            ResponseEntity<StandardError> response = handler.handleHttpMessageNotReadable(ex, request);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody().getMessage()).isEqualTo("Dados fornecidos na requisição são inválidos.");
            assertThat(response.getBody().getFields()).hasSize(1);
            assertThat(response.getBody().getFields().getFirst().getField()).isEqualTo("orientacaoSexual");
            assertThat(response.getBody().getFields().getFirst().getMessage())
                    .contains("Valor 'INVALIDO' é inválido para o campo")
                    .contains("ASSEXUAL");
        }

        @Test
        @DisplayName("Deve retornar 404 NOT_FOUND ao tratar NoResourceFoundException")
        void deveRetornar404ParaNoResourceFoundException() {
            NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, REQUEST_URI, REQUEST_URI);

            ResponseEntity<StandardError> response = handler.handleNoResourceFound(ex, request);

            assertStandardError(
                    response,
                    HttpStatus.NOT_FOUND,
                    "O recurso ou endpoint solicitado não foi encontrado."
            );
        }

        @Test
        @DisplayName("Deve retornar 405 METHOD_NOT_ALLOWED ao tratar HttpRequestMethodNotSupportedException")
        void deveRetornar405ParaHttpRequestMethodNotSupportedException() {
            HttpRequestMethodNotSupportedException ex = new HttpRequestMethodNotSupportedException("DELETE");

            ResponseEntity<StandardError> response = handler.handleMethodNotSupported(ex, request);

            assertStandardError(
                    response,
                    HttpStatus.METHOD_NOT_ALLOWED,
                    "Método HTTP não permitido para este endpoint."
            );
        }
    }

    @Nested
    @DisplayName("Violação de Integridade de Dados (DataIntegrityViolationException)")
    class ViolacaoIntegridadeDados {

        @Test
        @DisplayName("Deve retornar 409 CONFLICT para SQLState 23505 (unique_violation) sem vazar detalhes de SQL")
        void deveRetornar409ParaSqlState23505() {
            DataIntegrityViolationException ex = criarDataIntegrityViolation(
                    "ERROR: duplicate key value violates unique constraint \"uk_usuarios_email\"",
                    "23505"
            );

            ResponseEntity<StandardError> response = handler.handleDataIntegrityViolation(ex, request);

            assertStandardError(
                    response,
                    HttpStatus.CONFLICT,
                    "Conflito de unicidade: já existe um registro com os dados informados."
            );
            assertThat(response.getBody().getMessage()).doesNotContain("uk_usuarios_email", "ERROR:");
        }

        @Test
        @DisplayName("Deve retornar 422 UNPROCESSABLE_CONTENT para SQLState 23503 (foreign_key_violation)")
        void deveRetornar422ParaSqlState23503() {
            DataIntegrityViolationException ex = criarDataIntegrityViolation(
                    "ERROR: insert or update on table violates foreign key constraint",
                    "23503"
            );

            ResponseEntity<StandardError> response = handler.handleDataIntegrityViolation(ex, request);

            assertStandardError(
                    response,
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Operação inválida: referência a recurso inexistente ou em uso."
            );
        }

        @Test
        @DisplayName("Deve retornar 422 UNPROCESSABLE_CONTENT para SQLState 23514 (check_violation)")
        void deveRetornar422ParaSqlState23514() {
            DataIntegrityViolationException ex = criarDataIntegrityViolation(
                    "ERROR: new row for relation violates check constraint",
                    "23514"
            );

            ResponseEntity<StandardError> response = handler.handleDataIntegrityViolation(ex, request);

            assertStandardError(
                    response,
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Violação de restrição de integridade dos dados informados."
            );
        }

        @Test
        @DisplayName("Deve retornar 409 CONFLICT com mensagem de autovalidação para SQLState P0001 (raise_exception)")
        void deveRetornar409ParaSqlStateP0001() {
            DataIntegrityViolationException ex = criarDataIntegrityViolation(
                    "ERROR: O usuário não pode validar a própria ocorrência",
                    "P0001"
            );

            ResponseEntity<StandardError> response = handler.handleDataIntegrityViolation(ex, request);

            assertStandardError(
                    response,
                    HttpStatus.CONFLICT,
                    "Operação negada: o usuário não pode validar sua própria ocorrência."
            );
        }

        @Test
        @DisplayName("Deve retornar 409 CONFLICT genérico quando SQLState não for mapeado ou não houver SQLException")
        void deveRetornar409ParaSqlStateDefaultOuAusente() {
            DataIntegrityViolationException exOutroEstado = criarDataIntegrityViolation("ERROR: not null", "23502");
            DataIntegrityViolationException exSemSqlException = new DataIntegrityViolationException(
                    "Erro genérico", new RuntimeException("Causa sem SQLException"));
            DataIntegrityViolationException exSqlStateNulo = criarDataIntegrityViolation("ERROR", null);

            assertStandardError(
                    handler.handleDataIntegrityViolation(exOutroEstado, request),
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados."
            );
            assertStandardError(
                    handler.handleDataIntegrityViolation(exSemSqlException, request),
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados."
            );
            assertStandardError(
                    handler.handleDataIntegrityViolation(exSqlStateNulo, request),
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados."
            );
        }

        private DataIntegrityViolationException criarDataIntegrityViolation(String sqlMessage, String sqlState) {
            SQLException sqlException = new SQLException(sqlMessage, sqlState);
            return new DataIntegrityViolationException("PreparedStatementCallback; SQL [...]", sqlException);
        }
    }

    @Nested
    @DisplayName("Exceções Inesperadas (500)")
    class ExcecoesInesperadas {

        @Test
        @DisplayName("Deve retornar 500 INTERNAL_SERVER_ERROR ao tratar Exception genérica sem expor detalhes internos")
        void deveRetornar500ParaExceptionGenerica() {
            Exception ex = new RuntimeException("NullPointerException em detalhe interno sensível");

            ResponseEntity<StandardError> response = handler.handleUnexpectedException(ex, request);

            assertStandardError(
                    response,
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Ocorreu um erro interno inesperado."
            );
            assertThat(response.getBody().getMessage()).doesNotContain("NullPointerException");
        }
    }

    private static final Class<?> UUID_CLASS = java.util.UUID.class;

    private void assertStandardError(
            ResponseEntity<StandardError> response,
            HttpStatus expectedStatus,
            String expectedMessage
    ) {
        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        StandardError body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getTimestamp()).isEqualTo(EXPECTED_TIMESTAMP);
        assertThat(body.getStatus()).isEqualTo(expectedStatus.value());
        assertThat(body.getError()).isEqualTo(expectedStatus.getReasonPhrase());
        assertThat(body.getMessage()).isEqualTo(expectedMessage);
        assertThat(body.getPath()).isEqualTo(REQUEST_URI);
        assertThat(body.getFields()).isEmpty();
    }
}
