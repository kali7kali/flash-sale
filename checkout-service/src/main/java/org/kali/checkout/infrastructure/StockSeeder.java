package org.kali.checkout.infrastructure;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.kali.checkout.domain.StockRepository;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

/** Cria um estoque inicial de demonstração no Redis (caso ainda não exista). */
@ApplicationScoped
public class StockSeeder {

    private static final Logger LOG = Logger.getLogger(StockSeeder.class);

    @Inject
    StockRepository stock;

    @ConfigProperty(name = "checkout.seed.enabled", defaultValue = "true")
    boolean enabled;

    @ConfigProperty(name = "checkout.seed.product-id", defaultValue = "PROD-001")
    String productId;

    @ConfigProperty(name = "checkout.seed.quantity", defaultValue = "100")
    long quantity;

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            return;
        }
        try {
            stock.setStockIfAbsent(productId, quantity);
            LOG.infof("Estoque inicial: %s = %d", productId, stock.available(productId));
        } catch (RuntimeException e) {
            LOG.warnf("Não foi possível criar o estoque inicial (Redis disponível?): %s", e.getMessage());
        }
    }
}
