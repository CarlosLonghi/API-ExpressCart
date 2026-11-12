package br.com.expresscart.controller.request;

import br.com.expresscart.controller.request.validation.UniqueProductIds;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CartRequest(
        @NotNull(message = "Informe o ID do cliente")
        @Min(value = 1, message = "O ID do cliente deve ser maior que 0")
        Long clientId,

        @NotEmpty(message = "Informe a lista de Produtos que deseja adicionar ao carrinho")
        @Valid
        @UniqueProductIds
        List<ProductRequest> products
) {
}
