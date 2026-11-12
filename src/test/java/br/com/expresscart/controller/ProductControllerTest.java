package br.com.expresscart.controller;

import br.com.expresscart.client.PlatziStoreClient;
import br.com.expresscart.exception.handler.DataNotFoundException;
import br.com.expresscart.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static br.com.expresscart.support.TestFixtures.platziProduct;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    private static final String BASE = "/api/v1/product";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private PlatziStoreClient platziStoreClient;

    @Test
    void list_returns200WithExternalProductShape() throws Exception {
        // A2: expõe o DTO do cliente externo (inclui images e category)
        when(productService.getAllProducts()).thenReturn(List.of(platziProduct(1L, "Camiseta", "10.50")));

        mockMvc.perform(get(BASE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].title").value("Camiseta"))
                .andExpect(jsonPath("$[0].price").value(10.5))
                .andExpect(jsonPath("$[0].images[0]").exists())
                .andExpect(jsonPath("$[0].category.name").value("Demo Category"));
    }

    @Test
    void list_emptyReturns204() throws Exception {
        when(productService.getAllProducts()).thenReturn(List.of());

        mockMvc.perform(get(BASE))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void getById_returns200() throws Exception {
        when(productService.getProductById(1L)).thenReturn(Optional.of(platziProduct(1L, "Camiseta", "10.50")));

        mockMvc.perform(get(BASE + "/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_emptyOptionalReturns404WithoutBody() throws Exception {
        // B1/A5: caminho inalcançável em produção; responde 404 sem o corpo padrão de erro
        when(productService.getProductById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get(BASE + "/{id}", 1))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
    }

    @Test
    void getById_dataNotFoundReturns404Object() throws Exception {
        when(productService.getProductById(1L))
                .thenThrow(new DataNotFoundException("Produto não encontrado (Platzi Client)"));

        mockMvc.perform(get(BASE + "/{id}", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Produto não encontrado (Platzi Client)"));
    }

    @Test
    void getById_nonNumericIdReturns500() throws Exception {
        // B4: MethodArgumentTypeMismatchException cai no handler genérico
        mockMvc.perform(get(BASE + "/{id}", "abc"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Erro interno da aplicação"));

        verifyNoInteractions(productService);
    }

    @Test
    void list_unexpectedErrorReturns500() throws Exception {
        when(productService.getAllProducts()).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get(BASE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Erro interno da aplicação"));
    }
}
