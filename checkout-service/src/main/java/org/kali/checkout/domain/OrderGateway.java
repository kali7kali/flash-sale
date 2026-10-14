package org.kali.checkout.domain;

import java.util.Optional;

/** Porta de consulta ao "banco de dados" de pedidos (pertence ao order-processor-service). */
public interface OrderGateway {

    Optional<OrderView> findById(String orderId);
}
