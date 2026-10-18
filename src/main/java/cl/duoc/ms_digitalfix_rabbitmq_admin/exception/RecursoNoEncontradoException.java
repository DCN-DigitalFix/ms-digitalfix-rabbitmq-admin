package cl.duoc.ms_digitalfix_rabbitmq_admin.exception;

/** La cola, exchange o binding referenciado no existe en el broker. Mapea a 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String message) {
        super(message);
    }
}
