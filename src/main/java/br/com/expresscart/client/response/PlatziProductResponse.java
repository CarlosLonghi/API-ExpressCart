package br.com.expresscart.client.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

public record PlatziProductResponse(
        Long id,
        String title,
        BigDecimal price,
        List<String> images,
        PlatziCategoryResponse category
) implements Serializable {
}
