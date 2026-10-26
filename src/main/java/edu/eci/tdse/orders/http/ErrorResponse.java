package edu.eci.tdse.orders.http;

/**
 * Representación JSON de los errores de la API: {@code {"error": "...", "message": "..."}}.
 */
public record ErrorResponse(String error, String message) {
}
