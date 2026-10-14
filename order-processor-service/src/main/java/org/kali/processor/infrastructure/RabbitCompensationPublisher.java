package org.kali.processor.infrastructure;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.jboss.logging.Logger;
import org.kali.processor.domain.CompensationMessage;
import org.kali.processor.domain.CompensationPublisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Publica em {@code order-compensation} de forma não bloqueante.
 * <p>
 * Não se espera o ack dentro do consumer de {@code order-created} (isso travava a thread do
 * consumer). A confirmação do broker é tratada por callbacks: ack em log e nack como erro.
 */
@ApplicationScoped
public class RabbitCompensationPublisher implements CompensationPublisher {

    private static final Logger LOG = Logger.getLogger(RabbitCompensationPublisher.class);

    private final Emitter<String> emitter;
    private final ObjectMapper mapper;

    @Inject
    public RabbitCompensationPublisher(@Channel("order-compensation") Emitter<String> emitter,
            ObjectMapper mapper) {
        this.emitter = emitter;
        this.mapper = mapper;
    }

    @Override
    public void publish(CompensationMessage message) {
        String payload;
        try {
            payload = mapper.writeValueAsString(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Falha ao serializar mensagem de compensação", e);
        }
        String pedidoId = message.pedidoId();
        emitter.send(Message.of(payload,
                () -> {
                    LOG.debugf("Compensação do pedido %s confirmada pelo broker", pedidoId);
                    return CompletableFuture.<Void>completedFuture(null);
                },
                (Throwable reason) -> {
                    LOG.errorf(reason, "Broker recusou a compensação do pedido %s", pedidoId);
                    return (CompletionStage<Void>) CompletableFuture.<Void>completedFuture(null);
                }));
    }
}

