package edu.eci.tdse.orders.domain;

/**
 * Error de negocio con un código estable que forma parte del contrato de la API
 * (por ejemplo {@code ORDER_NOT_FOUND}). No contiene códigos de estado HTTP: traducir
 * el código a un estado HTTP es responsabilidad de la capa que expone la API.
 */
public abstract class OrderException extends RuntimeException {

    private final String code;

    protected OrderException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
