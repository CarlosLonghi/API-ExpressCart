package br.com.expresscart.service;

import br.com.expresscart.client.response.PlatziProductResponse;
import br.com.expresscart.controller.request.CartRequest;
import br.com.expresscart.entity.Cart;
import br.com.expresscart.entity.Product;
import br.com.expresscart.entity.Status;
import br.com.expresscart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService;

    public Cart createCart(CartRequest cartRequest) {
        cartRepository.findByClientIdAndStatus(cartRequest.clientId(), Status.OPEN)
                .ifPresent(cart -> {
                    throw new IllegalArgumentException("Já existe um carrinho aberto para esse cliente");
                });

        List<Product> products = new ArrayList<>(cartRequest.products().size());
        for (var productRequest : cartRequest.products()) {
            PlatziProductResponse platziProductResponse = productService.getProductById(productRequest.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

            products.add(Product.builder()
                    .id(platziProductResponse.id())
                    .title(platziProductResponse.title())
                    .price(platziProductResponse.price())
                    .quantity(productRequest.quantity())
                    .build()
            );
        }

        Cart cart = Cart.builder()
                .clientId(cartRequest.clientId())
                .products(products)
                .status(Status.OPEN)
                .build();
        cart.calculateTotalPrice();

        return cartRepository.save(cart);
    }

    public Cart findCartById(String id) {
        return cartRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Carrinho não encontrado"));
    }

    public Cart updateCart(String id, CartRequest cartRequest) {
        Cart savedCart = findCartById(id);

        List<Product> products = new ArrayList<>(cartRequest.products().size());
        for (var productRequest : cartRequest.products()) {
            PlatziProductResponse platziProductResponse = productService.getProductById(productRequest.id())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

            products.add(Product.builder()
                    .id(platziProductResponse.id())
                    .title(platziProductResponse.title())
                    .price(platziProductResponse.price())
                    .quantity(productRequest.quantity())
                    .build()
            );
        }

        savedCart.setProducts(products);
        savedCart.calculateTotalPrice();

        return cartRepository.save(savedCart);
    }
}
