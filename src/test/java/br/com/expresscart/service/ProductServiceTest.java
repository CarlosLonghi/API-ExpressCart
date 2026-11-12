package br.com.expresscart.service;

import br.com.expresscart.client.PlatziStoreClient;
import br.com.expresscart.client.response.PlatziProductResponse;
import br.com.expresscart.exception.handler.DataNotFoundException;
import br.com.expresscart.exception.handler.ExternalServiceException;
import feign.FeignException;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static br.com.expresscart.support.TestFixtures.platziProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Testa o ProductService isolado (sem cache).
 * O comportamento com Feign + Redis reais está em ProductIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Request REQUEST = Request.create(
            Request.HttpMethod.GET, "https://platzi/products/1", Map.of(), null, StandardCharsets.UTF_8, null);
    private static final String UNAVAILABLE = "Erro ao consultar o catálogo de produtos (Platzi)";

    @Mock
    private PlatziStoreClient platziStoreClient;

    @InjectMocks
    private ProductService productService;

    @Test
    void getAllProducts_delegatesToClient() {
        List<PlatziProductResponse> products = List.of(platziProduct(1L, "A", "1.00"));
        when(platziStoreClient.getAllProducts()).thenReturn(products);

        assertThat(productService.getAllProducts()).isEqualTo(products);
    }

    @Test
    void getAllProducts_translatesFeignErrorToExternalServiceException() {
        FeignException error = new FeignException.InternalServerError("error", REQUEST, null, null);
        when(platziStoreClient.getAllProducts()).thenThrow(error);

        assertThatThrownBy(() -> productService.getAllProducts())
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage(UNAVAILABLE)
                .hasCause(error);
    }

    @Test
    void getProductById_returnsProduct() {
        PlatziProductResponse product = platziProduct(1L, "A", "1.00");
        when(platziStoreClient.getProductById(1L)).thenReturn(product);

        assertThat(productService.getProductById(1L)).isEqualTo(product);
    }

    @Test
    void getProductById_translatesBadRequestToNotFoundWithId() {
        // A Platzi responde 400 para id inexistente
        when(platziStoreClient.getProductById(99L))
                .thenThrow(new FeignException.BadRequest("bad request", REQUEST, null, null));

        assertThatThrownBy(() -> productService.getProductById(99L))
                .isInstanceOf(DataNotFoundException.class)
                .hasMessage("Produto não encontrado: id=99");
    }

    @Test
    void getProductById_translatesServerErrorToExternalServiceException() {
        FeignException error = new FeignException.InternalServerError("error", REQUEST, null, null);
        when(platziStoreClient.getProductById(1L)).thenThrow(error);

        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(ExternalServiceException.class)
                .hasMessage(UNAVAILABLE)
                .hasCause(error);
    }

    @Test
    void getProductById_translatesConnectionFailureToExternalServiceException() {
        FeignException error = new RetryableException(
                -1, "connection refused", Request.HttpMethod.GET, (Long) null, REQUEST);
        when(platziStoreClient.getProductById(1L)).thenThrow(error);

        assertThatThrownBy(() -> productService.getProductById(1L))
                .isInstanceOf(ExternalServiceException.class)
                .hasCause(error);
    }
}
