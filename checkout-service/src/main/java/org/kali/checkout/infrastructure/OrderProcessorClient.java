package org.kali.checkout.infrastructure;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.kali.checkout.domain.OrderView;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(configKey = "order-processor")
@Path("/internal/orders")
public interface OrderProcessorClient {

    @GET
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    OrderView findById(@PathParam("id") String id);
}

