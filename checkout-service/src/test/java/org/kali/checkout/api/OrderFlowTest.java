package org.kali.checkout.api;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.eclipse.microprofile.reactive.messaging.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kali.checkout.domain.OrderCache;
import org.kali.checkout.domain.OrderGateway;
import org.kali.checkout.domain.OrderStatus;
import org.kali.checkout.domain.OrderView;
import org.kali.checkout.domain.StockRepository;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import io.smallrye.reactive.messaging.memory.InMemoryConnector;
import io.smallrye.reactive.messaging.memory.InMemorySink;
import jakarta.enterprise.inject.Any;
import jakarta.inject.Inject;

@QuarkusTest
class OrderFlowTest {

    @InjectMock
    StockRepository stock;

    @InjectMock
    OrderCache cache;

    @InjectMock
    OrderGateway gateway;

    @Inject
    @Any
    InMemoryConnector connector;

    @BeforeEach
    void setUp() {
        connector.sink("order-created").clear();
    }

    private static final String BODY = """
            {"produto_id":"PROD-001","quantidade":2,"cliente_cpf":"50329291076","cliente_nome":"Fernando Augusto"}
            """;

    @Test
    void criaPedidoRetorna202EPublicaNaFila() {
        when(stock.reserve("PROD-001", 2)).thenReturn(true);

        given().contentType(ContentType.JSON).body(BODY)
                .when().post("/v1/orders")
                .then().statusCode(202)
                .body("pedido_id", notNullValue())
                .body("status", equalTo("PROCESSING"))
                .body("data_hora_requisicao", notNullValue());

        InMemorySink<String> sink = connector.sink("order-created");
        assertEquals(1, sink.received().size());
        Message<String> sent = sink.received().get(0);
        org.junit.jupiter.api.Assertions.assertTrue(sent.getPayload().contains("\"produto_id\":\"PROD-001\""));
    }

    @Test
    void estoqueInsuficienteRetorna409() {
        when(stock.reserve(anyString(), anyInt())).thenReturn(false);

        given().contentType(ContentType.JSON).body(BODY)
                .when().post("/v1/orders")
                .then().statusCode(409)
                .body("mensagem", equalTo("Estoque insuficiente para o produto PROD-001"));

        assertEquals(0, connector.sink("order-created").received().size());
    }

    @Test
    void validacaoRetorna400() {
        given().contentType(ContentType.JSON)
                .body("{\"produto_id\":\"\",\"quantidade\":0,\"cliente_cpf\":\"\"}")
                .when().post("/v1/orders")
                .then().statusCode(400);

        verify(stock, never()).reserve(anyString(), anyInt());
    }

    @Test
    void consultaRetornaPedidoDoBancoQuandoNaoEstaNoCache() {
        OrderView view = new OrderView("b819cc65-f6f0-478c-8bb7-69ee1c4f6402", "PROD-001", 2, "50329291076",
                "Fernando Augusto", OrderStatus.CONFIRMED, LocalDateTime.of(2026, 9, 7, 20, 4, 10));
        when(cache.get(view.pedidoId())).thenReturn(Optional.empty());
        when(gateway.findById(view.pedidoId())).thenReturn(Optional.of(view));

        given().when().get("/v1/orders/" + view.pedidoId())
                .then().statusCode(200)
                .body("status", equalTo("CONFIRMED"))
                .body("data_hora_atualizacao", equalTo("2026-09-07T20:04:10"));

        verify(cache).put(view);
    }

    @Test
    void consultaInexistenteRetorna404AposTresTentativas() {
        when(cache.get("nao-existe")).thenReturn(Optional.empty());
        when(gateway.findById("nao-existe")).thenReturn(Optional.empty());

        given().when().get("/v1/orders/nao-existe")
                .then().statusCode(404)
                .body("mensagem", equalTo("Pedido não encontrado"));

        verify(gateway, org.mockito.Mockito.times(3)).findById("nao-existe");
    }

    @Test
    void mensagemDeCompensacaoDevolveEstoque() {
        connector.source("order-compensation").send("""
                {"pedido_id":"b819cc65-f6f0-478c-8bb7-69ee1c4f6402","produto_id":"PROD-001","quantidade":2,"motivo":"Falha no processamento do pedido"}
                """);

        verify(stock, timeout(3000)).release("PROD-001", 2);
    }
}

