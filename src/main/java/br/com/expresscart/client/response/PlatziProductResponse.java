package br.com.expresscart.client.response;

import java.math.BigDecimal;
import java.util.ArrayList;

public record PlatziProductResponse(
        Long id,
        String title,
        BigDecimal price,
        ArrayList<String> images,
        PlatziCategoryResponse category
) {
}
