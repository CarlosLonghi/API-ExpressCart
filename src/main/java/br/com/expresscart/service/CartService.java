package br.com.expresscart.service;

import br.com.expresscart.client.response.PlatziProductResponse;
import br.com.expresscart.controller.request.CartRequest;
import br.com.expresscart.controller.request.PaymentRequest;
import br.com.expresscart.entity.Cart;
import br.com.expresscart.entity.Product;
import br.com.expresscart.entity.Status;
import br.com.expresscart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
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

        List<Product> productList = buildProductListFromRequest(cartRequest);

        Cart cart = Cart.builder()
                .clientId(cartRequest.clientId())
                .products(productList)
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

        List<Product> productList = buildProductListFromRequest(cartRequest);

        savedCart.setProducts(productList);
        savedCart.calculateTotalPrice();

        return cartRepository.save(savedCart);
    }

    public Cart payCart(String id, PaymentRequest paymentRequest) {
        Cart savedCart = findCartById(id);

        savedCart.setPaymentMethod(paymentRequest.paymentMethod());
        savedCart.setStatus(Status.SOLD);

        return cartRepository.save(savedCart);
    }

    public Cart clearCart(String cartId) {
        Cart savedCart = findCartById(cartId);

        if (savedCart.getStatus().equals(Status.SOLD)) {
            throw new IllegalArgumentException("Não é possivel limpar os produtos de um carrinho já vendido");
        }

        List<Product> emptyProductList = new ArrayList<>();
        savedCart.setProducts(emptyProductList);
        savedCart.calculateTotalPrice();

        return cartRepository.save(savedCart);
    }

    private List<Product> buildProductListFromRequest(CartRequest cartRequest) {
        return cartRequest.products().stream()
                .map(productRequest -> {
                    PlatziProductResponse productResponse = productService.getProductById(productRequest.id())
                            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
                    return Product.builder()
                            .id(productResponse.id())
                            .title(productResponse.title())
                            .price(productResponse.price())
                            .quantity(productRequest.quantity())
                            .build();
                }).toList();
    }
}
