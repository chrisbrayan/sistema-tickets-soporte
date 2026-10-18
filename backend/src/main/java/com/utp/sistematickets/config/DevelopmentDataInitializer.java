package com.utp.sistematickets.config;

import com.utp.sistematickets.model.Rol;
import com.utp.sistematickets.model.Usuario;
import com.utp.sistematickets.repository.UsuarioRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DevelopmentDataInitializer {

    @Bean
    CommandLineRunner initializeDevelopmentUsers(UsuarioRepository usuarioRepository,
                                                  PasswordEncoder passwordEncoder) {
        return args -> {
            ensureUser(usuarioRepository, passwordEncoder,
                    "Ana Usuario", "ana.usuario@example.com", "ana1234", Rol.USUARIO);
            ensureUser(usuarioRepository, passwordEncoder,
                    "Luis Técnico", "luis.tecnico@example.com", "tecnico1234", Rol.TECNICO);
            ensureUser(usuarioRepository, passwordEncoder,
                    "María Administradora", "maria.admin@example.com", "admin1234", Rol.ADMIN);
        };
    }

    private void ensureUser(UsuarioRepository usuarioRepository,
                            PasswordEncoder passwordEncoder,
                            String nombre,
                            String email,
                            String password,
                            Rol rol) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElseGet(() ->
                new Usuario(null, nombre, email, passwordEncoder.encode(password), rol));

        if (usuario.getRol() == null) {
            throw new IllegalStateException("El usuario no tiene un rol válido: " + email);
        }
        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isBlank()) {
            usuario.setPasswordHash(passwordEncoder.encode(password));
        }
        usuarioRepository.save(usuario);
    }
}
