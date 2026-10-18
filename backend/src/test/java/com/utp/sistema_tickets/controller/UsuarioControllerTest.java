package com.utp.sistema_tickets.controller;

import com.utp.sistema_tickets.model.Rol;
import com.utp.sistema_tickets.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registroPublicoCreaSiempreUnUsuario() throws Exception {
        String email = "registro." + System.nanoTime() + "@empresa.test";

        mockMvc.perform(post("/api/auth/registro")
                        .contentType("application/json")
                        .content("{\"nombre\":\"Nuevo Usuario\",\"email\":\""
                                + email + "\",\"password\":\"usuario123\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.rol").value("USUARIO"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        var usuario = usuarioRepository.findByEmailIgnoreCase(email).orElseThrow();
        assertThat(usuario.getRol()).isEqualTo(Rol.USUARIO);
        assertThat(passwordEncoder.matches("usuario123", usuario.getPasswordHash())).isTrue();
    }

    @Test
    void administradorPuedeCrearUnUsuarioConRolTecnico() throws Exception {
        String email = "tecnico." + System.nanoTime() + "@empresa.test";
        String token = login("maria.admin@example.com", "admin1234");

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"nombre\":\"Técnico Nuevo\",\"email\":\""
                                + email + "\",\"password\":\"tecnico123\",\"rol\":\"TECNICO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.rol").value("TECNICO"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        assertThat(usuarioRepository.findByEmailIgnoreCase(email).orElseThrow().getRol())
                .isEqualTo(Rol.TECNICO);
    }

    @Test
    void usuarioNormalNoPuedeCrearUsuariosAdministrados() throws Exception {
        String token = login("ana.usuario@example.com", "ana1234");

        mockMvc.perform(post("/api/usuarios")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{\"nombre\":\"No autorizado\",\"email\":\"no.autorizado@empresa.test\","
                                + "\"password\":\"usuario123\",\"rol\":\"ADMIN\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void registroConEmailDuplicadoResponde400() throws Exception {
        mockMvc.perform(post("/api/auth/registro")
                        .contentType("application/json")
                        .content("{\"nombre\":\"Duplicado\",\"email\":\"ana.usuario@example.com\","
                                + "\"password\":\"usuario123\"}"))
                .andExpect(status().isBadRequest());
    }

    private String login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceFirst(".*\\\"token\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }
}
