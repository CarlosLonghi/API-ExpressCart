package br.com.expresscart.support;

import br.com.expresscart.client.response.PlatziCategoryResponse;
import br.com.expresscart.client.response.PlatziProductResponse;
import br.com.expresscart.controller.request.CartRequest;
import br.com.expresscart.controller.request.ProductRequest;
import br.com.expresscart.entity.Cart;
import br.com.expresscart.entity.Product;
import br.com.expresscart.entity.Status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class TestFixtures {

    public static final String CART_ID = "cart-1";
    public static final Long CLIENT_ID = 1L;

    private TestFixtures() {
    }

    public static PlatziProductResponse platziProduct(Long id, String title, String price) {
        return new PlatziProductResponse(
                id,
                title,
                new BigDecimal(price),
                List.of("https://placehold.co/600x400"),
                new PlatziCategoryResponse(1L, "Demo Category", "https://placehold.co/640x480")
        );
    }

    public static ProductRequest item(Long productId, Integer quantity) {
        return new ProductRequest(productId, quantity);
    }

    public static CartRequest cartRequest(Long clientId, ProductRequest... items) {
        return new CartRequest(clientId, List.of(items));
    }

    public static Product cartItem(Long id, String title, String price, Integer quantity) {
        return Product.builder()
                .id(id)
                .title(title)
                .price(new BigDecimal(price))
                .quantity(quantity)
                .build();
    }

    public static Cart cart(Status status, Product... items) {
        Cart cart = Cart.builder()
                .id(CART_ID)
                .clientId(CLIENT_ID)
                .products(new ArrayList<>(List.of(items)))
                .status(status)
                .build();
        cart.calculateTotalPrice();
        return cart;
    }
}
