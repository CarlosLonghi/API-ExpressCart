package br.com.expresscart.service;

import br.com.expresscart.client.PlatziStoreClient;
import br.com.expresscart.client.response.PlatziProductResponse;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@EnableCaching
@Slf4j
public class ProductService {

    private final PlatziStoreClient platziStoreClient;

    @Cacheable(value = "products")
    public List<PlatziProductResponse> getAllProducts() {
        log.info("Buscando todos os produtos");

        try {
            return platziStoreClient.getAllProducts();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Cacheable(value = "product", key = "#id")
    public Optional<PlatziProductResponse> getProductById(Long id) {
        log.info("Buscando produto com id={}", id);

        try {
            PlatziProductResponse platziProductResponse = platziStoreClient.getProductById(id);
            return Optional.of(platziProductResponse);
        } catch (FeignException e) {
            if (e.status() == HttpStatus.BAD_REQUEST.value()) {
                log.warn("Produto não encontrado: id={}", id);
                return Optional.empty();
            }
            throw e;
        }
    }
}
