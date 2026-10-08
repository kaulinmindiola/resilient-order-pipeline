package io.github.kaulinmindiola.rop.order.adapter.in.rest;

import io.github.kaulinmindiola.rop.order.adapter.security.InvalidClientCredentialsException;
import io.github.kaulinmindiola.rop.order.application.OrderNotFoundException;
import io.github.kaulinmindiola.rop.order.application.UnknownProductException;
import io.github.kaulinmindiola.rop.order.domain.model.InvalidOrderException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Maps every error of the API to an RFC 9457 ProblemDetail (AI-CONTEXT §5). Spring's standard
 * exceptions are handled by the base class; only this service's exceptions are added here. No
 * response ever exposes stack traces or internal details.
 */
@RestControllerAdvice
public class RestExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(RestExceptionHandler.class);
    private static final HttpStatusCode UNPROCESSABLE = HttpStatusCode.valueOf(422);

    /** Bean Validation failures: 400 with one entry per invalid field (REQ-FUNC-001, 002). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        List<Map<String, String>> errors =
                ex.getBindingResult().getFieldErrors().stream()
                        .sorted(Comparator.comparing(FieldError::getField))
                        .map(
                                error ->
                                        Map.of(
                                                "field",
                                                error.getField(),
                                                "message",
                                                Objects.requireNonNullElse(
                                                        error.getDefaultMessage(), "is invalid")))
                        .toList();
        ProblemDetail problem = ex.getBody();
        problem.setProperty("errors", errors);
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(InvalidOrderException.class)
    ProblemDetail invalidOrder(InvalidOrderException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(UnknownProductException.class)
    ProblemDetail unknownProduct(UnknownProductException ex) {
        return ProblemDetail.forStatusAndDetail(UNPROCESSABLE, ex.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail orderNotFound(OrderNotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Same response for unknown client and wrong secret: clients cannot be enumerated (§5). */
    @ExceptionHandler(InvalidClientCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidClientCredentialsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    /** Last resort: the client gets a generic message, the server log gets the full exception. */
    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception ex) {
        log.error("Unexpected error while handling a request", ex);
        return ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred.");
    }
}
