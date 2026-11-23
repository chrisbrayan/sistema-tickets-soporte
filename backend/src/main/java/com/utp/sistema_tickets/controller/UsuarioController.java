package com.utp.sistema_tickets.controller;

import com.utp.sistema_tickets.dto.UsuarioAdminRequest;
import com.utp.sistema_tickets.dto.UsuarioResponse;
import com.utp.sistema_tickets.model.Rol;
import com.utp.sistema_tickets.service.UsuarioService;
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
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@Valid @RequestBody UsuarioAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(UsuarioResponse.from(
                usuarioService.crearAdministrado(
                        request.getNombre(),
                        request.getEmail(),
                        request.getPassword(),
                        request.getRol())));
    }

    @GetMapping
    public ResponseEntity<List<UsuarioResponse>> listar() {
        return ResponseEntity.ok(usuarioService.listarCatalogo(null).stream()
                .map(UsuarioResponse::from)
                .toList());
    }
    @GetMapping("/catalogo")
    public ResponseEntity<List<UsuarioResponse>> listarCatalogo(
            @RequestParam(required = false) Rol rol) {
        return ResponseEntity.ok(usuarioService.listarCatalogo(rol).stream()
                .map(UsuarioResponse::from)
                .toList());
    }
}
