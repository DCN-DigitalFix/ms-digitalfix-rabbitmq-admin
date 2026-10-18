package cl.duoc.ms_digitalfix_rabbitmq_admin.dto;

/**
 * Tipos de exchange que admite el administrador.
 *
 * Usar un enum en lugar de un String libre hace que Spring rechace un tipo
 * invalido durante la deserializacion, antes de llegar al servicio.
 */
public enum TipoExchange {
    DIRECT,
    TOPIC,
    FANOUT,
    HEADERS
}
