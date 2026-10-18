package cl.duoc.ms_digitalfix_rabbitmq_admin.config;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Acceso administrativo al broker.
 *
 * Este servicio no declara ninguna topologia propia: su trabajo es crear y
 * eliminar la que le pidan por HTTP. Por eso se desactiva la declaracion
 * automatica al arrancar, que de otro modo intentaria recrear cualquier bean
 * Queue o Exchange presente en el contexto.
 *
 * ignoreDeclarationExceptions se deja en false para que un conflicto con el
 * broker llegue como excepcion al servicio, que la traduce a un 400 con el
 * motivo. Con true el fallo se silenciaria y la API responderia 201 sin haber
 * creado nada.
 */
@Configuration
public class RabbitAdminConfig {

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        RabbitAdmin admin = new RabbitAdmin(connectionFactory);
        admin.setAutoStartup(false);
        admin.setIgnoreDeclarationExceptions(false);
        return admin;
    }
}
