package edu.eci.tdse.orders.domain;

/**
 * Datos necesarios para crear un pedido.
 *
 * <p>Es un POJO mutable (constructor vacío + setters) porque el runtime de Java de AWS Lambda
 * lo instancia directamente a partir del JSON de entrada. {@code quantity} es {@code Integer}
 * para poder distinguir "no enviado" de un valor inválido.
 */
public class CreateOrderRequest {

    private String customerId;
    private String product;
    private Integer quantity;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String customerId, String product, Integer quantity) {
        this.customerId = customerId;
        this.product = product;
        this.quantity = quantity;
    }

    public String getCustomerId() {
        return customerId;
    }

    public void setCustomerId(String customerId) {
        this.customerId = customerId;
    }

    public String getProduct() {
        return product;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}
