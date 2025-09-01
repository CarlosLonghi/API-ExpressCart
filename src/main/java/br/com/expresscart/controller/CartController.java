package br.com.expresscart.controller;

import br.com.expresscart.controller.api.docs.CartApi;
import br.com.expresscart.controller.request.CartRequest;
import br.com.expresscart.controller.request.PaymentRequest;
import br.com.expresscart.controller.response.CartResponse;
import br.com.expresscart.controller.response.PaymentResponse;
import br.com.expresscart.entity.Cart;
import br.com.expresscart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController implements CartApi {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<CartResponse> createCart(@RequestBody @Valid CartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.createCart(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CartResponse> getCartById(@PathVariable String id) {
        return ResponseEntity.ok(cartService.findCartById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CartResponse> updateCart(@PathVariable String id, @RequestBody CartRequest request) {
        return ResponseEntity.ok(cartService.updateCart(id, request));
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<PaymentResponse> payCart(@PathVariable String id, @RequestBody PaymentRequest request) {
        return ResponseEntity.ok(cartService.payCart(id, request));
    }

    @PutMapping("/{id}/clear")
    public ResponseEntity<CartResponse> clearCart(@PathVariable String id) {
        return ResponseEntity.ok(cartService.clearCart(id));
    }
}
