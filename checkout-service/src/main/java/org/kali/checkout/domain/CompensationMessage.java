package org.kali.checkout.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Mensagem consumida da fila {@code order-compensation}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CompensationMessage(
        @JsonProperty("pedido_id") String pedidoId,
        @JsonProperty("produto_id") String produtoId,
        @JsonProperty("quantidade") int quantidade,
        @JsonProperty("motivo") String motivo) {
}

