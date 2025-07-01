package br.com.expresscart.controller;

import br.com.expresscart.controller.request.CartRequest;
import br.com.expresscart.entity.Cart;
import br.com.expresscart.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @PostMapping
    public ResponseEntity<Cart> createCart(@RequestBody CartRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED).body(cartService.createCart(request));
    }
}
