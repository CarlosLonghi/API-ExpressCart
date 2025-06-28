package br.com.expresscart.client.response;

import java.io.Serializable;

public record PlatziCategoryResponse(
        Long id,
        String name,
        String image
) implements Serializable {
}
