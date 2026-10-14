package org.kali.checkout.application;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.kali.checkout.domain.OrderCache;
import org.kali.checkout.domain.OrderGateway;
import org.kali.checkout.domain.OrderView;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Consulta de pedido com Cache-Aside e até 3 tentativas no banco. */
@ApplicationScoped
public class GetOrderService {

    static final int MAX_ATTEMPTS = 3;
    private static final Logger LOG = Logger.getLogger(GetOrderService.class);

    private final OrderCache cache;
    private final OrderGateway gateway;
    private final long retryDelayMs;

    @Inject
    public GetOrderService(OrderCache cache, OrderGateway gateway,
            @ConfigProperty(name = "checkout.order-lookup.retry-delay-ms", defaultValue = "300") long retryDelayMs) {
        this.cache = cache;
        this.gateway = gateway;
        this.retryDelayMs = retryDelayMs;
    }

    public OrderView get(String orderId) {
        Optional<OrderView> cached = cache.get(orderId);
        if (cached.isPresent()) {
            return cached.get();
        }

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            Optional<OrderView> found = lookup(orderId);
            if (found.isPresent()) {
                cache.put(found.get());
                return found.get();
            }
            LOG.debugf("Pedido %s não encontrado (tentativa %d/%d)", orderId, attempt, MAX_ATTEMPTS);
            if (attempt < MAX_ATTEMPTS) {
                sleep();
            }
        }
        throw new OrderNotFoundException();
    }

    private Optional<OrderView> lookup(String orderId) {
        try {
            return gateway.findById(orderId);
        } catch (RuntimeException e) {
            LOG.warnf("Erro ao consultar pedido %s: %s", orderId, e.getMessage());
            return Optional.empty();
        }
    }

    private void sleep() {
        if (retryDelayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(retryDelayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

