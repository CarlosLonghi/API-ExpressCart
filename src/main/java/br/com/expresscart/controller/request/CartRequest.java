package br.com.expresscart.controller.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CartRequest(
        @NotNull(message = "Informe o ID do cliente")
        @Min(value = 1, message = "O ID do cliente não pode ser menor que 0")
        Long clientId,

        @NotNull(message = "Informe a lista de Produtos que deseja adicionar ao carrinho")
        @Valid
        List<ProductRequest> products
) {
}
