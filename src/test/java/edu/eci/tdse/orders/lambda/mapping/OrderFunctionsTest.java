package edu.eci.tdse.orders.lambda.mapping;

import edu.eci.tdse.orders.application.OrderService;
import edu.eci.tdse.orders.application.SampleOrders;
import edu.eci.tdse.orders.domain.CreateOrderRequest;
import edu.eci.tdse.orders.domain.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderFunctionsTest {

    private OrderFunctions functions;

    @BeforeEach
    void setUp() {
        functions = new OrderFunctions(new OrderService(SampleOrders.newRepository()));
    }

    @Test
    void createsOrder() {
        Order order = functions.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", 1));

        assertEquals("ORD-004", order.getId());
    }

    @Test
    void getsOrderById() {
        assertEquals("Keyboard", functions.getOrder(new OrderIdInput("ORD-002")).getProduct());
    }

    @Test
    void findsOrdersByCustomer() {
        List<Order> orders = functions.getOrdersByCustomer(new CustomerQueryInput("CUST-42"));

        assertEquals(2, orders.size());
    }

    @Test
    void missingOrderMessageStartsWithErrorCodeForApiGatewayRegex() {
        OrderFunctionException error = assertThrows(OrderFunctionException.class,
                () -> functions.getOrder(new OrderIdInput("ORD-999")));

        assertEquals("ORDER_NOT_FOUND: Order ORD-999 does not exist", error.getMessage());
    }

    @Test
    void deletesOrder() {
        functions.deleteOrder(new OrderIdInput("ORD-001"));

        assertThrows(OrderFunctionException.class, () -> functions.getOrder(new OrderIdInput("ORD-001")));
    }

    @Test
    void deletingMissingOrderMessageStartsWithErrorCode() {
        OrderFunctionException error = assertThrows(OrderFunctionException.class,
                () -> functions.deleteOrder(new OrderIdInput("ORD-999")));

        assertEquals("ORDER_NOT_FOUND: Order ORD-999 does not exist", error.getMessage());
    }

    @Test
    void invalidInputMessageStartsWithErrorCode() {
        OrderFunctionException error = assertThrows(OrderFunctionException.class,
                () -> functions.createOrder(new CreateOrderRequest("CUST-42", "MacBook Air", -1)));

        assertEquals("INVALID_ORDER: quantity must be greater than 0", error.getMessage());
    }

    @Test
    void unexpectedErrorsAreReportedAsInternalError() {
        OrderFunctions broken = new OrderFunctions(new OrderService(null));

        OrderFunctionException error = assertThrows(OrderFunctionException.class,
                () -> broken.getOrder(new OrderIdInput("ORD-001")));

        assertEquals("INTERNAL_ERROR: Unexpected internal error", error.getMessage());
    }
}
