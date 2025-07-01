package br.com.expresscart.controller.request;

import java.util.List;

public record CartRequest(
        Long clientId,
        List<ProductRequest> products
) {
}
