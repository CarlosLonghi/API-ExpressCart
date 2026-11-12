package br.com.expresscart.controller.response;

import br.com.expresscart.entity.Cart;
import br.com.expresscart.entity.Status;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        String id,
        Long clientId,
        BigDecimal totalPrice,
        List<CartItemResponse> products,
        Status status
) {

    public CartResponse(Cart cart) {
        this(
                cart.getId(),
                cart.getClientId(),
                cart.getTotalPrice(),
                cart.getProducts().stream().map(CartItemResponse::new).toList(),
                cart.getStatus()
        );
    }
}
