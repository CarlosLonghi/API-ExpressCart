package br.com.expresscart.service;

import br.com.expresscart.client.response.PlatziProductResponse;
import br.com.expresscart.controller.request.CartRequest;
import br.com.expresscart.controller.request.PaymentRequest;
import br.com.expresscart.controller.response.CartResponse;
import br.com.expresscart.controller.response.PaymentResponse;
import br.com.expresscart.entity.Cart;
import br.com.expresscart.entity.Product;
import br.com.expresscart.entity.Status;
import br.com.expresscart.exception.handler.BusinessException;
import br.com.expresscart.exception.handler.DataNotFoundException;
import br.com.expresscart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductService productService;

    public CartResponse createCart(CartRequest cartRequest) {
        cartRepository.findByClientIdAndStatus(cartRequest.clientId(), Status.OPEN)
                .ifPresent(cart -> {
                    throw new BusinessException("Já existe um carrinho aberto para o cliente de id: " + cartRequest.clientId()
                    );
                });

        List<Product> productList = buildProductListFromRequest(cartRequest);

        Cart cart = Cart.builder()
                .clientId(cartRequest.clientId())
                .products(productList)
                .status(Status.OPEN)
                .build();
        cart.calculateTotalPrice();

        Cart createdCart = cartRepository.save(cart);
        return new CartResponse(createdCart);
    }

    public CartResponse findCartById(String id) {
        Cart cart = getCartById(id);
        return new CartResponse(cart);
    }

    public CartResponse updateCart(String cartId, CartRequest cartRequest) {
        Cart cart = getCartById(cartId);

        if (cart.getStatus().equals(Status.SOLD)) {
            throw new BusinessException("Não é possivel alterar os dados de um carrinho já vendido.");
        }

        List<Product> productList = buildProductListFromRequest(cartRequest);

        cart.setProducts(productList);
        cart.calculateTotalPrice();

        Cart updatedCart = cartRepository.save(cart);
        return new CartResponse(updatedCart);
    }

    public PaymentResponse payCart(String cartId, PaymentRequest paymentRequest) {
        Cart cart = getCartById(cartId);

        if (cart.getStatus().equals(Status.SOLD)) {
            throw new BusinessException("Operação cancelada. O carrinho informado já foi pago.");
        }

        cart.setPaymentMethod(paymentRequest.paymentMethod());
        cart.setStatus(Status.SOLD);

        Cart paidCart = cartRepository.save(cart);
        return new PaymentResponse(paidCart);
    }

    public CartResponse clearCart(String cartId) {
        Cart cart = getCartById(cartId);

        if (cart.getStatus().equals(Status.SOLD)) {
            throw new BusinessException("Não é possivel limpar um carrinho já vendido.");
        }

        List<Product> emptyProductList = new ArrayList<>();
        cart.setProducts(emptyProductList);
        cart.calculateTotalPrice();

        Cart cleanCart = cartRepository.save(cart);
        return new CartResponse(cleanCart);
    }

    private List<Product> buildProductListFromRequest(CartRequest cartRequest) {
        return cartRequest.products().stream()
                .map(productRequest -> {
                    PlatziProductResponse productResponse = productService.getProductById(productRequest.id())
                            .orElseThrow(() -> new DataNotFoundException("Produto não encontrado: id=" + productRequest.id()));
                    return Product.builder()
                            .id(productResponse.id())
                            .title(productResponse.title())
                            .price(productResponse.price())
                            .quantity(productRequest.quantity())
                            .build();
                }).toList();
    }

    private Cart getCartById(String id) {
        return cartRepository.findById(id)
                .orElseThrow(() -> new DataNotFoundException("Carrinho não encontrado."));
    }
}
