package org.kali.checkout.infrastructure;

import java.util.Optional;

import org.kali.checkout.domain.StockRepository;

import io.quarkus.redis.datasource.RedisDataSource;
import io.vertx.mutiny.redis.client.Response;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RedisStockRepository implements StockRepository {

    /** Verifica o saldo e decrementa na mesma operação atômica; retorna -1 se insuficiente. */
    private static final String RESERVE_SCRIPT = """
            local stock = tonumber(redis.call('GET', KEYS[1]))
            local qty = tonumber(ARGV[1])
            if stock == nil or stock < qty then
              return -1
            end
            return redis.call('DECRBY', KEYS[1], qty)
            """;

    private final RedisDataSource redis;

    @Inject
    public RedisStockRepository(RedisDataSource redis) {
        this.redis = redis;
    }

    static String stockKey(String productId) {
        return "stock:" + productId;
    }

    @Override
    public boolean reserve(String productId, int quantity) {
        Response response = redis.execute("EVAL", RESERVE_SCRIPT, "1", stockKey(productId),
                String.valueOf(quantity));
        return response != null && response.toLong() >= 0;
    }

    @Override
    public void release(String productId, int quantity) {
        redis.value(Long.class).incrby(stockKey(productId), quantity);
    }

    @Override
    public long available(String productId) {
        return Optional.ofNullable(redis.value(Long.class).get(stockKey(productId))).orElse(0L);
    }

    @Override
    public void setStock(String productId, long quantity) {
        redis.value(Long.class).set(stockKey(productId), quantity);
    }

    @Override
    public void setStockIfAbsent(String productId, long quantity) {
        redis.value(Long.class).setnx(stockKey(productId), quantity);
    }

    @Override
    public boolean isCompensated(String orderId) {
        return redis.key().exists("compensation:" + orderId);
    }

    @Override
    public void markCompensated(String orderId) {
        redis.value(String.class).setex("compensation:" + orderId, 7 * 24 * 3600L, "DONE");
    }
}
