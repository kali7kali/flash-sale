package org.kali.checkout.infrastructure;

import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kali.checkout.domain.OrderCache;
import org.kali.checkout.domain.OrderView;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.redis.datasource.RedisDataSource;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RedisOrderCache implements OrderCache {

    private final RedisDataSource redis;
    private final ObjectMapper mapper;
    private final long ttlSeconds;

    @Inject
    public RedisOrderCache(RedisDataSource redis, ObjectMapper mapper,
            @ConfigProperty(name = "checkout.cache.ttl-seconds", defaultValue = "300") long ttlSeconds) {
        this.redis = redis;
        this.mapper = mapper;
        this.ttlSeconds = ttlSeconds;
    }

    private static String key(String orderId) {
        return "order:" + orderId;
    }

    @Override
    public Optional<OrderView> get(String orderId) {
        String json = redis.value(String.class).get(key(orderId));
        if (json == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(mapper.readValue(json, OrderView.class));
        } catch (JsonProcessingException e) {
            return Optional.empty();
        }
    }

    @Override
    public void put(OrderView order) {
        try {
            redis.value(String.class).setex(key(order.pedidoId()), ttlSeconds, mapper.writeValueAsString(order));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar pedido para o cache", e);
        }
    }
}
