package org.kali.checkout.application;

public class InsufficientStockException extends RuntimeException {

    public InsufficientStockException(String productId) {
        super("Estoque insuficiente para o produto " + productId);
    }
}
