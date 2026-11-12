package br.com.expresscart.service;

import br.com.expresscart.client.PlatziStoreClient;
import br.com.expresscart.client.response.PlatziProductResponse;
import br.com.expresscart.exception.handler.DataNotFoundException;
import br.com.expresscart.exception.handler.ExternalServiceException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@EnableCaching
@Slf4j
public class ProductService {

    private static final String PLATZI_UNAVAILABLE = "Erro ao consultar o catálogo de produtos (Platzi)";

    private final PlatziStoreClient platziStoreClient;

    @Cacheable(value = "products")
    public List<PlatziProductResponse> getAllProducts() {
        log.info("Buscando todos os produtos");

        try {
            return platziStoreClient.getAllProducts();
        } catch (FeignException e) {
            throw new ExternalServiceException(PLATZI_UNAVAILABLE, e);
        }
    }

    @Cacheable(value = "product", key = "#id")
    public PlatziProductResponse getProductById(Long id) {
        log.info("Buscando produto com id={}", id);

        try {
            return platziStoreClient.getProductById(id);
        } catch (FeignException.BadRequest e) {
            // A Platzi responde 400 (e não 404) quando o id não existe
            log.warn("Produto não encontrado: id={}", id);
            throw new DataNotFoundException("Produto não encontrado: id=" + id);
        } catch (FeignException e) {
            throw new ExternalServiceException(PLATZI_UNAVAILABLE, e);
        }
    }
}
