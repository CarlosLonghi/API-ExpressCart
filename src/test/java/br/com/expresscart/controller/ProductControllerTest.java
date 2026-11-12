package br.com.expresscart.controller;

import br.com.expresscart.client.PlatziStoreClient;
import br.com.expresscart.exception.handler.DataNotFoundException;
import br.com.expresscart.exception.handler.ExternalServiceException;
import br.com.expresscart.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static br.com.expresscart.support.TestFixtures.platziProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@ExtendWith(OutputCaptureExtension.class)
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
        when(productService.getProductById(1L)).thenReturn(platziProduct(1L, "Camiseta", "10.50"));

        mockMvc.perform(get(BASE + "/{id}", 1))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getById_dataNotFoundReturns404Object() throws Exception {
        when(productService.getProductById(1L))
                .thenThrow(new DataNotFoundException("Produto não encontrado: id=1"));

        mockMvc.perform(get(BASE + "/{id}", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Produto não encontrado: id=1"));
    }

    @Test
    void externalServiceFailureReturns502Object() throws Exception {
        when(productService.getAllProducts()).thenThrow(new ExternalServiceException(
                "Erro ao consultar o catálogo de produtos (Platzi)", new RuntimeException("timeout")));

        mockMvc.perform(get(BASE))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                .andExpect(jsonPath("$.message").value("Erro ao consultar o catálogo de produtos (Platzi)"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getById_nonNumericIdReturns400() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", "abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Valor inválido para o parâmetro 'id': abc"));

        verifyNoInteractions(productService);
    }

    @Test
    void list_unexpectedErrorReturns500WithoutLeakingDetailsAndLogsIt(CapturedOutput output) throws Exception {
        when(productService.getAllProducts()).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get(BASE))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Erro interno da aplicação"));

        assertThat(output).contains("Erro inesperado").contains("java.lang.RuntimeException: boom");
    }
}
