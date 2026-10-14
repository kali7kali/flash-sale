package org.kali.checkout.api;

import org.kali.checkout.application.CreateOrderCommand;
import org.kali.checkout.application.CreateOrderResult;
import org.kali.checkout.application.CreateOrderService;
import org.kali.checkout.application.GetOrderService;
import org.kali.checkout.domain.OrderView;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/v1/orders")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class OrderResource {

    @Inject
    CreateOrderService createOrderService;

    @Inject
    GetOrderService getOrderService;

    @POST
    public Response create(@Valid CreateOrderRequest request) {
        CreateOrderResult result = createOrderService.create(new CreateOrderCommand(request.produtoId(),
                request.quantidade(), request.clienteCpf(), request.clienteNome()));
        return Response.accepted(result).build();
    }

    @GET
    @Path("/{pedidoId}")
    public OrderView get(@PathParam("pedidoId") String pedidoId) {
        return getOrderService.get(pedidoId);
    }
}
