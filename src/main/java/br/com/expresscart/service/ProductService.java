package br.com.expresscart.service;

import br.com.expresscart.client.PlatziStoreClient;
import br.com.expresscart.client.response.PlatziCategoryResponse;
import br.com.expresscart.client.response.PlatziProductResponse;
import br.com.expresscart.config.CacheConfig;
import br.com.expresscart.controller.response.CategoryResponse;
import br.com.expresscart.controller.response.ProductResponse;
import br.com.expresscart.exception.handler.DataNotFoundException;
import br.com.expresscart.exception.handler.ExternalServiceException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Catálogo de produtos. É a única classe que conhece a API da Platzi:
 * consulta, traduz os erros e converte a resposta para o modelo desta API.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private static final String PLATZI_UNAVAILABLE = "Erro ao consultar o catálogo de produtos (Platzi)";

    private final PlatziStoreClient platziStoreClient;

    @Cacheable(CacheConfig.PRODUCT_LIST_CACHE)
    public List<ProductResponse> getAllProducts() {
        log.info("Buscando todos os produtos");

        try {
            return platziStoreClient.getAllProducts().stream()
                    .map(ProductService::toResponse)
                    .toList();
        } catch (FeignException e) {
            throw new ExternalServiceException(PLATZI_UNAVAILABLE, e);
        }
    }

    @Cacheable(value = CacheConfig.PRODUCT_CACHE, key = "#id")
    public ProductResponse getProductById(Long id) {
        log.info("Buscando produto com id={}", id);

        try {
            return toResponse(platziStoreClient.getProductById(id));
        } catch (FeignException.BadRequest e) {
            // A Platzi responde 400 (e não 404) quando o id não existe
            log.warn("Produto não encontrado: id={}", id);
            throw new DataNotFoundException("Produto não encontrado: id=" + id);
        } catch (FeignException e) {
            throw new ExternalServiceException(PLATZI_UNAVAILABLE, e);
        }
    }

    private static ProductResponse toResponse(PlatziProductResponse product) {
        return new ProductResponse(
                product.id(),
                product.title(),
                product.price(),
                product.images() == null ? List.of() : List.copyOf(product.images()),
                toResponse(product.category())
        );
    }

    private static CategoryResponse toResponse(PlatziCategoryResponse category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(category.id(), category.name(), category.image());
    }
}
