package br.com.expresscart.controller.response;

import java.io.Serializable;

// Serializable: faz parte do ProductResponse, que é gravado no cache do Redis
public record CategoryResponse(
        Long id,
        String name,
        String image
) implements Serializable {
}
