package br.com.expresscart.integration;

import br.com.expresscart.entity.Cart;
import br.com.expresscart.entity.PaymentMethod;
import br.com.expresscart.entity.Status;
import br.com.expresscart.support.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import static br.com.expresscart.support.PlatziStubs.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CartIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE = "/api/v1/cart";

    private ResultActions createCart(long clientId, String productsJson) throws Exception {
        return mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON)
                .content("{\"clientId\": %d, \"products\": %s}".formatted(clientId, productsJson)));
    }

    private String createCartAndGetId(long clientId, String productsJson) throws Exception {
        String body = createCart(clientId, productsJson)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.id");
    }

    @Test
    void fullLifecycle_createUpdateClearPay_thenSoldIsImmutable() throws Exception {
        stubProduct(1, "Camiseta", "10.50");
        stubProduct(2, "Boné", "20.00");

        // cria
        String id = createCartAndGetId(1, "[{\"id\": 1, \"quantity\": 2}, {\"id\": 2, \"quantity\": 1}]");
        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.totalPrice").value(41.0))
                .andExpect(jsonPath("$.products.length()").value(2))
                .andExpect(jsonPath("$.products[0].title").value("Camiseta"));

        // atualiza (substitui a lista)
        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\": 1, \"products\": [{\"id\": 2, \"quantity\": 3}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products.length()").value(1))
                .andExpect(jsonPath("$.totalPrice").value(60.0));

        // limpa
        mockMvc.perform(put(BASE + "/{id}/clear", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isEmpty())
                .andExpect(jsonPath("$.totalPrice").value(0));

        // paga
        mockMvc.perform(put(BASE + "/{id}/payment", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\": \"PIX\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLD"))
                .andExpect(jsonPath("$.paymentMethod").value("PIX"));

        Cart persisted = cartRepository.findById(id).orElseThrow();
        assertThat(persisted.getStatus()).isEqualTo(Status.SOLD);
        assertThat(persisted.getPaymentMethod()).isEqualTo(PaymentMethod.PIX);

        // SOLD é terminal (invariante I2)
        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clientId\": 1, \"products\": [{\"id\": 1, \"quantity\": 1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Não é possivel alterar os dados de um carrinho já vendido."));
        mockMvc.perform(put(BASE + "/{id}/clear", id))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Não é possivel limpar um carrinho já vendido."));
        mockMvc.perform(put(BASE + "/{id}/payment", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\": \"CREDIT\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Operação cancelada. O carrinho informado já foi pago."));
    }

    @Test
    void secondOpenCartForSameClientIsRejected_butAllowedAfterPayment() throws Exception {
        stubProduct(1, "Camiseta", "10.00");
        String first = createCartAndGetId(7, "[{\"id\": 1, \"quantity\": 1}]");

        createCart(7, "[{\"id\": 1, \"quantity\": 1}]")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Já existe um carrinho aberto para o cliente de id: 7"));

        mockMvc.perform(put(BASE + "/{id}/payment", first).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\": \"DEBIT\"}"))
                .andExpect(status().isOk());

        createCart(7, "[{\"id\": 1, \"quantity\": 1}]").andExpect(status().isCreated());
        assertThat(cartRepository.count()).isEqualTo(2);
    }

    @Test
    void unknownProductReturns404WithProductId() throws Exception {
        // Invariante I6
        stubUnknownProduct(999);

        createCart(1, "[{\"id\": 999, \"quantity\": 1}]")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Produto não encontrado: id=999"));

        assertThat(cartRepository.count()).isZero();
    }

    @Test
    void platziServerErrorReturns502AndDoesNotCreateCart() throws Exception {
        stubProductServerError(5);

        createCart(1, "[{\"id\": 5, \"quantity\": 1}]")
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("Erro ao consultar o catálogo de produtos (Platzi)"));

        assertThat(cartRepository.count()).isZero();
    }

    @Test
    void priceIsSnapshotAtTimeOfAdding() throws Exception {
        // Invariante I4: mudança de preço na Platzi não altera carrinho existente
        stubProduct(1, "Camiseta", "10.00");
        String id = createCartAndGetId(1, "[{\"id\": 1, \"quantity\": 1}]");

        PLATZI.resetAll();
        stubProduct(1, "Camiseta", "99.00");

        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(jsonPath("$.totalPrice").value(10.0))
                .andExpect(jsonPath("$.products[0].price").value(10.0));
    }

    @Test
    void invalidUpdateIsRejectedAndLeavesCartUntouched() throws Exception {
        stubProduct(1, "Camiseta", "10.00");
        String id = createCartAndGetId(1, "[{\"id\": 1, \"quantity\": 2}]");

        mockMvc.perform(put(BASE + "/{id}", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"products\": [{\"id\": 1, \"quantity\": 0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$[0].field").value("products[0].quantity"));

        mockMvc.perform(get(BASE + "/{id}", id))
                .andExpect(jsonPath("$.products[0].quantity").value(2))
                .andExpect(jsonPath("$.totalPrice").value(20.0));
    }

    @Test
    void paymentWithoutMethodIsRejectedAndCartStaysOpen() throws Exception {
        stubProduct(1, "Camiseta", "10.00");
        String id = createCartAndGetId(1, "[{\"id\": 1, \"quantity\": 1}]");

        mockMvc.perform(put(BASE + "/{id}/payment", id).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());

        assertThat(cartRepository.findById(id).orElseThrow().getStatus()).isEqualTo(Status.OPEN);
    }

    @Test
    void unknownPaymentMethodReturns400AndCartStaysOpen() throws Exception {
        stubProduct(1, "Camiseta", "10.00");
        String id = createCartAndGetId(1, "[{\"id\": 1, \"quantity\": 1}]");

        mockMvc.perform(put(BASE + "/{id}/payment", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentMethod\": \"BOLETO\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        assertThat(cartRepository.findById(id).orElseThrow().getStatus()).isEqualTo(Status.OPEN);
    }

    @Test
    void getUnknownCartReturns404() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", "000000000000000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Carrinho não encontrado."));
    }
}
