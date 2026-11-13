package br.com.expresscart.service;

import br.com.expresscart.controller.request.CartRequest;
import br.com.expresscart.controller.request.CartUpdateRequest;
import br.com.expresscart.controller.request.PaymentRequest;
import br.com.expresscart.controller.request.ProductRequest;
import br.com.expresscart.controller.response.CartResponse;
import br.com.expresscart.controller.response.PaymentResponse;
import br.com.expresscart.controller.response.ProductResponse;
import br.com.expresscart.entity.Cart;
import br.com.expresscart.entity.Product;
import br.com.expresscart.entity.Status;
import br.com.expresscart.exception.BusinessException;
import br.com.expresscart.exception.DataNotFoundException;
import br.com.expresscart.repository.CartRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Orquestra os casos de uso do carrinho: carrega, consulta o catálogo e salva.
 * As regras de estado (carrinho vendido, cálculo do total) ficam na entidade {@link Cart}.
 */
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

        Cart cart = Cart.open(cartRequest.clientId(), buildProductList(cartRequest.products()));

        return new CartResponse(cartRepository.save(cart));
    }

    public CartResponse findCartById(String id) {
        return new CartResponse(getCartById(id));
    }

    public CartResponse updateCart(String cartId, CartUpdateRequest cartRequest) {
        Cart cart = getCartById(cartId);

        // Recusa carrinho vendido antes de consultar o catálogo
        cart.ensureItemsCanChange();
        cart.replaceItems(buildProductList(cartRequest.products()));

        return new CartResponse(cartRepository.save(cart));
    }

    public PaymentResponse payCart(String cartId, PaymentRequest paymentRequest) {
        Cart cart = getCartById(cartId);

        cart.pay(paymentRequest.paymentMethod());

        return new PaymentResponse(cartRepository.save(cart));
    }

    public CartResponse clearCart(String cartId) {
        Cart cart = getCartById(cartId);

        cart.clear();

        return new CartResponse(cartRepository.save(cart));
    }

    private List<Product> buildProductList(List<ProductRequest> productRequests) {
        return productRequests.stream()
                .map(productRequest -> {
                    ProductResponse productResponse = productService.getProductById(productRequest.id());
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
