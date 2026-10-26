package edu.eci.tdse.orders.http;

import java.util.Map;

/**
 * Respuesta HTTP mínima, independiente de AWS.
 */
public record HttpResponse(int statusCode, Map<String, String> headers, String body) {

    public HttpResponse {
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
