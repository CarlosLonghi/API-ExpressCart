package br.com.expresscart.controller.response;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

/**
 * Produto do catálogo como esta API o expõe.
 * Serializable porque é o valor gravado no cache do Redis (serialização JDK).
 */
public record ProductResponse(
        Long id,
        String title,
        BigDecimal price,
        List<String> images,
        CategoryResponse category
) implements Serializable {
}
