package br.com.expresscart.config;

import br.com.expresscart.exception.BusinessException;
import br.com.expresscart.exception.DataNotFoundException;
import br.com.expresscart.exception.ExternalServiceException;
import br.com.expresscart.exception.response.GenericErrorResponse;
import br.com.expresscart.exception.response.ValidationErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestControllerAdvice
@Slf4j
public class ApplicationControllerAdvice {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<GenericErrorResponse> handleNoHandlerFound(NoHandlerFoundException exception) {
        String path = Optional.ofNullable(exception.getRequestURL()).orElse("URL desconhecida");
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

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<GenericErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException exception) {
        final String message = "Content-Type não suportado. Use application/json";
        GenericErrorResponse errorResponse = new GenericErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                message
        );

        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(errorResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<GenericErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException exception) {
        final String message = "Corpo da requisição inválido: verifique o JSON e os valores permitidos em cada campo";
        GenericErrorResponse errorResponse = new GenericErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                message
        );

        return ResponseEntity.badRequest().body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<GenericErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        final String message = "Valor inválido para o parâmetro '" + exception.getName() + "': " + exception.getValue();
        GenericErrorResponse errorResponse = new GenericErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                message
        );

        return ResponseEntity.badRequest().body(errorResponse);
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

    @ExceptionHandler(ExternalServiceException.class)
    public ResponseEntity<GenericErrorResponse> handleExternalServiceException(ExternalServiceException exception) {
        log.error("Falha em serviço externo", exception);
        GenericErrorResponse errorResponse = new GenericErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_GATEWAY.value(),
                exception.getMessage()
        );

        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResponse);
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
        log.error("Erro inesperado", exception);
        final String message = "Erro interno da aplicação";
        GenericErrorResponse errorResponse = new GenericErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                message
        );

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
    }
}
