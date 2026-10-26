package edu.eci.tdse.orders.lambda.mapping;

import edu.eci.tdse.orders.application.OrderService;
import edu.eci.tdse.orders.application.SampleOrders;
import edu.eci.tdse.orders.domain.CreateOrderRequest;
import edu.eci.tdse.orders.domain.Order;
import edu.eci.tdse.orders.domain.OrderException;

import java.util.List;
import java.util.function.Supplier;

/**
 * Handlers de Lambda para la arquitectura A (integración no-proxy con plantillas de mapeo).
 *
 * <p>Cada método público es el handler de una función Lambda distinta, configurado como
 * {@code edu.eci.tdse.orders.lambda.mapping.OrderFunctions::metodo}. Reciben y devuelven POJOs:
 * el runtime de Java de Lambda convierte el JSON de entrada en objetos y el valor de retorno en
 * JSON. No conocen HTTP: los estados HTTP los decide API Gateway.
 */
public class OrderFunctions {

    static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    // Se crea una vez por entorno de ejecución (cold start) y se reutiliza mientras esté caliente.
    private static final OrderService DEFAULT_SERVICE = new OrderService(SampleOrders.newRepository());

    private final OrderService service;

    public OrderFunctions() {
        this(DEFAULT_SERVICE);
    }

    OrderFunctions(OrderService service) {
        this.service = service;
    }

    public Order createOrder(CreateOrderRequest request) {
        return execute(() -> service.createOrder(request));
    }

    public Order getOrder(OrderIdInput input) {
        return execute(() -> service.getOrder(input == null ? null : input.getId()));
    }

    public List<Order> getOrdersByCustomer(CustomerQueryInput input) {
        return execute(() -> service.getOrdersByCustomer(input == null ? null : input.getCustomerId()));
    }

    public void deleteOrder(OrderIdInput input) {
        execute(() -> {
            service.deleteOrder(input == null ? null : input.getId());
            return null;
        });
    }

    private static <T> T execute(Supplier<T> operation) {
        try {
            return operation.get();
        } catch (OrderException e) {
            throw new OrderFunctionException(e.getCode(), e.getMessage(), e);
        } catch (RuntimeException e) {
            throw new OrderFunctionException(INTERNAL_ERROR, "Unexpected internal error", e);
        }
    }
}
