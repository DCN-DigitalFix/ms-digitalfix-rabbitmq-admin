# ms-digitalfix-rabbitmq-admin

Microservicio administrador de RabbitMQ. Expone por HTTP la creación y
eliminación de colas, exchanges y bindings, de modo que la topología del
sistema se pueda operar sin entrar a la Management UI ni ejecutar
`rabbitmqctl` en la instancia.

---

## Endpoints

| Método | Ruta | Qué hace |
|---|---|---|
| POST | `/api/queues` | Declara una cola |
| GET | `/api/queues/{nombre}` | Mensajes pendientes y consumidores conectados |
| DELETE | `/api/queues/{nombre}` | Elimina la cola y sus mensajes |
| DELETE | `/api/queues/{nombre}/mensajes` | Vacía la cola sin eliminarla |
| POST | `/api/exchanges` | Declara un exchange |
| DELETE | `/api/exchanges/{nombre}` | Elimina el exchange y sus bindings |
| POST | `/api/bindings` | Enlaza un exchange con una cola |
| DELETE | `/api/bindings` | Deshace el enlace |
| GET | `/api/health` | Sonda de estado |

Vaciar una cola es `DELETE` sobre `/api/queues/{nombre}/mensajes` y no un
verbo propio: los mensajes son un subrecurso de la cola, y borrarlos es
exactamente lo que `DELETE` significa.

El `DELETE` de bindings lleva cuerpo porque un binding se identifica por tres
valores — exchange, cola y routing key — y meterlos en la URL obligaría a
escapar claves como `dispatch.#`.

No hay `GET` de exchanges: el protocolo AMQP no ofrece esa consulta, solo la
API HTTP del broker. Se prefiere no exponer un endpoint que no se puede
cumplir.

---

## Ejemplos

### Crear una cola con DLQ

```
POST /api/queues
Content-Type: application/json

{
  "nombre": "q.cmd.email",
  "durable": true,
  "dlxExchange": "cmd.dead.dlx",
  "dlxRoutingKey": "email.dlq"
}
```

```json
{
  "recurso": "q.cmd.email",
  "accion": "CREADO",
  "detalle": "Cola declarada en el broker"
}
```

### Crear un exchange

```
POST /api/exchanges

{ "nombre": "cmd.topic", "tipo": "TOPIC", "durable": true }
```

### Enlazarlos

```
POST /api/bindings

{ "exchange": "cmd.topic", "cola": "q.cmd.email", "routingKey": "email.*" }
```

### Consultar una cola

```
GET /api/queues/q.cmd.email
```

```json
{ "nombre": "q.cmd.email", "mensajes": 3, "consumidores": 2 }
```

---

## Validación

Las reglas viven en los DTO como anotaciones y Spring las aplica antes de que
el controlador se ejecute, así que al servicio nunca le llega una petición
inválida.

| Regla | Dónde |
|---|---|
| Nombre obligatorio y no vacío | `@NotBlank` |
| Solo letras, números, `.`, `-`, `_` | `@Pattern` |
| Máximo 255 caracteres | `@Size` |
| Tipo de exchange entre los cuatro válidos | `enum TipoExchange` |
| `dlxExchange` y `dlxRoutingKey` van juntos o no van | `tieneDlxIncompleto()` |

El enum hace que un tipo inventado se rechace durante la deserialización, sin
necesidad de validarlo a mano.

Una petición inválida devuelve 400 con un detalle por campo:

```json
{
  "timestamp": "2026-10-17T22:40:11.2",
  "status": 400,
  "error": "Bad Request",
  "message": "La peticion tiene campos invalidos",
  "path": "/api/queues",
  "detalles": [
    "nombre: el nombre de la cola es obligatorio"
  ]
}
```

| Código | Cuándo |
|---|---|
| 400 | Campos inválidos, JSON mal formado, o el broker rechazó la operación |
| 404 | La cola o el exchange no existe |

---

## Estructura

```
config/
  RabbitAdminConfig.java    bean RabbitAdmin, sin declaración automática
dto/
  CrearQueueRequest.java    validación de entrada
  CrearExchangeRequest.java
  CrearBindingRequest.java
  TipoExchange.java
  QueueInfoResponse.java
  OperacionResponse.java
service/
  RabbitAdminService.java   único punto que habla con el broker
controller/
  QueueController.java
  ExchangeController.java
  BindingController.java
  HealthController.java
exception/
  ApiError.java
  GlobalExceptionHandler.java
  RecursoNoEncontradoException.java
  OperacionInvalidaException.java
```

Los controladores no importan nada de `org.springframework.amqp`: llaman a
métodos de alto nivel de `RabbitAdminService` y reciben excepciones de
dominio, no `AmqpException`. Si se cambiara la librería de acceso al broker,
solo el servicio tendría que reescribirse.

---

## Ejecutar

Requiere el clúster de RabbitMQ levantado (ver el repo `infra`).

```
./mvnw spring-boot:run
```

Queda en el puerto 8090.

| Variable | Por defecto |
|---|---|
| `RABBIT_ADDRESSES` | `localhost:5672,localhost:5673` |
| `RABBIT_USER` | `digitalfix` |
| `RABBIT_PASS` | `digitalfix` |

---

## Seguridad

Todas las operaciones de administración exigen un JWT de Entra ID con el rol
`Admin`. EP3 lo pide explícitamente: *"el backend debe incluir filtros que
validen el JWT recibido desde el IDaaS para autorizar las peticiones"*.

`/api/health` queda público, lo que da el contraste 200 sin token frente a
401 en el resto — la evidencia que pide la pauta de presentación.

Además, el servicio queda fuera del API Gateway: no es alcanzable desde
internet, solo desde la red de la instancia. La validación del token es la
segunda capa, no la única.

Para probarlo, el token se obtiene del botón **Copiar access token** en
`/profile` del frontend.
