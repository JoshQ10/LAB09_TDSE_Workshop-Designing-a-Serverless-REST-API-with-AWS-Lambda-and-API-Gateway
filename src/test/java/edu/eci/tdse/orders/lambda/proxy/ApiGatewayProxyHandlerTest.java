package edu.eci.tdse.orders.lambda.proxy;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import edu.eci.tdse.orders.application.OrderService;
import edu.eci.tdse.orders.application.SampleOrders;
import edu.eci.tdse.orders.http.HttpRequest;
import edu.eci.tdse.orders.http.OrdersHttpAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiGatewayProxyHandlerTest {

    private ApiGatewayProxyHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ApiGatewayProxyHandler(new OrdersHttpAdapter(new OrderService(SampleOrders.newRepository())));
    }

    @Test
    void translatesProxyEventIntoHttpResponse() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withResource("/orders/{id}")
                .withPath("/orders/ORD-001")
                .withPathParameters(Map.of("id", "ORD-001"));

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(200, response.getStatusCode());
        assertEquals("application/json", response.getHeaders().get("Content-Type"));
        assertEquals("{\"id\":\"ORD-001\",\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}",
                response.getBody());
    }

    @Test
    void createsOrderFromProxyEventBody() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withResource("/orders")
                .withBody("{\"customerId\":\"CUST-42\",\"product\":\"MacBook Air\",\"quantity\":1}");

        APIGatewayProxyResponseEvent response = handler.handleRequest(event, null);

        assertEquals(201, response.getStatusCode());
        assertEquals("/orders/ORD-004", response.getHeaders().get("Location"));
    }

    @Test
    void decodesBase64Body() {
        String json = "{\"customerId\":\"CUST-1\"}";
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("POST")
                .withResource("/orders")
                .withIsBase64Encoded(true)
                .withBody(Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));

        HttpRequest request = ApiGatewayProxyHandler.toHttpRequest(event);

        assertEquals(json, request.body());
    }

    @Test
    void handlesEventWithoutParameters() {
        APIGatewayProxyRequestEvent event = new APIGatewayProxyRequestEvent()
                .withHttpMethod("GET")
                .withResource("/orders");

        assertEquals(400, handler.handleRequest(event, null).getStatusCode());
    }
}
