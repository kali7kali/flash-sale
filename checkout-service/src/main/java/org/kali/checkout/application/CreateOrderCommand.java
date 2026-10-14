package org.kali.checkout.application;

public record CreateOrderCommand(String produtoId, int quantidade, String clienteCpf, String clienteNome) {
}

