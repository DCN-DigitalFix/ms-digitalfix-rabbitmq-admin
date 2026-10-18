package cl.duoc.ms_digitalfix_rabbitmq_admin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de creacion de una cola.
 *
 * La validacion vive en las anotaciones y la aplica Spring antes de que el
 * controlador se ejecute, de modo que al servicio nunca le llega un nombre
 * vacio ni con caracteres que RabbitMQ rechazaria.
 *
 * @param nombre      nombre de la cola, p. ej. q.cmd.email
 * @param durable     si sobrevive al reinicio del broker; por defecto true
 * @param exclusiva   si solo la usa la conexion que la creo
 * @param autoDelete  si se borra al desconectarse el ultimo consumidor
 * @param dlxExchange dead letter exchange al que derivar los mensajes fallidos
 * @param dlxRoutingKey clave con la que el mensaje llega a la DLQ
 * @param argumentos  argumentos extra del broker, p. ej. x-message-ttl
 */
public record CrearQueueRequest(

        @NotBlank(message = "el nombre de la cola es obligatorio")
        @Size(max = 255, message = "el nombre no puede superar 255 caracteres")
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "el nombre solo admite letras, numeros, punto, guion y guion bajo")
        String nombre,

        Boolean durable,
        Boolean exclusiva,
        Boolean autoDelete,

        @Size(max = 255, message = "el nombre del DLX no puede superar 255 caracteres")
        String dlxExchange,

        @Size(max = 255, message = "la routing key del DLX no puede superar 255 caracteres")
        String dlxRoutingKey,

        Map<String, Object> argumentos) {

    /** Una cola sin indicacion explicita se crea durable: es el caso seguro. */
    public boolean durableODefecto() {
        return durable == null || durable;
    }

    public boolean exclusivaODefecto() {
        return exclusiva != null && exclusiva;
    }

    public boolean autoDeleteODefecto() {
        return autoDelete != null && autoDelete;
    }

    /**
     * Pedir DLX sin routing key, o al reves, deja una configuracion a medias
     * que el broker acepta pero que no deriva nada. Se rechaza antes.
     */
    public boolean tieneDlxIncompleto() {
        boolean hayExchange = dlxExchange != null && !dlxExchange.isBlank();
        boolean hayClave = dlxRoutingKey != null && !dlxRoutingKey.isBlank();
        return hayExchange != hayClave;
    }
}
