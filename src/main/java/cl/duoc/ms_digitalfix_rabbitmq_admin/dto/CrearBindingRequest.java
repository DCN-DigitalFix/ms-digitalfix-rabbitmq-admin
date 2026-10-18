package cl.duoc.ms_digitalfix_rabbitmq_admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Solicitud de creacion de un binding entre un exchange y una cola.
 *
 * La routing key se admite vacia porque en un exchange fanout se ignora, pero
 * no se admite nula: el productor debe decidir conscientemente.
 *
 * @param exchange   exchange de origen, ya existente
 * @param cola       cola de destino, ya existente
 * @param routingKey clave o patron del enlace
 */
public record CrearBindingRequest(

        @NotBlank(message = "el exchange de origen es obligatorio")
        @Size(max = 255, message = "el nombre del exchange no puede superar 255 caracteres")
        String exchange,

        @NotBlank(message = "la cola de destino es obligatoria")
        @Size(max = 255, message = "el nombre de la cola no puede superar 255 caracteres")
        String cola,

        @Size(max = 255, message = "la routing key no puede superar 255 caracteres")
        String routingKey) {

    public String routingKeyODefecto() {
        return routingKey == null ? "" : routingKey;
    }
}
