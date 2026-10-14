package org.kali.processor.application;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.kali.processor.domain.CompensationMessage;
import org.kali.processor.domain.CompensationPublisher;
import org.kali.processor.domain.FailureSimulator;
import org.kali.processor.domain.Order;
import org.kali.processor.domain.OrderCreatedMessage;
import org.kali.processor.domain.OrderStatus;
import org.kali.processor.domain.OrderStore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ProcessOrderService {

    private static final Logger LOG = Logger.getLogger(ProcessOrderService.class);
    static final String FAILURE_REASON = "Falha no processamento do pedido";

    private final OrderStore store;
    private final CompensationPublisher compensationPublisher;
    private final FailureSimulator failureSimulator;

    @Inject
    public ProcessOrderService(OrderStore store, CompensationPublisher compensationPublisher,
            FailureSimulator failureSimulator) {
        this.store = store;
        this.compensationPublisher = compensationPublisher;
        this.failureSimulator = failureSimulator;
    }

    public void process(OrderCreatedMessage message) {
        try {
            UUID id = UUID.fromString(message.pedidoId());
            if (store.findById(id).isPresent()) {
                LOG.infof("Pedido %s já processado; ignorando mensagem duplicada", message.pedidoId());
                return;
            }
            failureSimulator.check(message);
            store.save(Order.from(message, OrderStatus.CONFIRMED, null, now()));
            LOG.infof("Pedido %s CONFIRMED", message.pedidoId());
        } catch (RuntimeException e) {
            LOG.warnf("Falha ao processar pedido %s: %s", message.pedidoId(), e.getMessage());
            cancel(message, e);
        }
    }

    /** Passo de falha da SAGA: cancela o pedido e dispara a compensação do estoque. */
    private void cancel(OrderCreatedMessage message, RuntimeException cause) {
        String reason = FAILURE_REASON + (cause.getMessage() != null ? ": " + cause.getMessage() : "");
        try {
            store.save(Order.from(message, OrderStatus.CANCELLED, reason, now()));
        } catch (RuntimeException e) {
            LOG.errorf(e, "Não foi possível gravar o pedido %s como CANCELLED", message.pedidoId());
        }
        compensationPublisher.publish(new CompensationMessage(message.pedidoId(), message.produtoId(),
                message.quantidade(), reason));
        LOG.infof("Pedido %s CANCELLED; compensação publicada", message.pedidoId());
    }

    private static LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }
}

