package cl.duoc.ms_digitalfix_rabbitmq_admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ms_digitalfix_rabbitmq_admin.dto.CrearBindingRequest;
import cl.duoc.ms_digitalfix_rabbitmq_admin.dto.OperacionResponse;
import cl.duoc.ms_digitalfix_rabbitmq_admin.service.RabbitAdminService;

import jakarta.validation.Valid;

/**
 * Administracion de bindings.
 *
 * El DELETE lleva cuerpo en lugar de parametros de ruta porque un binding se
 * identifica por tres valores — exchange, cola y routing key — y meterlos en
 * la URL obligaria a escapar claves como "dispatch.#".
 *
 * Todas las operaciones exigen el rol Admin. EP3 pide que el backend valide
 * el JWT del IDaaS para autorizar las peticiones, y administrar la topologia
 * de mensajeria es la operacion mas sensible del sistema.
 */
@RestController
@RequestMapping("/api/bindings")
public class BindingController {

    private final RabbitAdminService adminService;

    public BindingController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    /** POST /api/bindings */
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<OperacionResponse> crear(@Valid @RequestBody CrearBindingRequest request) {
        adminService.crearBinding(request);

        String descripcion = request.exchange() + " -> " + request.cola();
        return ResponseEntity.status(HttpStatus.CREATED).body(OperacionResponse.creado(
                descripcion, "Binding con routing key '" + request.routingKeyODefecto() + "'"));
    }

    /** DELETE /api/bindings */
    @DeleteMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<OperacionResponse> eliminar(@Valid @RequestBody CrearBindingRequest request) {
        adminService.eliminarBinding(request);

        String descripcion = request.exchange() + " -> " + request.cola();
        return ResponseEntity.ok(OperacionResponse.eliminado(descripcion, "Binding eliminado"));
    }
}
