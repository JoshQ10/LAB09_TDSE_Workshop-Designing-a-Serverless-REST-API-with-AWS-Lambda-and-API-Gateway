package edu.eci.tdse.orders.http;

import java.util.Map;

/**
 * Petición HTTP mínima, independiente de AWS. {@code resource} es la plantilla de ruta
 * (por ejemplo {@code /orders/{id}}), no la ruta concreta.
 */
public record HttpRequest(String method,
                          String resource,
                          Map<String, String> pathParameters,
                          Map<String, String> queryParameters,
                          String body) {

    public HttpRequest {
        pathParameters = pathParameters == null ? Map.of() : Map.copyOf(pathParameters);
        queryParameters = queryParameters == null ? Map.of() : Map.copyOf(queryParameters);
    }

    public String pathParameter(String name) {
        return pathParameters.get(name);
    }

    public String queryParameter(String name) {
        return queryParameters.get(name);
    }
}
