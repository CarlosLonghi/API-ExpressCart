package br.com.expresscart.controller.response;

import java.math.BigDecimal;
import java.util.List;

/**
 * Produto do catálogo como esta API o expõe. Também é o valor gravado, em JSON, no cache do Redis.
 */
public record ProductResponse(
        Long id,
        String title,
        BigDecimal price,
        List<String> images,
        CategoryResponse category
) {
}
