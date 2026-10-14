package org.kali.checkout.domain;

public interface OrderEventPublisher {

    void publishOrderCreated(OrderCreatedMessage message);
}

