package cl.duoc.ms_digitalfix_rabbitmq_admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ms_digitalfix_rabbitmq_admin.dto.CrearQueueRequest;
import cl.duoc.ms_digitalfix_rabbitmq_admin.dto.OperacionResponse;
import cl.duoc.ms_digitalfix_rabbitmq_admin.dto.QueueInfoResponse;
import cl.duoc.ms_digitalfix_rabbitmq_admin.service.RabbitAdminService;

import jakarta.validation.Valid;

/**
 * Administracion de colas.
 *
 * El recurso es /api/queues y los verbos siguen su significado habitual:
 * POST crea, GET consulta, DELETE elimina. Los mensajes de una cola se tratan
 * como un subrecurso, de modo que vaciarla sin borrarla es DELETE sobre
 * /api/queues/{nombre}/mensajes y no un verbo inventado.
 *
 * El controlador no conoce RabbitMQ: delega todo en RabbitAdminService.
 *
 * Todas las operaciones exigen el rol Admin. EP3 pide que el backend valide
 * el JWT del IDaaS para autorizar las peticiones, y administrar la topologia
 * de mensajeria es la operacion mas sensible del sistema.
 */
@RestController
@RequestMapping("/api/queues")
public class QueueController {

    private final RabbitAdminService adminService;

    public QueueController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    /** POST /api/queues */
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<OperacionResponse> crear(@Valid @RequestBody CrearQueueRequest request) {
        String nombre = adminService.crearCola(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                OperacionResponse.creado(nombre, "Cola declarada en el broker"));
    }

    /** GET /api/queues/{nombre} */
    @GetMapping("/{nombre}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<QueueInfoResponse> obtener(@PathVariable String nombre) {
        return ResponseEntity.ok(adminService.obtenerCola(nombre));
    }

    /** DELETE /api/queues/{nombre} */
    @DeleteMapping("/{nombre}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<OperacionResponse> eliminar(@PathVariable String nombre) {
        adminService.eliminarCola(nombre);

        return ResponseEntity.ok(
                OperacionResponse.eliminado(nombre, "Cola eliminada junto con sus mensajes"));
    }

    /** DELETE /api/queues/{nombre}/mensajes */
    @DeleteMapping("/{nombre}/mensajes")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<OperacionResponse> purgar(@PathVariable String nombre) {
        long descartados = adminService.purgarCola(nombre);

        return ResponseEntity.ok(OperacionResponse.purgado(
                nombre, descartados + " mensajes descartados"));
    }
}
