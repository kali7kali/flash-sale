package org.kali.processor.infrastructure;

import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import org.kali.processor.application.ProcessOrderService;
import org.kali.processor.domain.OrderCreatedMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Consumer da fila {@code order-created}. */
@ApplicationScoped
public class OrderCreatedConsumer {

    private static final Logger LOG = Logger.getLogger(OrderCreatedConsumer.class);

    private final ProcessOrderService service;
    private final ObjectMapper mapper;

    @Inject
    public OrderCreatedConsumer(ProcessOrderService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Incoming("order-created")
    @Blocking
    public void consume(Object payload) throws JsonProcessingException {
        String json = payload instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8)
                : payload.toString();
        LOG.debugf("Pedido recebido: %s", json);
        service.process(mapper.readValue(json, OrderCreatedMessage.class));
    }
}
