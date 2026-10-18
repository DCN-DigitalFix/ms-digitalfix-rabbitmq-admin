package cl.duoc.ms_digitalfix_rabbitmq_admin.dto;

/**
 * Resultado de una operacion de administracion.
 *
 * @param recurso  nombre del recurso afectado
 * @param accion   que se hizo con el
 * @param detalle  texto legible para quien llamo
 */
public record OperacionResponse(String recurso, String accion, String detalle) {

    public static OperacionResponse creado(String recurso, String detalle) {
        return new OperacionResponse(recurso, "CREADO", detalle);
    }

    public static OperacionResponse eliminado(String recurso, String detalle) {
        return new OperacionResponse(recurso, "ELIMINADO", detalle);
    }

    public static OperacionResponse purgado(String recurso, String detalle) {
        return new OperacionResponse(recurso, "PURGADO", detalle);
    }
}
