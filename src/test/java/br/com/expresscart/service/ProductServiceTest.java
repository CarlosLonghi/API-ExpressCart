package br.com.expresscart.service;

import br.com.expresscart.client.PlatziStoreClient;
import br.com.expresscart.client.response.PlatziProductResponse;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static br.com.expresscart.support.TestFixtures.platziProduct;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Testa o ProductService isolado (sem cache e sem o CustomErrorDecoder).
 * O comportamento real com Feign + decoder + Redis está em ProductIntegrationTest.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Request REQUEST = Request.create(
            Request.HttpMethod.GET, "https://platzi/products/1", Map.of(), null, StandardCharsets.UTF_8, null);

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
    void getAllProducts_wrapsAnyExceptionInRuntimeException() {
        IllegalStateException cause = new IllegalStateException("boom");
        when(platziStoreClient.getAllProducts()).thenThrow(cause);

        assertThatThrownBy(() -> productService.getAllProducts())
                .isExactlyInstanceOf(RuntimeException.class)
                .hasCause(cause);
    }

    @Test
    void getProductById_returnsProduct() {
        PlatziProductResponse product = platziProduct(1L, "A", "1.00");
        when(platziStoreClient.getProductById(1L)).thenReturn(product);

        assertThat(productService.getProductById(1L)).contains(product);
    }

    @Test
    void getProductById_returnsEmptyOnFeignBadRequest() {
        // B1: só acontece sem o CustomErrorDecoder; em produção o decoder lança DataNotFoundException antes
        when(platziStoreClient.getProductById(1L))
                .thenThrow(new FeignException.BadRequest("bad request", REQUEST, null, null));

        assertThat(productService.getProductById(1L)).isEqualTo(Optional.empty());
    }

    @Test
    void getProductById_rethrowsOtherFeignErrors() {
        FeignException error = new FeignException.InternalServerError("error", REQUEST, null, null);
        when(platziStoreClient.getProductById(1L)).thenThrow(error);

        assertThatThrownBy(() -> productService.getProductById(1L)).isSameAs(error);
    }
}
