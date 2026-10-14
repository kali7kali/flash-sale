package org.kali.processor.infrastructure;

import java.util.Optional;
import java.util.UUID;

import org.kali.processor.domain.Order;
import org.kali.processor.domain.OrderStore;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

@ApplicationScoped
public class JpaOrderStore implements OrderStore {

    @Inject
    EntityManager em;

    @Override
    @Transactional
    public void save(Order order) {
        em.merge(order);
    }

    @Override
    @Transactional
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(em.find(Order.class, id));
    }
}

