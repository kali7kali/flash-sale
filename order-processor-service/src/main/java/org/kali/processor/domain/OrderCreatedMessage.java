package org.kali.processor.domain;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Mensagem consumida da fila {@code order-created}. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderCreatedMessage(
        @JsonProperty("pedido_id") String pedidoId,
        @JsonProperty("produto_id") String produtoId,
        @JsonProperty("quantidade") int quantidade,
        @JsonProperty("cliente_cpf") String clienteCpf,
        @JsonProperty("cliente_nome") String clienteNome,
        @JsonProperty("data_hora_criacao") LocalDateTime dataHoraCriacao) {
}

