package org.kali.checkout.infrastructure;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.kali.checkout.domain.OrderCreatedMessage;
import org.kali.checkout.domain.OrderEventPublisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RabbitOrderEventPublisher implements OrderEventPublisher {

    private final MutinyEmitter<String> emitter;
    private final ObjectMapper mapper;

    @Inject
    public RabbitOrderEventPublisher(@Channel("order-created") MutinyEmitter<String> emitter, ObjectMapper mapper) {
        this.emitter = emitter;
        this.mapper = mapper;
    }

    @Override
    public void publishOrderCreated(OrderCreatedMessage message) {
        try {
            emitter.sendAndAwait(mapper.writeValueAsString(message));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar mensagem do pedido", e);
        }
    }
}

