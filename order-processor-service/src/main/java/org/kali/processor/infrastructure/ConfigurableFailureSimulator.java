package org.kali.processor.infrastructure;

import java.util.concurrent.ThreadLocalRandom;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.kali.processor.domain.FailureSimulator;
import org.kali.processor.domain.OrderCreatedMessage;
import org.kali.processor.domain.OrderProcessingException;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * Falha determinística quando o nome do cliente contém a palavra-chave configurada (padrão "FALHA") e,
 * opcionalmente, de forma aleatória conforme {@code processor.failure-rate}.
 */
@ApplicationScoped
public class ConfigurableFailureSimulator implements FailureSimulator {

    @ConfigProperty(name = "processor.failure-keyword", defaultValue = "FALHA")
    String keyword;

    @ConfigProperty(name = "processor.failure-rate", defaultValue = "0")
    double failureRate;

    @Override
    public void check(OrderCreatedMessage message) {
        String nome = message.clienteNome();
        if (nome != null && !keyword.isBlank() && nome.toUpperCase().contains(keyword.toUpperCase())) {
            throw new OrderProcessingException("pagamento recusado (falha simulada)");
        }
        if (failureRate > 0 && ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new OrderProcessingException("falha aleatória simulada");
        }
    }
}

