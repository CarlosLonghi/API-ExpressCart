package br.com.expresscart.controller;

import br.com.expresscart.client.PlatziStoreClient;
import br.com.expresscart.controller.request.CartRequest;
import br.com.expresscart.controller.response.CartResponse;
import br.com.expresscart.controller.response.PaymentResponse;
import br.com.expresscart.entity.PaymentMethod;
import br.com.expresscart.entity.Status;
import br.com.expresscart.exception.handler.BusinessException;
import br.com.expresscart.exception.handler.DataNotFoundException;
import br.com.expresscart.service.CartService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static br.com.expresscart.support.TestFixtures.*;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

    private static final String BASE = "/api/v1/cart";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CartService cartService;

    // @EnableFeignClients fica na classe principal; o mock evita subir o Feign no slice
    @MockitoBean
    private PlatziStoreClient platziStoreClient;

    private static CartResponse openCartResponse() {
        return new CartResponse(cart(Status.OPEN, cartItem(1L, "Camiseta", "10.50", 2)));
    }

    // ---------- POST /cart ----------

    @Test
    void create_returns201WithCart() throws Exception {
        when(cartService.createCart(any(CartRequest.class))).thenReturn(openCartResponse());

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"clientId": 1, "products": [{"id": 1, "quantity": 2}]}
                        """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(CART_ID))
                .andExpect(jsonPath("$.clientId").value(1))
                .andExpect(jsonPath("$.totalPrice").value(21.0))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.products[0].title").value("Camiseta"))
                .andExpect(jsonPath("$.paymentMethod").doesNotExist());
    }

    @Test
    void create_validationErrorsComeAsFieldList() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"clientId": 0, "products": [{"id": null, "quantity": 101}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[*].field",
                        containsInAnyOrder("clientId", "products[0].id", "products[0].quantity")));

        verifyNoInteractions(cartService);
    }

    @Test
    void create_clientIdMustBePositive() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"clientId": 0, "products": []}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("clientId"))
                .andExpect(jsonPath("$[0].message").value("O ID do cliente deve ser maior que 0"));
    }

    @Test
    void create_missingProductsIsRejected() throws Exception {
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"clientId": 1}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("products"))
                .andExpect(jsonPath("$[0].message")
                        .value("Informe a lista de Produtos que deseja adicionar ao carrinho"));
    }

    @Test
    void create_businessExceptionBecomes400Object() throws Exception {
        when(cartService.createCart(any()))
                .thenThrow(new BusinessException("Já existe um carrinho aberto para o cliente de id: 1"));

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                        {"clientId": 1, "products": [{"id": 1, "quantity": 1}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Já existe um carrinho aberto para o cliente de id: 1"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_malformedJsonReturns500() throws Exception {
        // B4: HttpMessageNotReadableException cai no handler genérico
        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content("{ not json"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno da aplicação"));
    }

    // ---------- GET /cart/{id} ----------

    @Test
    void get_returns200() throws Exception {
        when(cartService.findCartById(CART_ID)).thenReturn(openCartResponse());

        mockMvc.perform(get(BASE + "/{id}", CART_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CART_ID));
    }

    @Test
    void get_notFoundReturns404Object() throws Exception {
        when(cartService.findCartById("x")).thenThrow(new DataNotFoundException("Carrinho não encontrado."));

        mockMvc.perform(get(BASE + "/{id}", "x"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Carrinho não encontrado."));
    }

    // ---------- PUT /cart/{id} ----------

    @Test
    void update_returns200() throws Exception {
        when(cartService.updateCart(eq(CART_ID), any())).thenReturn(openCartResponse());

        mockMvc.perform(put(BASE + "/{id}", CART_ID).contentType(MediaType.APPLICATION_JSON).content("""
                        {"clientId": 1, "products": [{"id": 1, "quantity": 2}]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CART_ID));
    }

    @Test
    void update_acceptsBodyWithoutClientIdAndIgnoresExtraFields() throws Exception {
        when(cartService.updateCart(eq(CART_ID), any())).thenReturn(openCartResponse());

        mockMvc.perform(put(BASE + "/{id}", CART_ID).contentType(MediaType.APPLICATION_JSON).content("""
                        {"products": [{"id": 1, "quantity": 2}]}
                        """))
                .andExpect(status().isOk());

        verify(cartService).updateCart(CART_ID, cartUpdateRequest(item(1L, 2)));
    }

    @Test
    void update_validationErrorsComeAsFieldList() throws Exception {
        mockMvc.perform(put(BASE + "/{id}", CART_ID).contentType(MediaType.APPLICATION_JSON).content("""
                        {"products": [{"id": 1, "quantity": 0}, {"id": null, "quantity": 101}]}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[*].field", containsInAnyOrder(
                        "products[0].quantity", "products[1].id", "products[1].quantity")));

        verifyNoInteractions(cartService);
    }

    @Test
    void update_missingProductsIsRejected() throws Exception {
        mockMvc.perform(put(BASE + "/{id}", CART_ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("products"))
                .andExpect(jsonPath("$[0].message").value("Informe a lista de Produtos do carrinho"));

        verifyNoInteractions(cartService);
    }

    // ---------- PUT /cart/{id}/payment ----------

    @Test
    void pay_returns200WithPayment() throws Exception {
        when(cartService.payCart(eq(CART_ID), any()))
                .thenReturn(new PaymentResponse(CART_ID, Status.SOLD, PaymentMethod.PIX));

        mockMvc.perform(put(BASE + "/{id}/payment", CART_ID).contentType(MediaType.APPLICATION_JSON).content("""
                        {"paymentMethod": "PIX"}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(CART_ID))
                .andExpect(jsonPath("$.status").value("SOLD"))
                .andExpect(jsonPath("$.paymentMethod").value("PIX"));
    }

    @Test
    void pay_missingPaymentMethodIsRejected() throws Exception {
        mockMvc.perform(put(BASE + "/{id}/payment", CART_ID).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("paymentMethod"))
                .andExpect(jsonPath("$[0].message")
                        .value("Informe o método de pagamento para o carrinho (PIX / CREDIT / DEBIT)"));

        verifyNoInteractions(cartService);
    }

    @Test
    void pay_unknownPaymentMethodReturns500() throws Exception {
        // B4: enum inválido → HttpMessageNotReadableException → 500
        mockMvc.perform(put(BASE + "/{id}/payment", CART_ID).contentType(MediaType.APPLICATION_JSON).content("""
                        {"paymentMethod": "BOLETO"}
                        """))
                .andExpect(status().isInternalServerError());

        verifyNoInteractions(cartService);
    }

    @Test
    void pay_alreadyPaidReturns400() throws Exception {
        when(cartService.payCart(eq(CART_ID), any()))
                .thenThrow(new BusinessException("Operação cancelada. O carrinho informado já foi pago."));

        mockMvc.perform(put(BASE + "/{id}/payment", CART_ID).contentType(MediaType.APPLICATION_JSON).content("""
                        {"paymentMethod": "PIX"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Operação cancelada. O carrinho informado já foi pago."));
    }

    // ---------- PUT /cart/{id}/clear ----------

    @Test
    void clear_returns200WithCart() throws Exception {
        when(cartService.clearCart(CART_ID))
                .thenReturn(new CartResponse(cart(Status.OPEN)));

        mockMvc.perform(put(BASE + "/{id}/clear", CART_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isEmpty())
                .andExpect(jsonPath("$.totalPrice").value(0));
    }

    // ---------- Rotas / métodos ----------

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(delete(BASE + "/{id}", CART_ID))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405))
                .andExpect(jsonPath("$.message").value("Método HTTP 'DELETE' não é permitido para essa rota"));
    }

    @Test
    void unknownRouteReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Recurso não encontrado: /api/v1/nao-existe"));
    }
}
