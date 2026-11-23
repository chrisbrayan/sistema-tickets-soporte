package com.utp.sistema_tickets.security;

import com.utp.sistema_tickets.model.Rol;
import com.utp.sistema_tickets.model.Usuario;
import com.utp.sistema_tickets.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SecurityIdentityTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private UserDetailsService userDetailsService;

    @Test
    void usuariosInicialesUsanPasswordBcryptYRolesValidos() {
        assertUser("usuario@soporte.com", "usuario123", Rol.USUARIO);
        assertUser("tecnico@soporte.com", "tecnico123", Rol.TECNICO);
        assertUser("admin@soporte.com", "admin123", Rol.ADMIN);
    }

    @Test
    void userDetailsCargaElRolPersistidoComoAutoridadSpring() {
        UserDetails tecnico = userDetailsService.loadUserByUsername("tecnico@soporte.com");

        assertThat(tecnico.getUsername()).isEqualTo("tecnico@soporte.com");
        assertThat(tecnico.getAuthorities())
                .extracting(authority -> authority.getAuthority())
                .containsExactly("ROLE_TECNICO");
    }

    private void assertUser(String email, String rawPassword, Rol expectedRole) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email).orElseThrow();

        assertThat(usuario.getRol()).isEqualTo(expectedRole);
        assertThat(usuario.getPasswordHash())
                .isNotBlank()
                .startsWith("$2")
                .doesNotContain(rawPassword);
        assertThat(passwordEncoder.matches(rawPassword, usuario.getPasswordHash())).isTrue();
    }
}
