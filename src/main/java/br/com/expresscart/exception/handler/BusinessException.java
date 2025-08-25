package br.com.expresscart.exception.handler;

public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
