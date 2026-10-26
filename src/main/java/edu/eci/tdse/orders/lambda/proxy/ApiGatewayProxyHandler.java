package edu.eci.tdse.orders.lambda.proxy;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import edu.eci.tdse.orders.application.OrderService;
import edu.eci.tdse.orders.application.SampleOrders;
import edu.eci.tdse.orders.http.HttpRequest;
import edu.eci.tdse.orders.http.HttpResponse;
import edu.eci.tdse.orders.http.OrdersHttpAdapter;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Handler de Lambda para la arquitectura B (integración Lambda proxy).
 *
 * <p>Es la única clase que conoce los eventos de API Gateway: traduce el evento a un
 * {@link HttpRequest} y el {@link HttpResponse} al formato que espera API Gateway.
 * Handler: {@code edu.eci.tdse.orders.lambda.proxy.ApiGatewayProxyHandler::handleRequest}.
 */
public class ApiGatewayProxyHandler
        implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private static final OrdersHttpAdapter DEFAULT_ADAPTER =
            new OrdersHttpAdapter(new OrderService(SampleOrders.newRepository()));

    private final OrdersHttpAdapter adapter;

    public ApiGatewayProxyHandler() {
        this(DEFAULT_ADAPTER);
    }

    ApiGatewayProxyHandler(OrdersHttpAdapter adapter) {
        this.adapter = adapter;
    }

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        HttpResponse response = adapter.handle(toHttpRequest(event));
        return new APIGatewayProxyResponseEvent()
                .withStatusCode(response.statusCode())
                .withHeaders(response.headers())
                .withBody(response.body());
    }

    static HttpRequest toHttpRequest(APIGatewayProxyRequestEvent event) {
        String body = event.getBody();
        if (body != null && Boolean.TRUE.equals(event.getIsBase64Encoded())) {
            body = new String(Base64.getDecoder().decode(body), StandardCharsets.UTF_8);
        }
        return new HttpRequest(event.getHttpMethod(), event.getResource(),
                event.getPathParameters(), event.getQueryStringParameters(), body);
    }
}
