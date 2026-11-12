package br.com.expresscart.exception.handler;

/**
 * Falha ao consultar um serviço externo (hoje, a API de produtos da Platzi).
 * Traduzida para 502 Bad Gateway pelo ControllerAdvice.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
