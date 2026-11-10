package br.com.lume.api.shared.exception;

import br.com.lume.api.model.FieldErrorDetail;
import br.com.lume.api.model.StandardError;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.sql.SQLException;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import tools.jackson.databind.exc.InvalidFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final Clock clock;

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<StandardError> handleResourceNotFound(
            ResourceNotFoundException ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<StandardError> handleInvalidRequest(
            InvalidRequestException ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<StandardError> handleBusinessRule(
            BusinessRuleException ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        return buildResponse(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<StandardError> handleConflict(
            ConflictException ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        return buildResponse(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<StandardError> handleAccountLocked(
            AccountLockedException ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        return buildResponse(HttpStatus.LOCKED, ex.getMessage(), request.getRequestURI(), List.of());
    }

    @ExceptionHandler({ForbiddenOperationException.class, AccessDeniedException.class})
    public ResponseEntity<StandardError> handleForbidden(
            Exception ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        String message = (ex instanceof ForbiddenOperationException)
                ? ex.getMessage()
                : "Acesso negado: permissão insuficiente para realizar esta operação.";
        return buildResponse(HttpStatus.FORBIDDEN, message, request.getRequestURI(), List.of());
    }

    @ExceptionHandler({UnauthorizedException.class, AuthenticationException.class})
    public ResponseEntity<StandardError> handleUnauthorized(
            Exception ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        String message = (ex instanceof UnauthorizedException)
                ? ex.getMessage()
                : "Não autenticado ou credenciais inválidas.";
        return buildResponse(HttpStatus.UNAUTHORIZED, message, request.getRequestURI(), List.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardError> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        List<FieldErrorDetail> fields = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Dados fornecidos na requisição são inválidos.",
                request.getRequestURI(),
                fields
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<StandardError> handleConstraintViolation(
            ConstraintViolationException ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        List<FieldErrorDetail> fields = ex.getConstraintViolations().stream()
                .map(violation -> {
                    String path = violation.getPropertyPath() != null ? violation.getPropertyPath().toString() : "param";
                    int lastDot = path.lastIndexOf('.');
                    String field = lastDot >= 0 ? path.substring(lastDot + 1) : path;
                    return new FieldErrorDetail(field, violation.getMessage());
                })
                .toList();
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Parâmetros fornecidos na requisição são inválidos.",
                request.getRequestURI(),
                fields
        );
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<StandardError> handleHandlerMethodValidation(
            HandlerMethodValidationException ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        List<FieldErrorDetail> fields = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();
            String fallbackField = paramName != null ? paramName : "param";
            result.getResolvableErrors().forEach(error -> {
                String field = (error instanceof FieldError fe) ? fe.getField() : fallbackField;
                fields.add(new FieldErrorDetail(field, error.getDefaultMessage()));
            });
        });
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Dados fornecidos na requisição são inválidos.",
                request.getRequestURI(),
                fields
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<StandardError> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);

        List<FieldErrorDetail> fields = new ArrayList<>();
        String message = "Formato da requisição ou parâmetros informados são inválidos.";

        if (ex.getCause() instanceof InvalidFormatException invalidFormatEx) {
            String fieldName = invalidFormatEx.getPath().stream()
                    .map(ref -> ref.getPropertyName() != null ? ref.getPropertyName() : String.valueOf(ref.getIndex()))
                    .reduce((first, second) -> first + "." + second)
                    .orElse("campo");

            Object valorInvalido = invalidFormatEx.getValue();
            Class<?> targetType = invalidFormatEx.getTargetType();

            if (targetType != null && targetType.isEnum()) {
                String valoresAceitos = Arrays.toString(targetType.getEnumConstants());
                String msgErro = String.format("Valor '%s' é inválido para o campo. Valores aceitos: %s", valorInvalido, valoresAceitos);
                fields.add(new FieldErrorDetail(fieldName, msgErro));
                message = "Dados fornecidos na requisição são inválidos.";
            } else {
                String msgErro = String.format("Valor '%s' é inválido para o tipo esperado (%s).",
                        valorInvalido, targetType != null ? targetType.getSimpleName() : "desconhecido");
                fields.add(new FieldErrorDetail(fieldName, msgErro));
                message = "Dados fornecidos na requisição são inválidos.";
            }
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                message,
                request.getRequestURI(),
                fields
        );
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class,
            IllegalArgumentException.class
    })
    public ResponseEntity<StandardError> handleBadRequestExceptions(
            Exception ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "Formato da requisição ou parâmetros informados são inválidos.",
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<StandardError> handleNoResourceFound(
            NoResourceFoundException ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "O recurso ou endpoint solicitado não foi encontrado.",
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<StandardError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            HttpServletRequest request
    ) {
        printLogWarn(ex);
        return buildResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Método HTTP não permitido para este endpoint.",
                request.getRequestURI(),
                List.of()
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<StandardError> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request
    ) {
        String sqlState = extractSqlState(ex);
        log.warn("Violação de integridade no banco: sqlState={}, path={}", sqlState, request.getRequestURI());

        return switch (sqlState != null ? sqlState : "") {
            case "23505" -> buildResponse(
                    HttpStatus.CONFLICT,
                    "Conflito de unicidade: já existe um registro com os dados informados.",
                    request.getRequestURI(),
                    List.of()
            );
            case "23503" -> buildResponse(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Operação inválida: referência a recurso inexistente ou em uso.",
                    request.getRequestURI(),
                    List.of()
            );
            case "23514" -> buildResponse(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "Violação de restrição de integridade dos dados informados.",
                    request.getRequestURI(),
                    List.of()
            );
            case "P0001" -> buildResponse(
                    HttpStatus.CONFLICT,
                    "Operação negada: o usuário não pode validar sua própria ocorrência.",
                    request.getRequestURI(),
                    List.of()
            );
            default -> buildResponse(
                    HttpStatus.CONFLICT,
                    "Conflito de integridade de dados.",
                    request.getRequestURI(),
                    List.of()
            );
        };
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<StandardError> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request
    ) {
        printLogError(ex);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno inesperado.",
                request.getRequestURI(),
                List.of()
        );
    }

    private ResponseEntity<StandardError> buildResponse(
            HttpStatus status,
            String message,
            String path,
            List<FieldErrorDetail> fields
    ) {
        StandardError error = new StandardError(
                OffsetDateTime.now(clock),
                status.value(),
                status.getReasonPhrase(),
                message,
                path
        );
        error.setFields(fields != null ? fields : List.of());
        return ResponseEntity.status(status).body(error);
    }

    private String extractSqlState(Throwable ex) {
        Throwable current = ex;
        while (current != null) {
            if (current instanceof SQLException sqlEx && sqlEx.getSQLState() != null) {
                return sqlEx.getSQLState();
            }
            current = current.getCause();
        }
        return null;
    }

    private void printLogWarn(Exception e) {
        String message;
        try {
            message = e.getMessage();
        } catch (Exception _) {
            message = e.getClass().getSimpleName();
        }
        log.warn("Exceção cliente [{}]: {}", e.getClass().getSimpleName(), message);
    }

    private void printLogError(Exception e) {
        String message;
        try {
            message = e.getMessage();
        } catch (Exception _) {
            message = e.getClass().getSimpleName();
        }
        log.error("Exceção servidor [{}]: {}", e.getClass().getSimpleName(), message, e);
    }
}
