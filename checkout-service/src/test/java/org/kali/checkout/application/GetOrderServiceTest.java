package org.kali.checkout.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kali.checkout.domain.OrderCache;
import org.kali.checkout.domain.OrderGateway;
import org.kali.checkout.domain.OrderStatus;
import org.kali.checkout.domain.OrderView;

class GetOrderServiceTest {

    private final Map<String, OrderView> cacheStore = new HashMap<>();
    private final Map<String, OrderView> database = new HashMap<>();
    private final AtomicInteger dbCalls = new AtomicInteger();
    private GetOrderService service;

    private final OrderView order = new OrderView("id-1", "PROD-001", 2, "50329291076", "Fernando Augusto",
            OrderStatus.CONFIRMED, LocalDateTime.now());

    @BeforeEach
    void setUp() {
        OrderCache cache = new OrderCache() {
            @Override
            public Optional<OrderView> get(String orderId) {
                return Optional.ofNullable(cacheStore.get(orderId));
            }

            @Override
            public void put(OrderView o) {
                cacheStore.put(o.pedidoId(), o);
            }
        };
        OrderGateway gateway = id -> {
            dbCalls.incrementAndGet();
            return Optional.ofNullable(database.get(id));
        };
        service = new GetOrderService(cache, gateway, 0);
    }

    @Test
    void retornaDoCacheSemConsultarBanco() {
        cacheStore.put("id-1", order);

        assertSame(order, service.get("id-1"));
        assertEquals(0, dbCalls.get());
    }

    @Test
    void cacheMissConsultaBancoEPopulaCache() {
        database.put("id-1", order);

        assertSame(order, service.get("id-1"));
        assertEquals(1, dbCalls.get());
        assertEquals(order, cacheStore.get("id-1"));
    }

    @Test
    void tentaTresVezesAntesDeRetornarNaoEncontrado() {
        assertThrows(OrderNotFoundException.class, () -> service.get("inexistente"));

        assertEquals(3, dbCalls.get());
    }

    @Test
    void encontraPedidoQueApareceNaSegundaTentativa() {
        OrderGateway flaky = id -> {
            if (dbCalls.incrementAndGet() < 2) {
                return Optional.empty();
            }
            return Optional.of(order);
        };
        OrderCache cache = new OrderCache() {
            @Override
            public Optional<OrderView> get(String orderId) {
                return Optional.empty();
            }

            @Override
            public void put(OrderView o) {
                cacheStore.put(o.pedidoId(), o);
            }
        };

        assertEquals(order, new GetOrderService(cache, flaky, 0).get("id-1"));
        assertEquals(2, dbCalls.get());
    }

    @Test
    void erroNoBancoContaComoTentativaFalha() {
        OrderGateway broken = id -> {
            dbCalls.incrementAndGet();
            throw new IllegalStateException("fora do ar");
        };
        OrderCache emptyCache = new OrderCache() {
            @Override
            public Optional<OrderView> get(String orderId) {
                return Optional.empty();
            }

            @Override
            public void put(OrderView o) {
            }
        };
        GetOrderService s = new GetOrderService(emptyCache, broken, 0);

        assertThrows(OrderNotFoundException.class, () -> s.get("x"));
        assertEquals(3, dbCalls.get());
    }
}

