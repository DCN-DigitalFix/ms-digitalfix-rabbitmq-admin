package cl.duoc.ms_digitalfix_rabbitmq_admin.exception;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cuerpo uniforme de error.
 *
 * El campo detalles lleva una entrada por campo invalido cuando la peticion
 * no pasa la validacion, de modo que quien llama sepa exactamente que
 * corregir en lugar de recibir un "bad request" a secas.
 */
public record ApiError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<String> detalles) {

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, List.of());
    }

    public static ApiError conDetalles(
            int status, String error, String message, String path, List<String> detalles) {
        return new ApiError(LocalDateTime.now(), status, error, message, path, detalles);
    }

    /**
     * Serializacion manual para los handlers de seguridad, que escriben
     * directo en la respuesta sin pasar por el conversor de Spring MVC.
     */
    public static String json(int status, String error, String message, String path) {
        return """
                {"timestamp":"%s","status":%d,"error":"%s","message":"%s","path":"%s","detalles":[]}"""
                .formatted(LocalDateTime.now(), status, error, message, path);
    }
}
