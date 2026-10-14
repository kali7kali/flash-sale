package org.kali.checkout.application;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.kali.checkout.domain.StockRepository;

/** Implementação em memória de {@link StockRepository} para testes unitários. */
public class FakeStockRepository implements StockRepository {

    public final Map<String, Long> stock = new HashMap<>();
    public final Set<String> compensated = new HashSet<>();

    @Override
    public boolean reserve(String productId, int quantity) {
        long current = stock.getOrDefault(productId, 0L);
        if (current < quantity) {
            return false;
        }
        stock.put(productId, current - quantity);
        return true;
    }

    @Override
    public void release(String productId, int quantity) {
        stock.merge(productId, (long) quantity, Long::sum);
    }

    @Override
    public long available(String productId) {
        return stock.getOrDefault(productId, 0L);
    }

    @Override
    public void setStock(String productId, long quantity) {
        stock.put(productId, quantity);
    }

    @Override
    public void setStockIfAbsent(String productId, long quantity) {
        stock.putIfAbsent(productId, quantity);
    }

    @Override
    public boolean isCompensated(String orderId) {
        return compensated.contains(orderId);
    }

    @Override
    public void markCompensated(String orderId) {
        compensated.add(orderId);
    }
}

