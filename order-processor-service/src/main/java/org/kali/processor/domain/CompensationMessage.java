package org.kali.processor.domain;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Mensagem publicada na fila {@code order-compensation}. */
public record CompensationMessage(
        @JsonProperty("pedido_id") String pedidoId,
        @JsonProperty("produto_id") String produtoId,
        @JsonProperty("quantidade") int quantidade,
        @JsonProperty("motivo") String motivo) {
}
