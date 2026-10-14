package org.kali.checkout.application;

import org.jboss.logging.Logger;
import org.kali.checkout.domain.CompensationMessage;
import org.kali.checkout.domain.StockRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Passo de compensação da SAGA: devolve ao Redis o estoque reservado. */
@ApplicationScoped
public class CompensationService {

    private static final Logger LOG = Logger.getLogger(CompensationService.class);

    private final StockRepository stock;

    @Inject
    public CompensationService(StockRepository stock) {
        this.stock = stock;
    }

    public void compensate(CompensationMessage message) {
        if (message.pedidoId() != null && stock.isCompensated(message.pedidoId())) {
            LOG.infof("Compensação do pedido %s já realizada; ignorando duplicidade", message.pedidoId());
            return;
        }
        stock.release(message.produtoId(), message.quantidade());
        if (message.pedidoId() != null) {
            stock.markCompensated(message.pedidoId());
        }
        LOG.infof("Compensação concluída: pedido=%s produto=%s quantidade=%d motivo=%s", message.pedidoId(),
                message.produtoId(), message.quantidade(), message.motivo());
    }
}
