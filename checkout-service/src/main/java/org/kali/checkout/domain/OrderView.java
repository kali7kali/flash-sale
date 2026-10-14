package org.kali.checkout.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Visão de leitura do pedido (retornada em GET /v1/orders/{id}). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderView(
        @JsonProperty("pedido_id") String pedidoId,
        @JsonProperty("produto_id") String produtoId,
        @JsonProperty("quantidade") int quantidade,
        @JsonProperty("cliente_cpf") String clienteCpf,
        @JsonProperty("cliente_nome") String clienteNome,
        @JsonProperty("status") OrderStatus status,
        @JsonProperty("data_hora_atualizacao") LocalDateTime dataHoraAtualizacao) {
}
