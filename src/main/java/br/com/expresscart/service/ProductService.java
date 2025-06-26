package br.com.expresscart.service;

import br.com.expresscart.client.PlatziStoreClient;
import br.com.expresscart.client.response.PlatziProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final PlatziStoreClient platziStoreClient;

    public List<PlatziProductResponse> getAllProducts() {
        return platziStoreClient.getAllProducts();
    }

    public Optional<PlatziProductResponse> getProductById(Long id) {
        return platziStoreClient.getProductById(id);
    }
}
