package com.utp.sistema_tickets.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void loginConCredencialesValidasDevuelveJwt() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "ana.usuario@example.com",
                                  "password": "ana1234"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.subject").value("ana.usuario@example.com"))
                .andExpect(jsonPath("$.role").value("USUARIO"))
                .andExpect(jsonPath("$.expiresAt").isString());
    }

    @Test
    void loginConCredencialesInvalidasDevuelve401() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("""
                                {
                                  "email": "ana.usuario@example.com",
                                  "password": "incorrecta"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rutaProtegidaSinTokenDevuelve401() throws Exception {
        mockMvc.perform(delete("/api/tickets/999999"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTicketsSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getTicketsConTokenValidoDevuelve200() throws Exception {
        String token = login("luis.tecnico@example.com", "tecnico1234");

        mockMvc.perform(get("/api/tickets")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void rutaProtegidaConRolInsuficienteDevuelve403() throws Exception {
        String token = login("ana.usuario@example.com", "ana1234");

        mockMvc.perform(delete("/api/tickets/999999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
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
