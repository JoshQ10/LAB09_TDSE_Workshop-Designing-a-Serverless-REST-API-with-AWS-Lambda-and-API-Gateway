package edu.eci.tdse.orders.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.eci.tdse.orders.application.OrderService;
import edu.eci.tdse.orders.domain.CreateOrderRequest;
import edu.eci.tdse.orders.domain.InvalidOrderException;
import edu.eci.tdse.orders.domain.Order;
import edu.eci.tdse.orders.domain.OrderNotFoundException;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adaptador HTTP de la arquitectura B (integración proxy).
 *
 * <p>Aquí viven las responsabilidades HTTP: enrutamiento por método y recurso, lectura de
 * parámetros, (de)serialización JSON, códigos de estado y cabeceras. No depende de clases de AWS,
 * así que se prueba localmente con peticiones construidas a mano.
 */
public class OrdersHttpAdapter {

    static final String ORDERS = "/orders";
    static final String ORDER_BY_ID = "/orders/{id}";

    private static final Logger LOGGER = Logger.getLogger(OrdersHttpAdapter.class.getName());
    private static final Map<String, String> JSON_HEADERS = Map.of("Content-Type", "application/json");

    private final OrderService service;
    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public OrdersHttpAdapter(OrderService service) {
        this.service = service;
    }

    public HttpResponse handle(HttpRequest request) {
        try {
            return route(request);
        } catch (InvalidOrderException e) {
            return error(400, e.getCode(), e.getMessage());
        } catch (OrderNotFoundException e) {
            return error(404, e.getCode(), e.getMessage());
        } catch (RuntimeException e) {
            LOGGER.log(Level.SEVERE, "Unexpected error handling " + request.method() + " " + request.resource(), e);
            return error(500, "INTERNAL_ERROR", "Unexpected internal error");
        }
    }

    private HttpResponse route(HttpRequest request) {
        String method = request.method() == null ? "" : request.method().toUpperCase();
        String resource = request.resource() == null ? "" : request.resource();

        switch (resource) {
            case ORDERS -> {
                return switch (method) {
                    case "POST" -> createOrder(request);
                    case "GET" -> json(200, service.getOrdersByCustomer(request.queryParameter("customerId")));
                    default -> methodNotAllowed(method, resource);
                };
            }
            case ORDER_BY_ID -> {
                return switch (method) {
                    case "GET" -> json(200, service.getOrder(request.pathParameter("id")));
                    case "DELETE" -> deleteOrder(request);
                    default -> methodNotAllowed(method, resource);
                };
            }
            default -> {
                return error(404, "ROUTE_NOT_FOUND", "No route for " + method + " " + resource);
            }
        }
    }

    private HttpResponse createOrder(HttpRequest request) {
        Order order = service.createOrder(parseCreateRequest(request.body()));
        return new HttpResponse(201,
                Map.of("Content-Type", "application/json", "Location", ORDERS + "/" + order.getId()),
                write(order));
    }

    private HttpResponse deleteOrder(HttpRequest request) {
        service.deleteOrder(request.pathParameter("id"));
        return new HttpResponse(204, Map.of(), null);
    }

    private CreateOrderRequest parseCreateRequest(String body) {
        if (body == null || body.isBlank()) {
            throw new InvalidOrderException("Request body is required");
        }
        try {
            return mapper.readValue(body, CreateOrderRequest.class);
        } catch (JsonProcessingException e) {
            throw new InvalidOrderException("Request body must be a valid order JSON document");
        }
    }

    private HttpResponse methodNotAllowed(String method, String resource) {
        return error(405, "METHOD_NOT_ALLOWED", "Method " + method + " is not allowed on " + resource);
    }

    private HttpResponse json(int status, Object body) {
        return new HttpResponse(status, JSON_HEADERS, write(body));
    }

    private HttpResponse error(int status, String code, String message) {
        return json(status, new ErrorResponse(code, message));
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize response", e);
        }
    }
}
