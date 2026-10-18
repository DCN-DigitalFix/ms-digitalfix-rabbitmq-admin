package cl.duoc.ms_digitalfix_rabbitmq_admin.dto;

import java.util.Map;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de creacion de un exchange.
 *
 * @param nombre     nombre del exchange, p. ej. cmd.direct
 * @param tipo       direct, topic, fanout o headers
 * @param durable    si sobrevive al reinicio del broker; por defecto true
 * @param autoDelete si se borra cuando deja de tener bindings
 * @param argumentos argumentos extra del broker
 */
public record CrearExchangeRequest(

        @NotBlank(message = "el nombre del exchange es obligatorio")
        @Size(max = 255, message = "el nombre no puede superar 255 caracteres")
        @Pattern(
                regexp = "^[a-zA-Z0-9._-]+$",
                message = "el nombre solo admite letras, numeros, punto, guion y guion bajo")
        String nombre,

        @NotNull(message = "el tipo es obligatorio: DIRECT, TOPIC, FANOUT o HEADERS")
        TipoExchange tipo,

        Boolean durable,
        Boolean autoDelete,
        Map<String, Object> argumentos) {

    public boolean durableODefecto() {
        return durable == null || durable;
    }

    public boolean autoDeleteODefecto() {
        return autoDelete != null && autoDelete;
    }
}
