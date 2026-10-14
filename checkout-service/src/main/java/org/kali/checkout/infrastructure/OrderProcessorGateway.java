package org.kali.checkout.infrastructure;

import java.util.Optional;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.kali.checkout.domain.OrderGateway;
import org.kali.checkout.domain.OrderView;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

/** Consulta pedidos no order-processor-service (dono do banco de pedidos). */
@ApplicationScoped
public class OrderProcessorGateway implements OrderGateway {

    private final OrderProcessorClient client;

    @Inject
    public OrderProcessorGateway(@RestClient OrderProcessorClient client) {
        this.client = client;
    }

    @Override
    public Optional<OrderView> findById(String orderId) {
        try {
            return Optional.ofNullable(client.findById(orderId));
        } catch (WebApplicationException e) {
            if (e.getResponse().getStatus() == 404) {
                return Optional.empty();
            }
            throw e;
        }
    }
}
