package edu.eci.tdse.orders.lambda.mapping;

/**
 * Entrada {@code {"id": "ORD-001"}} que construye la plantilla de mapeo de API Gateway
 * a partir del path {@code /orders/{id}}.
 */
public class OrderIdInput {

    private String id;

    public OrderIdInput() {
    }

    public OrderIdInput(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
