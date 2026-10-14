package org.kali.processor;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.UUID;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

@QuarkusTest
class OrderProcessingFlowTest {

    @Inject
    @Any
    InMemoryConnector connector;

    @BeforeEach
    void setUp() {
        connector.sink("order-compensation").clear();
    }

    private String message(String id, String nome) {
        return """
                {"pedido_id":"%s","produto_id":"PROD-001","quantidade":2,"cliente_cpf":"50329291076",
                 "cliente_nome":"%s","data_hora_criacao":"2026-09-07T20:03:57"}
                """.formatted(id, nome);
    }

    private void awaitOrder(String id, int expectedStatus) {
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(
                () -> given().get("/internal/orders/" + id).statusCode() == expectedStatus);
    }

    @Test
    void fluxoDeSucessoPersisteConfirmed() {
        String id = UUID.randomUUID().toString();

        connector.source("order-created").send(message(id, "Fernando Augusto"));

        awaitOrder(id, 200);
        given().get("/internal/orders/" + id).then().statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .body("produto_id", equalTo("PROD-001"))
                .body("quantidade", equalTo(2));
        assertEquals(0, connector.sink("order-compensation").received().size());
    }

    @Test
    void fluxoDeFalhaGravaCancelledEPublicaCompensacao() {
        String id = UUID.randomUUID().toString();

        connector.source("order-created").send(message(id, "Cliente FALHA"));

        awaitOrder(id, 200);
        given().get("/internal/orders/" + id).then().statusCode(200).body("status", equalTo("CANCELLED"));

        InMemorySink<String> sink = connector.sink("order-compensation");
        Awaitility.await().atMost(Duration.ofSeconds(5)).until(() -> sink.received().size() == 1);
        String payload = sink.received().get(0).getPayload();
        assertTrue(payload.contains(id));
        assertTrue(payload.contains("\"produto_id\":\"PROD-001\""));
        assertTrue(payload.contains("\"quantidade\":2"));
    }

    @Test
    void pedidoInexistenteRetorna404() {
        given().get("/internal/orders/" + UUID.randomUUID()).then().statusCode(404);
    }
}

