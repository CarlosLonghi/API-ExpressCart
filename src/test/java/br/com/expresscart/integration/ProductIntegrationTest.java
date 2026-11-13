package br.com.expresscart.integration;

import br.com.expresscart.config.CacheConfig;
import br.com.expresscart.support.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Set;

import static br.com.expresscart.support.PlatziStubs.*;
import static org.assertj.core.api.Assertions.assertThat;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductIntegrationTest extends AbstractIntegrationTest {

    private static final String BASE = "/api/v1/product";

    @Autowired
    private StringRedisTemplate redis;

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
    void cachedProductIsStoredAsJsonWithItsOwnTtl() throws Exception {
        stubProduct(1, "Camiseta", "10.50");
        mockMvc.perform(get(BASE + "/{id}", 1)).andExpect(status().isOk());

        String key = CacheConfig.PRODUCT_CACHE + "::1";
        assertThat(redis.opsForValue().get(key))
                .startsWith("{")
                .contains("\"title\":\"Camiseta\"", "\"price\":10.50", "\"name\":\"Demo Category\"");
        assertThat(redis.getExpire(key)).isBetween(1L, 60L);
    }

    @Test
    void cachedProductListIsStoredAsJsonArray() throws Exception {
        stubProductList("[" + productJson(1, "Camiseta", "10.50") + "," + productJson(2, "Boné", "20") + "]");
        mockMvc.perform(get(BASE)).andExpect(status().isOk());

        Set<String> keys = redis.keys(CacheConfig.PRODUCT_LIST_CACHE + "::*");
        assertThat(keys).hasSize(1);
        assertThat(redis.opsForValue().get(keys.iterator().next()))
                .startsWith("[")
                .contains("\"title\":\"Boné\"");

        // a leitura do cache devolve os mesmos dados
        mockMvc.perform(get(BASE))
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].price").value(10.5))
                .andExpect(jsonPath("$[1].category.name").value("Demo Category"));
        PLATZI.verify(1, getRequestedFor(urlEqualTo("/products")));
    }

    @Test
    void unknownProductReturns404WithProductId() throws Exception {
        stubUnknownProduct(999);

        mockMvc.perform(get(BASE + "/{id}", 999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Produto não encontrado: id=999"));
    }

    @Test
    void unknownProductIsNotCached() throws Exception {
        stubUnknownProduct(999);
        mockMvc.perform(get(BASE + "/{id}", 999)).andExpect(status().isNotFound());

        stubProduct(999, "Novo", "5.00");
        mockMvc.perform(get(BASE + "/{id}", 999))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Novo"));
    }

    @Test
    void platziServerErrorReturns502() throws Exception {
        stubProductServerError(5);

        mockMvc.perform(get(BASE + "/{id}", 5))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("Erro ao consultar o catálogo de produtos (Platzi)"));
    }

    @Test
    void platziConnectionFailureReturns502() throws Exception {
        stubProductListConnectionReset();

        mockMvc.perform(get(BASE))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
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
