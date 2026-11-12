package br.com.expresscart.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Corpo do PUT /cart/{id}: a lista substitui todos os itens do carrinho.
 * O dono do carrinho não muda, por isso não há clientId aqui.
 */
public record CartUpdateRequest(
        @NotNull(message = "Informe a lista de Produtos do carrinho")
        @Valid
        List<ProductRequest> products
) {
}
