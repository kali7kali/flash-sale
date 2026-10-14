package org.kali.checkout.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.kali.checkout.domain.CompensationMessage;

class CompensationServiceTest {

    @Test
    void devolveEstoqueReservado() {
        FakeStockRepository stock = new FakeStockRepository();
        stock.setStock("PROD-001", 3);
        CompensationService service = new CompensationService(stock);

        service.compensate(new CompensationMessage("p1", "PROD-001", 2, "Falha no processamento do pedido"));

        assertEquals(5, stock.available("PROD-001"));
        assertTrue(stock.isCompensated("p1"));
    }

    @Test
    void mensagemDuplicadaNaoDevolveEstoqueDuasVezes() {
        FakeStockRepository stock = new FakeStockRepository();
        stock.setStock("PROD-001", 3);
        CompensationService service = new CompensationService(stock);
        CompensationMessage message = new CompensationMessage("p1", "PROD-001", 2, "falha");

        service.compensate(message);
        service.compensate(message);

        assertEquals(5, stock.available("PROD-001"));
    }
}

