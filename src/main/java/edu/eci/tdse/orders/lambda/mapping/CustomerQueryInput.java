package edu.eci.tdse.orders.lambda.mapping;

/**
 * Entrada {@code {"customerId": "CUST-42"}} que construye la plantilla de mapeo de API Gateway
 * a partir del query string {@code ?customerId=CUST-42}.
 */
public class CustomerQueryInput {

    private String customerId;

    public CustomerQueryInput() {
    }

    public CustomerQueryInput(String customerId) {
        this.customerId = customerId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }
}
