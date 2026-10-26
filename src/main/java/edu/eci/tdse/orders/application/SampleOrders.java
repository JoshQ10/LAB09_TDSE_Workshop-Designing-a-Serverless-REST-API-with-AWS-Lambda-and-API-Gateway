package edu.eci.tdse.orders.application;

import edu.eci.tdse.orders.domain.Order;

/**
 * Datos predefinidos para que la API tenga información consultable aunque cada entorno de
 * Lambda arranque con la memoria vacía.
 */
public final class SampleOrders {

    private SampleOrders() {
    }

    public static OrderRepository newRepository() {
        InMemoryOrderRepository repository = new InMemoryOrderRepository();
        repository.save(new Order("ORD-001", "CUST-42", "MacBook Air", 1));
        repository.save(new Order("ORD-002", "CUST-7", "Keyboard", 2));
        repository.save(new Order("ORD-003", "CUST-42", "USB-C Hub", 3));
        return repository;
    }
}
