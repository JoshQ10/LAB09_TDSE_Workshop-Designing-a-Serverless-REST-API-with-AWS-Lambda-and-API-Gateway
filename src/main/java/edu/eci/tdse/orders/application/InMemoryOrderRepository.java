package edu.eci.tdse.orders.application;

import edu.eci.tdse.orders.domain.Order;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Repositorio en memoria. En Lambda vive solo mientras el entorno de ejecución esté "caliente":
 * NO es almacenamiento persistente y cada instancia de Lambda tiene su propia copia.
 */
public class InMemoryOrderRepository implements OrderRepository {

    private final Map<String, Order> orders = new ConcurrentHashMap<>();
    private final AtomicInteger sequence = new AtomicInteger();

    @Override
    public String nextId() {
        String id;
        do {
            id = String.format("ORD-%03d", sequence.incrementAndGet());
        } while (orders.containsKey(id));
        return id;
    }

    @Override
    public Order save(Order order) {
        orders.put(order.getId(), order);
        return order;
    }

    @Override
    public Optional<Order> findById(String id) {
        return Optional.ofNullable(orders.get(id));
    }

    @Override
    public List<Order> findByCustomerId(String customerId) {
        return orders.values().stream()
                .filter(order -> order.getCustomerId().equals(customerId))
                .sorted(Comparator.comparing(Order::getId))
                .toList();
    }

    @Override
    public boolean deleteById(String id) {
        return orders.remove(id) != null;
    }
}
