package br.com.expresscart.entity;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static br.com.expresscart.support.TestFixtures.cartItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    @Test
    void calculateTotalPrice_sumsPriceTimesQuantityOfEveryItem() {
        Cart cart = Cart.builder()
                .products(List.of(
                        cartItem(1L, "A", "10.50", 2),
                        cartItem(2L, "B", "20.00", 1)
                ))
                .build();

        cart.calculateTotalPrice();

        assertThat(cart.getTotalPrice()).isEqualByComparingTo("41.00");
    }

    @Test
    void calculateTotalPrice_keepsBigDecimalPrecision() {
        Cart cart = Cart.builder()
                .products(List.of(cartItem(1L, "A", "0.10", 3)))
                .build();

        cart.calculateTotalPrice();

        assertThat(cart.getTotalPrice()).isEqualByComparingTo("0.30");
    }

    @Test
    void calculateTotalPrice_isZeroForEmptyCart() {
        Cart cart = Cart.builder().products(List.of()).build();

        cart.calculateTotalPrice();

        assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void totalPrice_isNullUntilCalculated() {
        Cart cart = Cart.builder()
                .products(List.of(cartItem(1L, "A", "10.00", 1)))
                .build();

        assertThat(cart.getTotalPrice()).isNull();
    }

    @Test
    void calculateTotalPrice_throwsNpeWhenProductsIsNull() {
        // B8: sem proteção contra lista nula
        Cart cart = Cart.builder().build();

        assertThatThrownBy(cart::calculateTotalPrice).isInstanceOf(NullPointerException.class);
    }
}
