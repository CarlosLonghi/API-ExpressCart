package br.com.expresscart.client.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;

public record PlatziProductResponse(
        Long id,
        String title,
        BigDecimal price,
        ArrayList<String> images,
        PlatziCategoryResponse category
) implements Serializable {
}
