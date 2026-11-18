package com.utp.sistema_tickets.controller;

import com.utp.sistema_tickets.dto.UsuarioAdminRequest;
import com.utp.sistema_tickets.dto.UsuarioResponse;
import com.utp.sistema_tickets.model.Rol;
import com.utp.sistema_tickets.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuarios", description = "Administración de usuarios y consulta del catálogo de técnicos.")
@SecurityRequirement(name = "bearerAuth")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    @Operation(summary = "Crear usuario administrado", description = "Disponible solo para ADMIN; permite asignar USUARIO, TECNICO o ADMIN.")
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(
                usuarioService.crearAdministrado(
                        request.getNombre(),
                        request.getEmail(),
                        request.getPassword(),
                        request.getRol())));
    }

    @GetMapping
    @Operation(summary = "Listar usuarios", description = "Disponible solo para ADMIN.")
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarCatalogo(null).stream()
                .map(UsuarioResponse::from)
                .toList());
    }
    @GetMapping("/catalogo")
    @Operation(summary = "Consultar catálogo de usuarios", description = "Disponible para TECNICO y ADMIN; se puede filtrar por rol.")
    public ResponseEntity<List<UsuarioResponse>> listarCatalogo(
            @RequestParam(required = false) Rol rol) {
        return ResponseEntity.ok(usuarioService.listarCatalogo(rol).stream()
                .map(UsuarioResponse::from)
                .toList());
    }
}
