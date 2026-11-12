package br.com.expresscart.config;

import br.com.expresscart.exception.handler.DataNotFoundException;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CustomErrorDecoderTest {

    private final CustomErrorDecoder decoder = new CustomErrorDecoder();

    private static Response response(int status) {
        Request request = Request.create(
                Request.HttpMethod.GET, "https://platzi/products/1", Map.of(), null, StandardCharsets.UTF_8, null);
        return Response.builder()
                .status(status)
                .request(request)
                .headers(Map.of())
                .build();
    }

    @Test
    void badRequest_becomesDataNotFound() {
        Exception exception = decoder.decode("PlatziStoreClient#getProductById(Long)", response(400));

        assertThat(exception)
                .isInstanceOf(DataNotFoundException.class)
                .hasMessage("Produto não encontrado (Platzi Client)");
    }

    @ParameterizedTest
    @ValueSource(ints = {401, 404, 429, 500, 503})
    void anyOtherStatus_becomesCheckedGenericException(int status) {
        // B2: retorna uma Exception checada (não RuntimeException) para todo erro que não seja 400
        Exception exception = decoder.decode("PlatziStoreClient#getProductById(Long)", response(status));

        assertThat(exception)
                .isExactlyInstanceOf(Exception.class)
                .hasMessage("Erro ao buscar produto (Platzi Client)");
    }
}
