package br.com.expresscart.controller.response;

import br.com.expresscart.entity.Product;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        String title,
        BigDecimal price,
        Integer quantity
) {

    public CartItemResponse(Product product) {
        this(
                product.getId(),
                product.getTitle(),
                product.getPrice(),
                product.getQuantity()
        );
    }
}
