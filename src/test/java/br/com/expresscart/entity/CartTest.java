package br.com.expresscart.entity;

import br.com.expresscart.exception.BusinessException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static br.com.expresscart.support.TestFixtures.CLIENT_ID;
import static br.com.expresscart.support.TestFixtures.cartItem;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CartTest {

    private static Cart openCart(CartItem... items) {
        return Cart.open(CLIENT_ID, List.of(items));
    }

    private static Cart soldCart(CartItem... items) {
        Cart cart = openCart(items);
        cart.pay(PaymentMethod.PIX);
        return cart;
    }

    @Nested
    class Open {

        @Test
        void startsOpenWithoutPaymentAndWithTotalCalculated() {
            Cart cart = openCart(cartItem(1L, "A", "10.50", 2), cartItem(2L, "B", "20.00", 1));

            assertThat(cart.getClientId()).isEqualTo(CLIENT_ID);
            assertThat(cart.getStatus()).isEqualTo(Status.OPEN);
            assertThat(cart.isSold()).isFalse();
            assertThat(cart.getPaymentMethod()).isNull();
            assertThat(cart.getId()).isNull();
            assertThat(cart.getTotalPrice()).isEqualByComparingTo("41.00");
        }

        @Test
        void totalKeepsBigDecimalPrecision() {
            assertThat(openCart(cartItem(1L, "A", "0.10", 3)).getTotalPrice()).isEqualByComparingTo("0.30");
        }

        @Test
        void emptyCartHasZeroTotal() {
            Cart cart = openCart();

            assertThat(cart.getProducts()).isEmpty();
            assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void requiresClientAndItems() {
            assertThatThrownBy(() -> Cart.open(null, List.of())).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> Cart.open(CLIENT_ID, null)).isInstanceOf(NullPointerException.class);
        }

        @Test
        void itemsAreCopiedAndCannotBeChangedFromOutside() {
            List<CartItem> source = new ArrayList<>(List.of(cartItem(1L, "A", "10.00", 1)));
            Cart cart = Cart.open(CLIENT_ID, source);

            source.add(cartItem(2L, "B", "99.00", 1));

            assertThat(cart.getProducts()).hasSize(1);
            assertThat(cart.getTotalPrice()).isEqualByComparingTo("10.00");
            assertThatThrownBy(() -> cart.getProducts().add(cartItem(3L, "C", "1.00", 1)))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class ReplaceItems {

        @Test
        void swapsItemsAndRecalculatesTotal() {
            // Invariantes I3 e I5
            Cart cart = openCart(cartItem(1L, "Antigo", "100.00", 1));

            cart.replaceItems(List.of(cartItem(2L, "Novo", "7.00", 3)));

            assertThat(cart.getProducts()).extracting(CartItem::getId).containsExactly(2L);
            assertThat(cart.getTotalPrice()).isEqualByComparingTo("21.00");
            assertThat(cart.getStatus()).isEqualTo(Status.OPEN);
        }

        @Test
        void isRejectedOnSoldCart() {
            // Invariante I2
            Cart cart = soldCart(cartItem(1L, "A", "10.00", 1));

            assertThatThrownBy(() -> cart.replaceItems(List.of()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Não é possivel alterar os dados de um carrinho já vendido.");
            assertThatThrownBy(cart::ensureItemsCanChange)
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Não é possivel alterar os dados de um carrinho já vendido.");

            assertThat(cart.getProducts()).hasSize(1);
            assertThat(cart.getTotalPrice()).isEqualByComparingTo("10.00");
        }

        @Test
        void ensureItemsCanChangePassesOnOpenCart() {
            openCart().ensureItemsCanChange();
        }
    }

    @Nested
    class Clear {

        @Test
        void removesItemsAndZeroesTotal() {
            Cart cart = openCart(cartItem(1L, "A", "5.00", 3));

            cart.clear();

            assertThat(cart.getProducts()).isEmpty();
            assertThat(cart.getTotalPrice()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(cart.getStatus()).isEqualTo(Status.OPEN);
        }

        @Test
        void isRejectedOnSoldCart() {
            Cart cart = soldCart(cartItem(1L, "A", "5.00", 3));

            assertThatThrownBy(cart::clear)
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Não é possivel limpar um carrinho já vendido.");

            assertThat(cart.getProducts()).hasSize(1);
        }
    }

    @Nested
    class Pay {

        @Test
        void marksAsSoldWithPaymentMethodAndKeepsItems() {
            Cart cart = openCart(cartItem(1L, "A", "5.00", 1));

            cart.pay(PaymentMethod.CREDIT);

            assertThat(cart.getStatus()).isEqualTo(Status.SOLD);
            assertThat(cart.isSold()).isTrue();
            assertThat(cart.getPaymentMethod()).isEqualTo(PaymentMethod.CREDIT);
            assertThat(cart.getTotalPrice()).isEqualByComparingTo("5.00");
        }

        @Test
        void isRejectedOnSoldCartAndKeepsOriginalMethod() {
            Cart cart = soldCart();

            assertThatThrownBy(() -> cart.pay(PaymentMethod.DEBIT))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Operação cancelada. O carrinho informado já foi pago.");

            assertThat(cart.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);
        }

        @Test
        void requiresPaymentMethodAndStaysOpenWithoutIt() {
            Cart cart = openCart();

            assertThatThrownBy(() -> cart.pay(null)).isInstanceOf(NullPointerException.class);

            assertThat(cart.getStatus()).isEqualTo(Status.OPEN);
        }
    }

    @Test
    void itemSubtotalIsPriceTimesQuantity() {
        assertThat(cartItem(1L, "A", "10.50", 4).subtotal()).isEqualByComparingTo("42.00");
    }
}
