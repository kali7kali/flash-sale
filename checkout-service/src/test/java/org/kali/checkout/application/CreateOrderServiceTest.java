package org.kali.checkout.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kali.checkout.domain.OrderCreatedMessage;
import org.kali.checkout.domain.OrderStatus;

class CreateOrderServiceTest {

    private FakeStockRepository stock;
    private List<OrderCreatedMessage> published;
    private boolean publisherFails;
    private CreateOrderService service;

    @BeforeEach
    void setUp() {
        stock = new FakeStockRepository();
        stock.setStock("PROD-001", 5);
        published = new ArrayList<>();
        publisherFails = false;
        service = new CreateOrderService(stock, message -> {
            if (publisherFails) {
                throw new IllegalStateException("broker indisponível");
            }
            published.add(message);
        });
    }

    private CreateOrderCommand command(int qty) {
        return new CreateOrderCommand("PROD-001", qty, "50329291076", "Fernando Augusto");
    }

    @Test
    void reservaEstoquePublicaMensagemERetornaProcessing() {
        CreateOrderResult result = service.create(command(2));

        assertEquals(OrderStatus.PROCESSING, result.status());
        assertEquals(3, stock.available("PROD-001"));
        assertEquals(1, published.size());
        assertEquals(result.pedidoId(), published.get(0).pedidoId());
        assertEquals(2, published.get(0).quantidade());
        assertNotNull(result.dataHoraRequisicao());
        UUID uuid = UUID.fromString(result.pedidoId());
        assertEquals(4, uuid.version());
    }

    @Test
    void estoqueInsuficienteNaoAlteraSaldoNemPublica() {
        assertThrows(InsufficientStockException.class, () -> service.create(command(6)));

        assertEquals(5, stock.available("PROD-001"));
        assertEquals(0, published.size());
    }

    @Test
    void estoqueNuncaFicaNegativo() {
        service.create(command(5));
        assertThrows(InsufficientStockException.class, () -> service.create(command(1)));
        assertEquals(0, stock.available("PROD-001"));
    }

    @Test
    void quantidadeInvalidaEhRejeitada() {
        assertThrows(IllegalArgumentException.class, () -> service.create(command(0)));
    }

    @Test
    void devolveEstoqueSeAPublicacaoFalhar() {
        publisherFails = true;

        assertThrows(IllegalStateException.class, () -> service.create(command(2)));

        assertEquals(5, stock.available("PROD-001"));
    }
}

