package cl.duoc.ms_digitalfix_rabbitmq_admin.exception;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Traduce las excepciones a respuestas HTTP con un cuerpo consistente.
 *
 * Centralizarlo aqui evita que cada controlador repita try/catch y garantiza
 * que todos los errores de la API tengan la misma forma.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Validacion de los DTO: devuelve un detalle por campo rechazado. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidacion(
            MethodArgumentNotValidException ex, WebRequest request) {

        List<String> detalles = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .toList();

        return ResponseEntity.badRequest().body(ApiError.conDetalles(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "La peticion tiene campos invalidos",
                path(request),
                detalles));
    }

    /**
     * JSON mal formado, o un valor que no corresponde al tipo esperado. El
     * caso tipico es un tipo de exchange que no existe en el enum.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleJsonInvalido(
            HttpMessageNotReadableException ex, WebRequest request) {

        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                "El cuerpo de la peticion no se pudo interpretar. "
                        + "Revisa el JSON y que el tipo de exchange sea DIRECT, TOPIC, FANOUT o HEADERS",
                path(request)));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ApiError> handleNoEncontrado(
            RecursoNoEncontradoException ex, WebRequest request) {

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(
                HttpStatus.NOT_FOUND.value(),
                "Not Found",
                ex.getMessage(),
                path(request)));
    }

    @ExceptionHandler(OperacionInvalidaException.class)
    public ResponseEntity<ApiError> handleOperacionInvalida(
            OperacionInvalidaException ex, WebRequest request) {

        return ResponseEntity.badRequest().body(ApiError.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad Request",
                ex.getMessage(),
                path(request)));
    }

    private String path(WebRequest request) {
        return request.getDescription(false).replace("uri=", "");
    }
}
