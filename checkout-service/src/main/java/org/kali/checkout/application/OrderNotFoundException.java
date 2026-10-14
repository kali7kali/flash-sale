package org.kali.checkout.application;

public class OrderNotFoundException extends RuntimeException {

    public OrderNotFoundException() {
        super("Pedido não encontrado");
    }
}

