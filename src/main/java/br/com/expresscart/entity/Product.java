package br.com.expresscart.entity;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Item do carrinho. Título e preço são uma cópia do catálogo no momento em que o item foi adicionado.
 */
@Getter
@Builder
public class Product {

    private final Long id;
    private final String title;
    private final BigDecimal price;
    private final Integer quantity;

    public BigDecimal subtotal() {
        return price.multiply(BigDecimal.valueOf(quantity));
    }
}
