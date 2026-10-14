package org.kali.processor.api;

import java.time.LocalDateTime;
import java.util.UUID;

import org.kali.processor.domain.Order;
import org.kali.processor.domain.OrderStatus;
import org.kali.processor.domain.OrderStore;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/** Endpoint interno usado pelo checkout-service para consultar o banco de pedidos. */
@Path("/internal/orders")
@Produces(MediaType.APPLICATION_JSON)
public class OrderQueryResource {

    public record OrderResponse(
            @JsonProperty("pedido_id") String pedidoId,
            @JsonProperty("produto_id") String produtoId,
            @JsonProperty("quantidade") int quantidade,
            @JsonProperty("cliente_cpf") String clienteCpf,
            @JsonProperty("cliente_nome") String clienteNome,
            @JsonProperty("status") OrderStatus status,
            @JsonProperty("data_hora_atualizacao") LocalDateTime dataHoraAtualizacao) {

        static OrderResponse from(Order o) {
            return new OrderResponse(o.id.toString(), o.produtoId, o.quantidade, o.clienteCpf, o.clienteNome,
                    o.status, o.dataHoraAtualizacao);
        }
    }

    public record ErrorResponse(@JsonProperty("mensagem") String mensagem) {
    }

    @Inject
    OrderStore store;

    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") String id) {
        try {
            return store.findById(UUID.fromString(id))
                    .map(o -> Response.ok(OrderResponse.from(o)).build())
                    .orElseGet(OrderQueryResource::notFound);
        } catch (IllegalArgumentException e) {
            return notFound();
        }
    }

    private static Response notFound() {
        return Response.status(Response.Status.NOT_FOUND).entity(new ErrorResponse("Pedido não encontrado"))
                .build();
    }
}
