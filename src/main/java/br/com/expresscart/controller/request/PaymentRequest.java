package br.com.expresscart.controller.request;

import br.com.expresscart.entity.PaymentMethod;

public record PaymentRequest(
        PaymentMethod paymentMethod
) {
}
