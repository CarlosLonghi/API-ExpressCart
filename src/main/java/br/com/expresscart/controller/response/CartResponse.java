package br.com.expresscart.controller.response;

import br.com.expresscart.entity.Cart;
import br.com.expresscart.entity.Product;
import br.com.expresscart.entity.Status;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        String id,
        Long clientId,
        BigDecimal totalPrice,
        List<Product> products,
        Status status
) {

    public CartResponse(Cart cart) {
        this(
                cart.getId(),
                cart.getClientId(),
                cart.getTotalPrice(),
                cart.getProducts(),
                cart.getStatus()
        );
    }
}
