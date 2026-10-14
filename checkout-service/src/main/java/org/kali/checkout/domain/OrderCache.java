package org.kali.checkout.domain;

import java.util.Optional;

public interface OrderCache {

    Optional<OrderView> get(String orderId);

    void put(OrderView order);
}

