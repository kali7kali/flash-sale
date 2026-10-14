package org.kali.checkout.application;

import java.time.LocalDateTime;

import org.kali.checkout.domain.OrderStatus;

import com.fasterxml.jackson.annotation.JsonProperty;

public record CreateOrderResult(
        @JsonProperty("pedido_id") String pedidoId,
        @JsonProperty("status") OrderStatus status,
        @JsonProperty("data_hora_requisicao") LocalDateTime dataHoraRequisicao) {
}

