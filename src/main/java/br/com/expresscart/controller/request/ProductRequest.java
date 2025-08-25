package br.com.expresscart.controller.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProductRequest(
        @NotNull(message = "Informe o ID do Produto")
        Long id,

        @NotNull(message = "Informe a quantidade que deseja adicionar ao Carrinho")
        @Min(value = 1, message = "A quantidade deve ser maior que 0")
        @Max(value = 100, message = "A quantidade máxima permitida é 100")
        Integer quantity
) {
}
