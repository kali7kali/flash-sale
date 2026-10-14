package org.kali.checkout.application;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.jboss.logging.Logger;
import org.kali.checkout.domain.OrderCreatedMessage;
import org.kali.checkout.domain.OrderEventPublisher;
import org.kali.checkout.domain.OrderStatus;
import org.kali.checkout.domain.StockRepository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CreateOrderService {

    private static final Logger LOG = Logger.getLogger(CreateOrderService.class);

    private final StockRepository stock;
    private final OrderEventPublisher publisher;

    @Inject
    public CreateOrderService(StockRepository stock, OrderEventPublisher publisher) {
        this.stock = stock;
        this.publisher = publisher;
    }

    public CreateOrderResult create(CreateOrderCommand command) {
        if (command.quantidade() <= 0) {
            throw new IllegalArgumentException("A quantidade deve ser maior que zero");
        }
        if (!stock.reserve(command.produtoId(), command.quantidade())) {
            throw new InsufficientStockException(command.produtoId());
        }

        String orderId = UUID.randomUUID().toString(); // UUID v4
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
        OrderCreatedMessage message = new OrderCreatedMessage(orderId, command.produtoId(), command.quantidade(),
                command.clienteCpf(), command.clienteNome(), now);

        try {
            publisher.publishOrderCreated(message);
        } catch (RuntimeException e) {
            LOG.errorf(e, "Falha ao publicar pedido %s; devolvendo estoque", orderId);
            stock.release(command.produtoId(), command.quantidade());
            throw e;
        }
        LOG.infof("Pedido %s aceito (produto=%s, quantidade=%d)", orderId, command.produtoId(), command.quantidade());
        return new CreateOrderResult(orderId, OrderStatus.PROCESSING, now);
    }
}
