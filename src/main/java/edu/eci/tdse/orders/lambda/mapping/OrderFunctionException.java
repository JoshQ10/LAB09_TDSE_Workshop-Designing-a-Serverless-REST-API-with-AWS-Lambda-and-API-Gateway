package edu.eci.tdse.orders.lambda.mapping;

/**
 * Error que Lambda devuelve a API Gateway como {@code {"errorMessage": "...", "errorType": "..."}}.
 *
 * <p>El mensaje empieza por el código de error ({@code ORDER_NOT_FOUND: ...}) porque en la
 * integración no-proxy API Gateway solo puede elegir el estado HTTP aplicando una expresión
 * regular sobre {@code errorMessage}.
 */
public class OrderFunctionException extends RuntimeException {

    public OrderFunctionException(String code, String message, Throwable cause) {
        super(code + ": " + message, cause);
    }
}
