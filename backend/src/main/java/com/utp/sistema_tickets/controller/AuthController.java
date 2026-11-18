package com.utp.sistema_tickets.controller;

import com.utp.sistema_tickets.dto.LoginRequest;
import com.utp.sistema_tickets.dto.LoginResponse;
import com.utp.sistema_tickets.dto.RegistroRequest;
import com.utp.sistema_tickets.dto.UsuarioResponse;
import com.utp.sistema_tickets.security.JwtService;
import com.utp.sistema_tickets.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Registro e inicio de sesión.")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UsuarioService usuarioService;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
                          UsuarioService usuarioService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.usuarioService = usuarioService;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "Valida las credenciales y devuelve un JWT junto con el rol del usuario.")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().trim(), request.getPassword()));
        String token = jwtService.generateToken(authentication);
        String role = authentication.getAuthorities().stream()
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("El usuario autenticado no tiene rol"))
                .getAuthority()
                .replaceFirst("^ROLE_", "");
        var usuario = usuarioService.obtenerPorEmail(authentication.getName());

        return ResponseEntity.ok(new LoginResponse(
                token,
                "Bearer",
                authentication.getName(),
                usuario.getId(),
                usuario.getNombre(),
                role,
                jwtService.getExpiration(token)));
    }

    @PostMapping("/registro")
    @Operation(summary = "Registrar usuario", description = "Crea una cuenta pública con rol USUARIO; no permite elegir un rol administrativo.")
    public ResponseEntity<UsuarioResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UsuarioResponse.from(usuarioService.registrarUsuario(
                        request.getNombre(), request.getEmail(), request.getPassword())));
    }
}
