package br.com.expresscart.support;

import com.github.tomakehurst.wiremock.http.Fault;

import static br.com.expresscart.support.AbstractIntegrationTest.PLATZI;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

/** Stubs do WireMock imitando as respostas da Platzi Fake Store API. */
public final class PlatziStubs {

    private PlatziStubs() {
    }

    public static String productJson(long id, String title, String price) {
        return """
                {"id": %d, "title": "%s", "price": %s,
                 "images": ["https://placehold.co/600x400"],
                 "category": {"id": 1, "name": "Demo Category", "image": "https://placehold.co/640x480"}}
                """.formatted(id, title, price);
    }

    public static void stubProduct(long id, String title, String price) {
        PLATZI.stubFor(get(urlEqualTo("/products/" + id))
                .willReturn(okJson(productJson(id, title, price))));
    }

    /** A Platzi responde 400 (não 404) para um id inexistente. */
    public static void stubUnknownProduct(long id) {
        PLATZI.stubFor(get(urlEqualTo("/products/" + id))
                .willReturn(aResponse().withStatus(400).withHeader("Content-Type", "application/json")
                        .withBody("""
                                {"path": "/api/v1/products/%d", "message": "Could not find any entity"}
                                """.formatted(id))));
    }

    public static void stubProductServerError(long id) {
        PLATZI.stubFor(get(urlEqualTo("/products/" + id)).willReturn(serverError()));
    }

    /** Derruba a conexão no meio da resposta, simulando a Platzi fora do ar. */
    public static void stubProductListConnectionReset() {
        PLATZI.stubFor(get(urlEqualTo("/products"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    public static void stubProductList(String jsonArray) {
        PLATZI.stubFor(get(urlEqualTo("/products")).willReturn(okJson(jsonArray)));
    }
}
