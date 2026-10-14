package org.kali.processor.domain;

/** Regra de simulação de falhas no processamento (ex.: pagamento recusado). */
public interface FailureSimulator {

    /** @throws OrderProcessingException se o pedido deve falhar */
    void check(OrderCreatedMessage message);
}
