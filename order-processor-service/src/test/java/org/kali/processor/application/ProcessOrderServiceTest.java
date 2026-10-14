package org.kali.processor.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kali.processor.domain.CompensationMessage;
import org.kali.processor.domain.Order;
import org.kali.processor.domain.OrderCreatedMessage;
import org.kali.processor.domain.OrderProcessingException;
import org.kali.processor.domain.OrderStatus;
import org.kali.processor.domain.OrderStore;

class ProcessOrderServiceTest {

    private final Map<UUID, Order> db = new HashMap<>();
    private final List<CompensationMessage> compensations = new ArrayList<>();
    private boolean dbFailsOnConfirm;
    private boolean simulatorFails;
    private ProcessOrderService service;

    private final OrderCreatedMessage message = new OrderCreatedMessage("b819cc65-f6f0-478c-8bb7-69ee1c4f6402",
            "PROD-001", 2, "50329291076", "Fernando Augusto", LocalDateTime.now());

    @BeforeEach
    void setUp() {
        dbFailsOnConfirm = false;
        simulatorFails = false;
        db.clear();
        compensations.clear();
        OrderStore store = new OrderStore() {
            @Override
            public void save(Order order) {
                if (dbFailsOnConfirm && order.status == OrderStatus.CONFIRMED) {
                    throw new IllegalStateException("banco indisponível");
                }
                db.put(order.id, order);
            }

            @Override
            public Optional<Order> findById(UUID id) {
                return Optional.ofNullable(db.get(id));
            }
        };
        service = new ProcessOrderService(store, compensations::add, m -> {
            if (simulatorFails) {
                throw new OrderProcessingException("pagamento recusado");
            }
        });
    }

    @Test
    void sucessoPersisteComoConfirmedSemCompensacao() {
        service.process(message);

        assertEquals(OrderStatus.CONFIRMED, db.get(UUID.fromString(message.pedidoId())).status);
        assertTrue(compensations.isEmpty());
    }

    @Test
    void falhaDeRegraGravaCancelledEPublicaCompensacao() {
        simulatorFails = true;

        service.process(message);

        assertEquals(OrderStatus.CANCELLED, db.get(UUID.fromString(message.pedidoId())).status);
        assertEquals(1, compensations.size());
        CompensationMessage c = compensations.get(0);
        assertEquals(message.pedidoId(), c.pedidoId());
        assertEquals("PROD-001", c.produtoId());
        assertEquals(2, c.quantidade());
        assertTrue(c.motivo().startsWith("Falha no processamento do pedido"));
    }

    @Test
    void falhaDePersistenciaCancelaECompensa() {
        dbFailsOnConfirm = true;

        service.process(message);

        assertEquals(OrderStatus.CANCELLED, db.get(UUID.fromString(message.pedidoId())).status);
        assertEquals(1, compensations.size());
    }

    @Test
    void mensagemDuplicadaEhIgnorada() {
        service.process(message);
        simulatorFails = true;
        service.process(message);

        assertEquals(OrderStatus.CONFIRMED, db.get(UUID.fromString(message.pedidoId())).status);
        assertTrue(compensations.isEmpty());
    }
}

