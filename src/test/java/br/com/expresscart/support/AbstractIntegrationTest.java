package br.com.expresscart.support;

import br.com.expresscart.repository.CartRepository;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MongoDBContainer;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

/**
 * Base dos testes de integração: MongoDB e Redis reais (Testcontainers) e a API da Platzi simulada
 * pelo WireMock. Os containers sobem uma vez por JVM e são compartilhados (o Ryuk os remove no fim).
 * <p>
 * A senha do Redis NÃO é sobrescrita: o teste roda com {@code spring.data.redis.password: password}
 * do application.yml contra um Redis sem senha, documentando que essa combinação funciona (B6).
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    // Mesmas imagens do docker-compose.yml
    static final MongoDBContainer MONGO = new MongoDBContainer("mongo:4");
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:latest").withExposedPorts(6379);
    protected static final WireMockServer PLATZI = new WireMockServer(options().dynamicPort());

    static {
        MONGO.start();
        REDIS.start();
        PLATZI.start();
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.host", MONGO::getHost);
        registry.add("spring.data.mongodb.port", () -> MONGO.getMappedPort(27017));
        registry.add("spring.data.mongodb.database", () -> "express-cart-test");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("expresscart.client.platzi", PLATZI::baseUrl);
    }

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected CartRepository cartRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void resetState() {
        cartRepository.deleteAll();
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
        PLATZI.resetAll();
    }
}
