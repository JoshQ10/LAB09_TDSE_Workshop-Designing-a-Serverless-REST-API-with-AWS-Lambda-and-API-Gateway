package edu.eci.tdse.orders.application;

import edu.eci.tdse.orders.domain.CreateOrderRequest;
import edu.eci.tdse.orders.domain.InvalidOrderException;
import edu.eci.tdse.orders.domain.Order;
import edu.eci.tdse.orders.domain.OrderNotFoundException;

import java.util.List;

/**
 * Lógica de aplicación del API de pedidos. Java puro: no depende de HTTP, API Gateway,
 * Spring Boot ni de un servidor web.
 */
public class OrderService {

    private final OrderRepository repository;

    public OrderService(OrderRepository repository) {
        this.repository = repository;
    }

    public Order createOrder(CreateOrderRequest request) {
        if (request == null) {
            throw new InvalidOrderException("Order data is required");
        }
        requireText(request.getCustomerId(), "customerId");
        requireText(request.getProduct(), "product");
        if (request.getQuantity() == null) {
            throw new InvalidOrderException("quantity is required");
        }
        if (request.getQuantity() < 1) {
            throw new InvalidOrderException("quantity must be greater than 0");
        }
        Order order = new Order(repository.nextId(), request.getCustomerId().trim(),
                request.getProduct().trim(), request.getQuantity());
        return repository.save(order);
    }

    public Order getOrder(String id) {
        requireText(id, "id");
        return repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
    }

    public List<Order> getOrdersByCustomer(String customerId) {
        requireText(customerId, "customerId");
        return repository.findByCustomerId(customerId);
    }

    public void deleteOrder(String id) {
        requireText(id, "id");
        if (!repository.deleteById(id)) {
            throw new OrderNotFoundException(id);
        }
    }

    private static void requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new InvalidOrderException(field + " is required");
        }
    }
}
