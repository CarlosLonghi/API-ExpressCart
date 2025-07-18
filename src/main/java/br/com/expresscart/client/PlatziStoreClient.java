package br.com.expresscart.client;

import br.com.expresscart.client.response.PlatziProductResponse;
import br.com.expresscart.exceptions.CustomErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(
        name = "platzi-store-client",
        url = "${expresscart.client.platzi}",
        configuration = {CustomErrorDecoder.class}
)
public interface PlatziStoreClient {

    @GetMapping("/products")
    List<PlatziProductResponse> getAllProducts();

    @GetMapping("products/{id}")
    PlatziProductResponse getProductById(@PathVariable Long id);
}
