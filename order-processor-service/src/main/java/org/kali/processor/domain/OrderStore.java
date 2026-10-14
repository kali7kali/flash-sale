package org.kali.processor.domain;

import java.util.Optional;
import java.util.UUID;

public interface OrderStore {

    /** Insere ou atualiza o pedido. */
    void save(Order order);

    Optional<Order> findById(UUID id);
}

