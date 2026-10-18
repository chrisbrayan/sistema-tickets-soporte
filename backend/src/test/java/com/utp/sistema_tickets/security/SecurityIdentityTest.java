package com.utp.sistematickets.security;

import com.utp.sistematickets.model.Rol;
import com.utp.sistematickets.model.Usuario;
import com.utp.sistematickets.repository.UsuarioRepository;
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
        assertUser("ana.usuario@example.com", "ana1234", Rol.USUARIO);
        assertUser("luis.tecnico@example.com", "tecnico1234", Rol.TECNICO);
        assertUser("maria.admin@example.com", "admin1234", Rol.ADMIN);
    }

    @Test
    void userDetailsCargaElRolPersistidoComoAutoridadSpring() {
        UserDetails tecnico = userDetailsService.loadUserByUsername("luis.tecnico@example.com");

        assertThat(tecnico.getUsername()).isEqualTo("luis.tecnico@example.com");
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
