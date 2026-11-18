package com.utp.sistematickets.service;

import com.utp.sistematickets.exception.UsuarioValidationException;
import com.utp.sistematickets.model.Rol;
import com.utp.sistematickets.model.Usuario;
import com.utp.sistematickets.repository.UsuarioRepository;
import java.util.Locale;
import java.util.Objects;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
    }

    @Transactional
    public Usuario registrarUsuario(String nombre, String email, String password) {
        return crear(nombre, email, password, Rol.USUARIO);
    }

    @Transactional
    public Usuario crearAdministrado(String nombre, String email, String password, Rol rol) {
        return crear(nombre, email, password, rol);
    }

    private Usuario crear(String nombre, String email, String password, Rol rol) {
        String emailNormalizado = normalizarEmail(email);
        if (usuarioRepository.findByEmailIgnoreCase(emailNormalizado).isPresent()) {
            throw new UsuarioValidationException("Ya existe un usuario con el email: " + emailNormalizado);
        }
        if (rol == null) {
            throw new UsuarioValidationException("El rol es obligatorio");
        }

        Usuario usuario = new Usuario(
                null,
                nombre.trim(),
                emailNormalizado,
                passwordEncoder.encode(password),
                rol);
        return usuarioRepository.save(usuario);
    }

    private String normalizarEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new UsuarioValidationException("El email es obligatorio");
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
