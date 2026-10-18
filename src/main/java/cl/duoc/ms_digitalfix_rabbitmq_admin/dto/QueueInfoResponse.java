package cl.duoc.ms_digitalfix_rabbitmq_admin.dto;

/**
 * Estado de una cola.
 *
 * Los contadores son long porque es lo que devuelve QueueInformation: una
 * cola puede acumular mas mensajes de los que cabe en un int.
 *
 * @param nombre       nombre de la cola
 * @param mensajes     mensajes pendientes de consumir
 * @param consumidores consumidores conectados
 */
public record QueueInfoResponse(String nombre, long mensajes, long consumidores) {}