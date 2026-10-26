package edu.eci.tdse.orders.http;

import edu.eci.tdse.orders.application.OrderService;
import edu.eci.tdse.orders.application.SampleOrders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class OrdersHttpAdapterTest {

    private OrdersHttpAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new OrdersHttpAdapter(new OrderService(SampleOrders.newRepository()));
    }

    @Test
    void getExistingOrderReturns200() {
        HttpResponse response = adapter.handle(get("/orders/{id}", Map.of("id", "ORD-002"), Map.of()));

        assertEquals(200, response.statusCode());
        assertEquals("application/json", response.headers().get("Content-Type"));
        assertEquals("{\"id\":\"ORD-002\",\"customerId\":\"CUST-7\",\"product\":\"Keyboard\",\"quantity\":2}",
                response.body());
    }

    @Test
    void getMissingOrderReturns404WithErrorDocument() {
        HttpResponse response = adapter.handle(get("/orders/{id}", Map.of("id", "ORD-999"), Map.of()));

        assertEquals(404, response.statusCode());
        assertEquals("{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}",
                response.body());
    }

    @Test
    void searchByCustomerReturns200WithList() {
        HttpResponse response = adapter.handle(get("/orders", Map.of(), Map.of("customerId", "CUST-42")));

        assertEquals(200, response.statusCode());
        assertEquals("[{\"id\":\"ORD-001\",\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1},"
                + "{\"id\":\"ORD-003\",\"customerId\":\"CUST-42\",\"product\":\"USB-C Hub\",\"quantity\":3}]",
                response.body());
    }

    @Test
    void searchForCustomerWithoutOrdersReturnsEmptyList() {
        HttpResponse response = adapter.handle(get("/orders", Map.of(), Map.of("customerId", "CUST-100")));

        assertEquals(200, response.statusCode());
        assertEquals("[]", response.body());
    }

    @Test
    void searchWithoutCustomerIdReturns400() {
        HttpResponse response = adapter.handle(get("/orders", Map.of(), Map.of()));

        assertEquals(400, response.statusCode());
        assertEquals("{\"error\":\"INVALID_ORDER\",\"message\":\"customerId is required\"}", response.body());
    }

    @Test
    void postValidOrderReturns201WithLocation() {
        HttpResponse response = adapter.handle(post(
                "{\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}"));

        assertEquals(201, response.statusCode());
        assertEquals("/orders/ORD-004", response.headers().get("Location"));
        assertEquals("{\"id\":\"ORD-004\",\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}",
                response.body());
    }

    @Test
    void postInvalidOrderReturns400() {
        HttpResponse response = adapter.handle(post("{\"customerId\":\"CUST-42\",\"product\":\"Mouse\",\"quantity\":0}"));

        assertEquals(400, response.statusCode());
        assertEquals("{\"error\":\"INVALID_ORDER\",\"message\":\"quantity must be greater than 0\"}",
                response.body());
    }

    @Test
    void postMalformedJsonReturns400() {
        HttpResponse response = adapter.handle(post("{not json"));

        assertEquals(400, response.statusCode());
    }

    @Test
    void postWithoutBodyReturns400() {
        assertEquals(400, adapter.handle(post(null)).statusCode());
    }

    @Test
    void deleteExistingOrderReturns204WithoutBody() {
        HttpResponse response = adapter.handle(delete("ORD-001"));

        assertEquals(204, response.statusCode());
        assertNull(response.body());
        assertEquals(404, adapter.handle(get("/orders/{id}", Map.of("id", "ORD-001"), Map.of())).statusCode());
    }

    @Test
    void deleteMissingOrderReturns404() {
        HttpResponse response = adapter.handle(delete("ORD-999"));

        assertEquals(404, response.statusCode());
        assertEquals("{\"error\":\"ORDER_NOT_FOUND\",\"message\":\"Order ORD-999 does not exist\"}",
                response.body());
    }

    @Test
    void unsupportedMethodReturns405() {
        HttpResponse response = adapter.handle(new HttpRequest("PUT", "/orders/{id}", Map.of("id", "ORD-001"), null, null));

        assertEquals(405, response.statusCode());
    }

    @Test
    void unknownRouteReturns404() {
        assertEquals(404, adapter.handle(get("/customers", Map.of(), Map.of())).statusCode());
    }

    @Test
    void unexpectedErrorReturns500WithoutInternalDetails() {
        OrdersHttpAdapter broken = new OrdersHttpAdapter(new OrderService(null));

        HttpResponse response = broken.handle(get("/orders/{id}", Map.of("id", "ORD-001"), Map.of()));

        assertEquals(500, response.statusCode());
        assertEquals("{\"error\":\"INTERNAL_ERROR\",\"message\":\"Unexpected internal error\"}", response.body());
    }

    private static HttpRequest get(String resource, Map<String, String> path, Map<String, String> query) {
        return new HttpRequest("GET", resource, path, query, null);
    }

    private static HttpRequest delete(String id) {
        return new HttpRequest("DELETE", "/orders/{id}", Map.of("id", id), null, null);
    }

    private static HttpRequest post(String body) {
        return new HttpRequest("POST", "/orders", null, null, body);
    }
}
