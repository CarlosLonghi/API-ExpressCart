package br.com.expresscart.exception.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.validation.FieldError;

public record ValidationErrorResponse(
        @Schema(description = "Campo que falhou na validação")
        String field,

        @Schema(description = "Mensagem de erro da validação")
        String message
) {
    public ValidationErrorResponse(FieldError fieldError) {
        this(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }
}
