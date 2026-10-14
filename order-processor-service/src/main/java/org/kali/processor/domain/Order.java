package org.kali.processor.domain;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Pedido persistido no banco do order-processor-service. */
@Entity
@Table(name = "orders")
public class Order {

    @Id
    public UUID id;

    @Column(nullable = false)
    public String produtoId;

    @Column(nullable = false)
    public int quantidade;

    @Column(nullable = false)
    public String clienteCpf;

    public String clienteNome;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    public OrderStatus status;

    public String motivo;

    public LocalDateTime dataHoraCriacao;

    public LocalDateTime dataHoraAtualizacao;

    public static Order from(OrderCreatedMessage m, OrderStatus status, String motivo, LocalDateTime now) {
        Order o = new Order();
        o.id = UUID.fromString(m.pedidoId());
        o.produtoId = m.produtoId();
        o.quantidade = m.quantidade();
        o.clienteCpf = m.clienteCpf();
        o.clienteNome = m.clienteNome();
        o.status = status;
        o.motivo = motivo;
        o.dataHoraCriacao = m.dataHoraCriacao();
        o.dataHoraAtualizacao = now;
        return o;
    }
}
