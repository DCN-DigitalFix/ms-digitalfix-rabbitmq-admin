package cl.duoc.ms_digitalfix_rabbitmq_admin.exception;

/**
 * La peticion era sintacticamente valida pero el broker no pudo aplicarla:
 * conflicto de argumentos, exchange inexistente, permisos. Mapea a 400.
 */
public class OperacionInvalidaException extends RuntimeException {

    public OperacionInvalidaException(String message) {
        super(message);
    }

    public OperacionInvalidaException(String message, Throwable cause) {
        super(message, cause);
    }
}
