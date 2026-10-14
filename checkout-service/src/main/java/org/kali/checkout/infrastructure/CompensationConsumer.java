package org.kali.checkout.infrastructure;

import java.nio.charset.StandardCharsets;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import org.kali.checkout.application.CompensationService;
import org.kali.checkout.domain.CompensationMessage;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/** Consumer da fila {@code order-compensation}. */
@ApplicationScoped
public class CompensationConsumer {

    private static final Logger LOG = Logger.getLogger(CompensationConsumer.class);

    private final CompensationService service;
    private final ObjectMapper mapper;

    @Inject
    public CompensationConsumer(CompensationService service, ObjectMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Incoming("order-compensation")
    @Blocking
    public void consume(Object payload) throws JsonProcessingException {
        String json = payload instanceof byte[] bytes ? new String(bytes, StandardCharsets.UTF_8)
                : payload.toString();
        LOG.debugf("Mensagem de compensação recebida: %s", json);
        service.compensate(mapper.readValue(json, CompensationMessage.class));
    }
}
