package com.utp.sistema_tickets.security;

import com.utp.sistema_tickets.model.Rol;
import com.utp.sistema_tickets.model.Usuario;
import com.utp.sistema_tickets.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class JpaUserDetailsService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public JpaUserDetailsService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(username.trim())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Usuario no encontrado: " + username));

        Rol rol = usuario.getRol();
        if (rol == null) {
            throw new IllegalStateException("El usuario no tiene un rol válido: " + usuario.getEmail());
        }
        if (usuario.getPasswordHash() == null || usuario.getPasswordHash().isBlank()) {
            throw new IllegalStateException("El usuario no tiene contraseña cifrada: " + usuario.getEmail());
        }

        return User.withUsername(usuario.getEmail())
                .password(usuario.getPasswordHash())
                .roles(rol.name())
                .build();
    }
}
