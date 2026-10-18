package cl.duoc.ms_digitalfix_rabbitmq_admin.service;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Exchange;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.amqp.core.HeadersExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueInformation;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.stereotype.Service;

import cl.duoc.ms_digitalfix_rabbitmq_admin.dto.CrearBindingRequest;
import cl.duoc.ms_digitalfix_rabbitmq_admin.dto.CrearExchangeRequest;
import cl.duoc.ms_digitalfix_rabbitmq_admin.dto.CrearQueueRequest;
import cl.duoc.ms_digitalfix_rabbitmq_admin.dto.QueueInfoResponse;
import cl.duoc.ms_digitalfix_rabbitmq_admin.exception.OperacionInvalidaException;
import cl.duoc.ms_digitalfix_rabbitmq_admin.exception.RecursoNoEncontradoException;

/**
 * Unico punto del servicio que habla con RabbitMQ.
 *
 * Los controladores llaman a metodos de alto nivel — crearCola, eliminarCola,
 * crearBinding — y no conocen RabbitAdmin, QueueBuilder ni los tipos de
 * Exchange de Spring AMQP. Si manana se cambiara la libreria de acceso al
 * broker, solo esta clase tendria que reescribirse.
 *
 * Aqui tambien se traducen los fallos del broker a las excepciones de
 * dominio del servicio, de modo que el controlador nunca vea una
 * AmqpException.
 */
@Service
public class RabbitAdminService {

    private static final Logger log = LoggerFactory.getLogger(RabbitAdminService.class);

    private final RabbitAdmin rabbitAdmin;

    public RabbitAdminService(RabbitAdmin rabbitAdmin) {
        this.rabbitAdmin = rabbitAdmin;
    }

    // -----------------------------------------------------------------------
    // Colas
    // -----------------------------------------------------------------------

    /**
     * Declara una cola. La operacion es idempotente mientras los argumentos
     * coincidan: si la cola ya existe con la misma definicion, el broker la
     * acepta sin cambios.
     *
     * @return el nombre de la cola creada
     */
    public String crearCola(CrearQueueRequest request) {
        if (request.tieneDlxIncompleto()) {
            throw new OperacionInvalidaException(
                    "Para derivar a una DLQ hay que indicar dlxExchange y dlxRoutingKey juntos");
        }

        Map<String, Object> argumentos = new HashMap<>();
        if (request.argumentos() != null) {
            argumentos.putAll(request.argumentos());
        }
        if (request.dlxExchange() != null && !request.dlxExchange().isBlank()) {
            argumentos.put("x-dead-letter-exchange", request.dlxExchange());
            argumentos.put("x-dead-letter-routing-key", request.dlxRoutingKey());
        }

        Queue cola = new Queue(
                request.nombre(),
                request.durableODefecto(),
                request.exclusivaODefecto(),
                request.autoDeleteODefecto(),
                argumentos.isEmpty() ? null : argumentos);

        try {
            String nombre = rabbitAdmin.declareQueue(cola);

            // declareQueue devuelve null cuando el broker rechaza la
            // declaracion, tipicamente porque la cola ya existe con otros
            // argumentos. Es un conflicto, no un fallo de conexion.
            if (nombre == null) {
                throw new OperacionInvalidaException(
                        "El broker rechazo la cola '" + request.nombre()
                                + "'. Suele ocurrir cuando ya existe con argumentos distintos");
            }

            log.info("Cola '{}' declarada (durable={}, argumentos={})",
                    nombre, request.durableODefecto(), argumentos);
            return nombre;

        } catch (AmqpException e) {
            throw new OperacionInvalidaException(
                    "No se pudo crear la cola '" + request.nombre() + "': " + e.getMessage(), e);
        }
    }

    public QueueInfoResponse obtenerCola(String nombre) {
        QueueInformation info = rabbitAdmin.getQueueInfo(nombre);

        if (info == null) {
            throw new RecursoNoEncontradoException("No existe la cola '" + nombre + "'");
        }

        return new QueueInfoResponse(
                info.getName(),
                info.getMessageCount(),
                info.getConsumerCount());
    }

    /** Elimina la cola y todo lo que tenga encolado. */
    public void eliminarCola(String nombre) {
        boolean eliminada = rabbitAdmin.deleteQueue(nombre);

        if (!eliminada) {
            throw new RecursoNoEncontradoException("No existe la cola '" + nombre + "'");
        }

        log.info("Cola '{}' eliminada", nombre);
    }

    /** Descarta los mensajes pendientes sin eliminar la cola. */
    public long purgarCola(String nombre) {
        // Se consulta primero para distinguir una cola vacia de una que no
        // existe: purgeQueue devuelve 0 en ambos casos.
        QueueInformation info = rabbitAdmin.getQueueInfo(nombre);
        if (info == null) {
            throw new RecursoNoEncontradoException("No existe la cola '" + nombre + "'");
        }

        long descartados = info.getMessageCount();
        rabbitAdmin.purgeQueue(nombre);

        log.info("Cola '{}' purgada, {} mensajes descartados", nombre, descartados);
        return descartados;
    }

    // -----------------------------------------------------------------------
    // Exchanges
    // -----------------------------------------------------------------------

    public String crearExchange(CrearExchangeRequest request) {
        Exchange exchange = construirExchange(request);

        try {
            rabbitAdmin.declareExchange(exchange);
            log.info("Exchange '{}' declarado de tipo {}", request.nombre(), request.tipo());
            return request.nombre();

        } catch (AmqpException e) {
            throw new OperacionInvalidaException(
                    "No se pudo crear el exchange '" + request.nombre() + "': " + e.getMessage(), e);
        }
    }

    public void eliminarExchange(String nombre) {
        boolean eliminado = rabbitAdmin.deleteExchange(nombre);

        if (!eliminado) {
            throw new RecursoNoEncontradoException("No existe el exchange '" + nombre + "'");
        }

        log.info("Exchange '{}' eliminado", nombre);
    }

    // -----------------------------------------------------------------------
    // Bindings
    // -----------------------------------------------------------------------

    public void crearBinding(CrearBindingRequest request) {
        // Un binding hacia una cola inexistente cierra el canal con un 404 del
        // broker. Se verifica antes para devolver un error entendible.
        if (rabbitAdmin.getQueueInfo(request.cola()) == null) {
            throw new RecursoNoEncontradoException(
                    "No existe la cola de destino '" + request.cola() + "'");
        }

        try {
            rabbitAdmin.declareBinding(construirBinding(request));
            log.info("Binding creado: {} --{}--> {}",
                    request.exchange(), request.routingKeyODefecto(), request.cola());

        } catch (AmqpException e) {
            throw new OperacionInvalidaException(
                    "No se pudo crear el binding. Verifica que el exchange '"
                            + request.exchange() + "' exista: " + e.getMessage(), e);
        }
    }

    public void eliminarBinding(CrearBindingRequest request) {
        try {
            rabbitAdmin.removeBinding(construirBinding(request));
            log.info("Binding eliminado: {} --{}--> {}",
                    request.exchange(), request.routingKeyODefecto(), request.cola());

        } catch (AmqpException e) {
            throw new OperacionInvalidaException(
                    "No se pudo eliminar el binding: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------

    private Exchange construirExchange(CrearExchangeRequest request) {
        boolean durable = request.durableODefecto();
        boolean autoDelete = request.autoDeleteODefecto();
        Map<String, Object> args = request.argumentos();

        return switch (request.tipo()) {
            case DIRECT -> new DirectExchange(request.nombre(), durable, autoDelete, args);
            case TOPIC -> new TopicExchange(request.nombre(), durable, autoDelete, args);
            case FANOUT -> new FanoutExchange(request.nombre(), durable, autoDelete, args);
            case HEADERS -> new HeadersExchange(request.nombre(), durable, autoDelete, args);
        };
    }

    private Binding construirBinding(CrearBindingRequest request) {
        // Se usa el constructor directo en lugar de BindingBuilder porque el
        // tipo del exchange de destino no se conoce aqui: solo su nombre.
        return new Binding(
                request.cola(),
                Binding.DestinationType.QUEUE,
                request.exchange(),
                request.routingKeyODefecto(),
                null);
    }
}
