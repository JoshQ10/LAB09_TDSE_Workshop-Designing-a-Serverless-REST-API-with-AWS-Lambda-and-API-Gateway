package edu.eci.tdse.orders.domain;

public class InvalidOrderException extends OrderException {

    public static final String CODE = "INVALID_ORDER";

    public InvalidOrderException(String message) {
        super(CODE, message);
    }
}
