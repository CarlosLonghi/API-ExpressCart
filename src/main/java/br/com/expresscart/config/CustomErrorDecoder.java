package br.com.expresscart.config;

import br.com.expresscart.exception.handler.DataNotFoundException;
import feign.Response;
import feign.codec.ErrorDecoder;

public class CustomErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        switch (response.status()) {
            case 400:
                return new DataNotFoundException("Produto não encontrado (Platzi Client)");
            default:
                return new Exception("Erro ao buscar produto (Platzi Client)");
        }
    }
}
