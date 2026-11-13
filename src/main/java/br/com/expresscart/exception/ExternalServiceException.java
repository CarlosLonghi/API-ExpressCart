package br.com.expresscart.exception;

/**
 * Falha ao consultar um serviço externo (hoje, a API de produtos da Platzi).
 * Traduzida para 502 Bad Gateway pelo ApplicationControllerAdvice.
 */
public class ExternalServiceException extends RuntimeException {

    public ExternalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
