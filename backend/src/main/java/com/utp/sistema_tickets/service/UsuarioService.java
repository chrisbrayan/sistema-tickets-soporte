package com.utp.sistema_tickets.service;

import com.utp.sistema_tickets.exception.UsuarioValidationException;
import com.utp.sistema_tickets.model.Rol;
import com.utp.sistema_tickets.model.Usuario;
import com.utp.sistema_tickets.repository.UsuarioRepository;
import java.util.List;
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

    public Usuario obtenerPorEmail(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new UsuarioValidationException(
                        "Usuario autenticado no encontrado: " + email));
    }

    public List<Usuario> listarCatalogo(Rol rol) {
        return rol == null
                ? usuarioRepository.findAllByOrderByIdAsc()
                : usuarioRepository.findAllByRolOrderByIdAsc(rol);
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
