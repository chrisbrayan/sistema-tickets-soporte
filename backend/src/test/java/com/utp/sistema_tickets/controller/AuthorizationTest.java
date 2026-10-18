package com.utp.sistema_tickets.controller;

import com.utp.sistema_tickets.model.Categoria;
import com.utp.sistema_tickets.model.PrioridadTicket;
import com.utp.sistema_tickets.model.Ticket;
import com.utp.sistema_tickets.model.Usuario;
import com.utp.sistema_tickets.repository.CategoriaRepository;
import com.utp.sistema_tickets.repository.TicketHistorialRepository;
import com.utp.sistema_tickets.repository.TicketRepository;
import com.utp.sistema_tickets.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthorizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketHistorialRepository historialRepository;

    private Long ticketDeOtroUsuarioId;

    @BeforeEach
    void setUp() {
        historialRepository.deleteAll();
        ticketRepository.deleteAll();
        categoriaRepository.deleteAll();

        Usuario luis = usuarioRepository.findByEmailIgnoreCase("luis.tecnico@example.com").orElseThrow();
        Categoria categoria = categoriaRepository.save(new Categoria(null, "Autorización", "Pruebas de acceso"));
        Ticket ticket = new Ticket("Ticket de Luis", "No debe verlo Ana", PrioridadTicket.MEDIA,
                luis, categoria);
        ticketDeOtroUsuarioId = ticketRepository.save(ticket).getId();
    }

    @Test
    void usuarioNoPuedeConsultarTicketDeOtroUsuario() throws Exception {
        String token = login("ana.usuario@example.com", "ana1234");

        mockMvc.perform(get("/api/tickets/{id}", ticketDeOtroUsuarioId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void tecnicoPuedeConsultarTicketDeOtroUsuario() throws Exception {
        String token = login("luis.tecnico@example.com", "tecnico1234");

        mockMvc.perform(get("/api/tickets/{id}", ticketDeOtroUsuarioId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void usuarioNoPuedeAdministrarCategorias() throws Exception {
        String token = login("ana.usuario@example.com", "ana1234");

        mockMvc.perform(get("/api/categorias")
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
