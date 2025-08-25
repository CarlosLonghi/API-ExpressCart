package br.com.expresscart.controller.request;

import br.com.expresscart.entity.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PaymentRequest(
        @NotNull(message = "Informe o método de pagamento para o carrinho (PIX / CREDIT / DEBIT)")
        @Valid
        PaymentMethod paymentMethod
) {
}
