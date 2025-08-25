package br.com.expresscart.exception.response;

import java.time.LocalDateTime;

public record GenericErrorResponse(
        LocalDateTime timestamp,
        int status,
        String message
) {
}
