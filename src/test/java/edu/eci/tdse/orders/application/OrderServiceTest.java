package edu.eci.tdse.orders.application;

import edu.eci.tdse.orders.domain.CreateOrderRequest;
import edu.eci.tdse.orders.domain.InvalidOrderException;
import edu.eci.tdse.orders.domain.Order;
import edu.eci.tdse.orders.domain.OrderNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderServiceTest {

    private OrderService service;

    @BeforeEach
    void setUp() {
        service = new OrderService(SampleOrders.newRepository());
    }

    @Test
    void createsValidOrderWithGeneratedId() {
        Order order = service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));

        assertNotNull(order.getId());
        assertEquals("ORD-004", order.getId());
        assertEquals("CUST-42", order.getCustomerId());
        assertEquals("MacBook Air", order.getProduct());
        assertEquals(1, order.getQuantity());
    }

    @Test
    void createdOrderCanBeRetrieved() {
        Order created = service.createOrder(new CreateOrderRequest("CUST-9", "Monitor", 2));

        assertEquals(created, service.getOrder(created.getId()));
    }

    @Test
    void retrievesExistingOrder() {
        Order order = service.getOrder("ORD-002");

        assertEquals(new Order("ORD-002", "CUST-7", "Keyboard", 2), order);
    }

    @Test
    void failsWhenOrderDoesNotExist() {
        OrderNotFoundException error =
                assertThrows(OrderNotFoundException.class, () -> service.getOrder("ORD-999"));

        assertEquals("ORDER_NOT_FOUND", error.getCode());
        assertEquals("Order ORD-999 does not exist", error.getMessage());
    }

    @Test
    void findsOrdersBelongingToCustomer() {
        List<Order> orders = service.getOrdersByCustomer("CUST-42");

        assertEquals(List.of("ORD-001", "ORD-003"), orders.stream().map(Order::getId).toList());
    }

    @Test
    void customerWithoutOrdersGetsEmptyList() {
        assertTrue(service.getOrdersByCustomer("CUST-100").isEmpty());
    }

    @Test
    void rejectsMissingCustomerId() {
        InvalidOrderException error = assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest(" ", "MacBook Air", 1)));

        assertEquals("INVALID_ORDER", error.getCode());
        assertEquals("customerId is required", error.getMessage());
    }

    @Test
    void rejectsMissingProduct() {
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-42", null, 1)));
    }

    @Test
    void rejectsMissingQuantity() {
        assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", null)));
    }

    @Test
    void rejectsNonPositiveQuantity() {
        InvalidOrderException error = assertThrows(InvalidOrderException.class,
                () -> service.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 0)));

        assertEquals("quantity must be greater than 0", error.getMessage());
    }

    @Test
    void rejectsNullRequest() {
        assertThrows(InvalidOrderException.class, () -> service.createOrder(null));
    }

    @Test
    void deletesExistingOrder() {
        service.deleteOrder("ORD-001");

        assertThrows(OrderNotFoundException.class, () -> service.getOrder("ORD-001"));
        assertEquals(List.of("ORD-003"),
                service.getOrdersByCustomer("CUST-42").stream().map(Order::getId).toList());
    }

    @Test
    void deletingMissingOrderFails() {
        OrderNotFoundException error =
                assertThrows(OrderNotFoundException.class, () -> service.deleteOrder("ORD-999"));

        assertEquals("Order ORD-999 does not exist", error.getMessage());
    }

    @Test
    void deletingSameOrderTwiceFailsTheSecondTime() {
        service.deleteOrder("ORD-002");

        assertThrows(OrderNotFoundException.class, () -> service.deleteOrder("ORD-002"));
    }

    @Test
    void deletingWithBlankIdIsInvalid() {
        assertThrows(InvalidOrderException.class, () -> service.deleteOrder(" "));
    }

    @Test
    void rejectsBlankSearchCriteria() {
        assertThrows(InvalidOrderException.class, () -> service.getOrdersByCustomer(""));
        assertThrows(InvalidOrderException.class, () -> service.getOrder(null));
    }
}
