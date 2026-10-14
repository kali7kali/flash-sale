package org.kali.checkout.api;

import org.kali.checkout.domain.StockRepository;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/** Endpoints auxiliares para preparar e inspecionar o estoque (demonstração). */
@Path("/v1/stock/{produtoId}")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StockResource {

    public record StockBody(
            @NotNull @Min(0) @JsonProperty("quantidade") Long quantidade,
            @JsonProperty("produto_id") String produtoId) {
    }

    @Inject
    StockRepository stock;

    @GET
    public StockBody get(@PathParam("produtoId") String produtoId) {
        return new StockBody(stock.available(produtoId), produtoId);
    }

    @PUT
    public StockBody put(@PathParam("produtoId") String produtoId, @Valid StockBody body) {
        stock.setStock(produtoId, body.quantidade());
        return new StockBody(stock.available(produtoId), produtoId);
    }
}
