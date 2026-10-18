package cl.duoc.ms_digitalfix_rabbitmq_admin.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.duoc.ms_digitalfix_rabbitmq_admin.dto.CrearExchangeRequest;
import cl.duoc.ms_digitalfix_rabbitmq_admin.dto.OperacionResponse;
import cl.duoc.ms_digitalfix_rabbitmq_admin.service.RabbitAdminService;

import jakarta.validation.Valid;

/**
 * Administracion de exchanges.
 *
 * No hay GET: el protocolo AMQP no ofrece una consulta de exchanges, solo la
 * API HTTP de administracion del broker. Se prefiere no exponer un endpoint
 * que no se pueda cumplir antes que devolver datos incompletos.
 *
 * Todas las operaciones exigen el rol Admin. EP3 pide que el backend valide
 * el JWT del IDaaS para autorizar las peticiones, y administrar la topologia
 * de mensajeria es la operacion mas sensible del sistema.
 */
@RestController
@RequestMapping("/api/exchanges")
public class ExchangeController {

    private final RabbitAdminService adminService;

    public ExchangeController(RabbitAdminService adminService) {
        this.adminService = adminService;
    }

    /** POST /api/exchanges */
    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<OperacionResponse> crear(@Valid @RequestBody CrearExchangeRequest request) {
        String nombre = adminService.crearExchange(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(OperacionResponse.creado(
                nombre, "Exchange de tipo " + request.tipo() + " declarado en el broker"));
    }

    /** DELETE /api/exchanges/{nombre} */
    @DeleteMapping("/{nombre}")
    @PreAuthorize("hasRole('Admin')")
    public ResponseEntity<OperacionResponse> eliminar(@PathVariable String nombre) {
        adminService.eliminarExchange(nombre);

        return ResponseEntity.ok(OperacionResponse.eliminado(
                nombre, "Exchange eliminado junto con sus bindings"));
    }
}
