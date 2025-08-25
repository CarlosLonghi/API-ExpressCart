package br.com.expresscart.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestControllerAdvice
public class ControllerAdvice {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<GenericErrorResponse> handleNoHandlerFound(NoHandlerFoundException exception) {
        String path = Optional.of(exception.getRequestURL()).orElse("URL desconhecida");
        final String message = "Recurso não encontrado: " + path;
        GenericErrorResponse body = new GenericErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                message
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<GenericErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException exception) {
        final String message = "Método HTTP '" + exception.getMethod() + "' não é permitido para essa rota";
        GenericErrorResponse errorResponse = new GenericErrorResponse(
                LocalDateTime.now(),
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                message
        );

        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(errorResponse);
    }

    @ExceptionHandler(DataNotFoundException.class)
    public ResponseEntity<GenericErrorResponse> handleDataNotFoundException(DataNotFoundException exception) {
        GenericErrorResponse errorResponse = new GenericErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<GenericErrorResponse> handleBusinessException(BusinessException exception) {
        GenericErrorResponse errorResponse = new GenericErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage()
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ValidationErrorResponse>> handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        List<FieldError> fieldErrors = exception.getFieldErrors();

        return ResponseEntity.badRequest().body(
                fieldErrors.stream()
                .map(ValidationErrorResponse::new).toList()
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<GenericErrorResponse> handleException(Exception exception) {
        final String message = "Erro interno da aplicação";
        GenericErrorResponse errorResponse = new GenericErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }

    // ERRORS RESPONSE
    public record GenericErrorResponse(LocalDateTime timestamp, int status, String message) {}

    public record ValidationErrorResponse(String field, String message) {
        public ValidationErrorResponse(FieldError fieldError) {
            this(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            );
        }
    }
}
