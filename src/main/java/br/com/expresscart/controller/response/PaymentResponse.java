package br.com.expresscart.controller.response;

import br.com.expresscart.entity.Cart;
import br.com.expresscart.entity.PaymentMethod;
import br.com.expresscart.entity.Status;

public record PaymentResponse(
        String id,
        Status status,
        PaymentMethod paymentMethod
) {

    public PaymentResponse(Cart cart) {
        this(
                cart.getId(),
                cart.getStatus(),
                cart.getPaymentMethod()
        );
    }
}
