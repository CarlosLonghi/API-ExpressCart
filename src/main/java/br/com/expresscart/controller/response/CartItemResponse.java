package br.com.expresscart.controller.response;

import br.com.expresscart.entity.CartItem;

import java.math.BigDecimal;

public record CartItemResponse(
        Long id,
        String title,
        BigDecimal price,
        Integer quantity
) {

    public CartItemResponse(CartItem item) {
        this(
                item.getId(),
                item.getTitle(),
                item.getPrice(),
                item.getQuantity()
        );
    }
}
