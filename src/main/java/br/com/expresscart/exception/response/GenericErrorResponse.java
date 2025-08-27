package br.com.expresscart.exception.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

public record GenericErrorResponse(
        @Schema(description = "Momento em que o erro ocorreu")
        LocalDateTime timestamp,

        @Schema(description = "Código HTTP retornado")
        int status,

        @Schema(description = "Mensagem descritiva do erro")
        String message
) {
}
