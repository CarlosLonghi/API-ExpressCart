package br.com.expresscart.integration;

import br.com.expresscart.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static br.com.expresscart.support.PlatziStubs.*;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE = "/api/v1/product";

    @Test
    void listReturnsProductsFromPlatzi() throws Exception {
        stubProductList("[" + productJson(1, "Camiseta", "10.50") + "," + productJson(2, "Boné", "20") + "]");

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[1].title").value("Boné"));
    }

    @Test
    void emptyListReturns204() throws Exception {
        stubProductList("[]");

        mockMvc.perform(get(BASE)).andExpect(status().isNoContent());
    }

    @Test
    void productByIdIsCachedInRedis() throws Exception {
        // Invariante I8: a segunda chamada não chega na Platzi
        stubProduct(1, "Camiseta", "10.50");

        mockMvc.perform(get(BASE + "/{id}", 1)).andExpect(status().isOk());
        mockMvc.perform(get(BASE + "/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Camiseta"));

        PLATZI.verify(1, getRequestedFor(urlEqualTo("/products/1")));
    }

    @Test
    void productListIsCachedInRedis() throws Exception {
        stubProductList("[" + productJson(1, "Camiseta", "10.50") + "]");

        mockMvc.perform(get(BASE)).andExpect(status().isOk());
        mockMvc.perform(get(BASE)).andExpect(status().isOk());

        PLATZI.verify(1, getRequestedFor(urlEqualTo("/products")));
    }

    @Test
    void unknownProductReturns404FromDecoder() throws Exception {
        // B1: o 404 vem do DataNotFoundException do decoder, não do Optional.empty() do controller
        stubUnknownProduct(999);

        mockMvc.perform(get(BASE + "/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Produto não encontrado (Platzi Client)"));
    }

    @Test
    void cartCreationReusesProductCache() throws Exception {
        stubProduct(1, "Camiseta", "10.00");

        mockMvc.perform(get(BASE + "/{id}", 1)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/cart")
                        .contentType("application/json")
                        .content("{\"clientId\": 1, \"products\": [{\"id\": 1, \"quantity\": 1}]}"))
                .andExpect(status().isCreated());

        PLATZI.verify(1, getRequestedFor(urlEqualTo("/products/1")));
    }
}
