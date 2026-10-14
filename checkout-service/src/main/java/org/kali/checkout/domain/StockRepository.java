package org.kali.checkout.domain;

/** Porta para o controle de estoque (implementada com Redis). */
public interface StockRepository {

    /**
     * Reserva (decrementa) atomicamente o estoque.
     *
     * @return {@code true} se havia saldo suficiente e a reserva foi feita
     */
    boolean reserve(String productId, int quantity);

    /** Devolve (incrementa) o estoque. */
    void release(String productId, int quantity);

    long available(String productId);

    void setStock(String productId, long quantity);

    /** Define o estoque apenas se o produto ainda não existir. */
    void setStockIfAbsent(String productId, long quantity);

    boolean isCompensated(String orderId);

    void markCompensated(String orderId);
}

