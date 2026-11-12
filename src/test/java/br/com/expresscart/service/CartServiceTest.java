package br.com.expresscart.service;

import br.com.expresscart.controller.request.PaymentRequest;
import br.com.expresscart.controller.response.CartResponse;
import br.com.expresscart.controller.response.PaymentResponse;
import br.com.expresscart.entity.Cart;
import br.com.expresscart.entity.PaymentMethod;
import br.com.expresscart.entity.Status;
import br.com.expresscart.exception.handler.BusinessException;
import br.com.expresscart.exception.handler.DataNotFoundException;
import br.com.expresscart.repository.CartRepository;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static br.com.expresscart.support.TestFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private CartService cartService;

    private void saveReturnsArgument() {
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Nested
    class CreateCart {

        @Test
        void createsOpenCartWithProductSnapshotAndTotal() {
            when(cartRepository.findByClientIdAndStatus(CLIENT_ID, Status.OPEN)).thenReturn(Optional.empty());
            when(productService.getProductById(1L)).thenReturn(platziProduct(1L, "Camiseta", "10.50"));
            when(productService.getProductById(2L)).thenReturn(platziProduct(2L, "Boné", "20.00"));
            saveReturnsArgument();

            CartResponse response = cartService.createCart(cartRequest(CLIENT_ID, item(1L, 2), item(2L, 1)));

            assertThat(response.clientId()).isEqualTo(CLIENT_ID);
            assertThat(response.status()).isEqualTo(Status.OPEN);
            assertThat(response.totalPrice()).isEqualByComparingTo("41.00");
            assertThat(response.products())
                    .extracting("id", "title", "quantity")
                    .containsExactly(
                            tuple(1L, "Camiseta", 2),
                            tuple(2L, "Boné", 1)
                    );

            ArgumentCaptor<Cart> saved = ArgumentCaptor.forClass(Cart.class);
            verify(cartRepository).save(saved.capture());
            assertThat(saved.getValue().getId()).isNull();
            assertThat(saved.getValue().getPaymentMethod()).isNull();
        }

        @Test
        void rejectsSecondOpenCartForSameClient() {
            // Invariante I1
            when(cartRepository.findByClientIdAndStatus(CLIENT_ID, Status.OPEN))
                    .thenReturn(Optional.of(cart(Status.OPEN)));

            assertThatThrownBy(() -> cartService.createCart(cartRequest(CLIENT_ID, item(1L, 1))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Já existe um carrinho aberto para o cliente de id: 1");

            verify(cartRepository, never()).save(any());
            verifyNoInteractions(productService);
        }

        @Test
        void propagatesNotFoundFromProductServiceWithoutSaving() {
            when(cartRepository.findByClientIdAndStatus(CLIENT_ID, Status.OPEN)).thenReturn(Optional.empty());
            when(productService.getProductById(99L))
                    .thenThrow(new DataNotFoundException("Produto não encontrado: id=99"));

            assertThatThrownBy(() -> cartService.createCart(cartRequest(CLIENT_ID, item(99L, 1))))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessage("Produto não encontrado: id=99");

            verify(cartRepository, never()).save(any());
        }

        @Test
        void keepsDuplicatedProductIdsAsSeparateLines() {
            // B8: itens repetidos não são agrupados
            when(cartRepository.findByClientIdAndStatus(CLIENT_ID, Status.OPEN)).thenReturn(Optional.empty());
            when(productService.getProductById(1L)).thenReturn(platziProduct(1L, "Camiseta", "10.00"));
            saveReturnsArgument();

            CartResponse response = cartService.createCart(cartRequest(CLIENT_ID, item(1L, 1), item(1L, 2)));

            assertThat(response.products()).hasSize(2);
            assertThat(response.totalPrice()).isEqualByComparingTo("30.00");
        }

        @Test
        void allowsEmptyProductList() {
            // B8: @NotNull aceita lista vazia
            when(cartRepository.findByClientIdAndStatus(CLIENT_ID, Status.OPEN)).thenReturn(Optional.empty());
            saveReturnsArgument();

            CartResponse response = cartService.createCart(cartRequest(CLIENT_ID));

            assertThat(response.products()).isEmpty();
            assertThat(response.totalPrice()).isEqualByComparingTo("0");
        }
    }

    @Nested
    class FindCartById {

        @Test
        void returnsCart() {
            when(cartRepository.findById(CART_ID))
                    .thenReturn(Optional.of(cart(Status.OPEN, cartItem(1L, "A", "5.00", 2))));

            CartResponse response = cartService.findCartById(CART_ID);

            assertThat(response.id()).isEqualTo(CART_ID);
            assertThat(response.totalPrice()).isEqualByComparingTo("10.00");
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.findCartById(CART_ID))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessage("Carrinho não encontrado.");
        }
    }

    @Nested
    class UpdateCart {

        @Test
        void replacesItemsAndRecalculatesTotal() {
            // Invariante I5: substitui a lista inteira
            when(cartRepository.findById(CART_ID))
                    .thenReturn(Optional.of(cart(Status.OPEN, cartItem(1L, "Antigo", "100.00", 1))));
            when(productService.getProductById(2L)).thenReturn(platziProduct(2L, "Novo", "7.00"));
            saveReturnsArgument();

            CartResponse response = cartService.updateCart(CART_ID, cartUpdateRequest(item(2L, 3)));

            assertThat(response.products()).singleElement()
                    .satisfies(p -> assertThat(p.getId()).isEqualTo(2L));
            assertThat(response.totalPrice()).isEqualByComparingTo("21.00");
        }

        @Test
        void keepsCartOwnerAndStatus() {
            // Invariante I5: o update só troca os itens
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cart(Status.OPEN)));
            when(productService.getProductById(1L)).thenReturn(platziProduct(1L, "A", "1.00"));
            saveReturnsArgument();

            CartResponse response = cartService.updateCart(CART_ID, cartUpdateRequest(item(1L, 1)));

            assertThat(response.clientId()).isEqualTo(CLIENT_ID);
            assertThat(response.status()).isEqualTo(Status.OPEN);
        }

        @Test
        void rejectsSoldCart() {
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cart(Status.SOLD)));

            assertThatThrownBy(() -> cartService.updateCart(CART_ID, cartUpdateRequest(item(1L, 1))))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Não é possivel alterar os dados de um carrinho já vendido.");

            verify(cartRepository, never()).save(any());
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.updateCart(CART_ID, cartUpdateRequest(item(1L, 1))))
                    .isInstanceOf(DataNotFoundException.class)
                    .hasMessage("Carrinho não encontrado.");
        }
    }

    @Nested
    class PayCart {

        @Test
        void marksCartAsSoldWithPaymentMethod() {
            when(cartRepository.findById(CART_ID))
                    .thenReturn(Optional.of(cart(Status.OPEN, cartItem(1L, "A", "5.00", 1))));
            saveReturnsArgument();

            PaymentResponse response = cartService.payCart(CART_ID, new PaymentRequest(PaymentMethod.PIX));

            assertThat(response.id()).isEqualTo(CART_ID);
            assertThat(response.status()).isEqualTo(Status.SOLD);
            assertThat(response.paymentMethod()).isEqualTo(PaymentMethod.PIX);
        }

        @Test
        void rejectsAlreadyPaidCart() {
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cart(Status.SOLD)));

            assertThatThrownBy(() -> cartService.payCart(CART_ID, new PaymentRequest(PaymentMethod.PIX)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Operação cancelada. O carrinho informado já foi pago.");

            verify(cartRepository, never()).save(any());
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.payCart(CART_ID, new PaymentRequest(PaymentMethod.PIX)))
                    .isInstanceOf(DataNotFoundException.class);
        }
    }

    @Nested
    class ClearCart {

        @Test
        void removesAllItemsAndZeroesTotal() {
            when(cartRepository.findById(CART_ID))
                    .thenReturn(Optional.of(cart(Status.OPEN, cartItem(1L, "A", "5.00", 3))));
            saveReturnsArgument();

            CartResponse response = cartService.clearCart(CART_ID);

            assertThat(response.products()).isEmpty();
            assertThat(response.totalPrice()).isEqualByComparingTo("0");
            assertThat(response.status()).isEqualTo(Status.OPEN);
        }

        @Test
        void rejectsSoldCart() {
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.of(cart(Status.SOLD)));

            assertThatThrownBy(() -> cartService.clearCart(CART_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessage("Não é possivel limpar um carrinho já vendido.");

            verify(cartRepository, never()).save(any());
        }

        @Test
        void throwsNotFoundWhenMissing() {
            when(cartRepository.findById(CART_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> cartService.clearCart(CART_ID))
                    .isInstanceOf(DataNotFoundException.class);
        }
    }
}
