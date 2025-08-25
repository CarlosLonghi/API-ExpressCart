package br.com.expresscart.exception.response;

import org.springframework.validation.FieldError;

public record ValidationErrorResponse(String field, String message) {
    public ValidationErrorResponse(FieldError fieldError) {
        this(
                fieldError.getField(),
                fieldError.getDefaultMessage()
        );
    }
}
