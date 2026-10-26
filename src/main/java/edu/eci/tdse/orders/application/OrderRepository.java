package edu.eci.tdse.orders.application;

import edu.eci.tdse.orders.domain.Order;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de persistencia. La única implementación del taller es en memoria, pero el servicio
 * no depende de ese detalle (podría reemplazarse por DynamoDB sin tocar {@link OrderService}).
 */
public interface OrderRepository {

    String nextId();

    Order save(Order order);

    Optional<Order> findById(String id);

    List<Order> findByCustomerId(String customerId);

    /** @return {@code true} si el pedido existía y se eliminó. */
    boolean deleteById(String id);
}
