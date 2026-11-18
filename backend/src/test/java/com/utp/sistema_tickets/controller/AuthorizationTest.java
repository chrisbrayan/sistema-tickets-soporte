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

    private Long ticketAsignadoACarlosId;
    private Long ticketAsignadoAMartaId;

    @BeforeEach
    void setUp() {
        historialRepository.deleteAll();
        ticketRepository.deleteAll();
        categoriaRepository.deleteAll();

        Usuario carlos = usuarioRepository.findByEmailIgnoreCase("tecnico@soporte.com").orElseThrow();
        Usuario marta = usuarioRepository.findByEmailIgnoreCase("tecnico2@soporte.com").orElseThrow();
        Usuario sofia = usuarioRepository.findByEmailIgnoreCase("sofia@soporte.com").orElseThrow();
        Categoria categoria = categoriaRepository.save(new Categoria(null, "Autorización", "Pruebas de acceso"));

        Ticket asignadoACarlos = new Ticket("Ticket asignado a Carlos", "Detalle visible para Carlos",
                PrioridadTicket.MEDIA, sofia, categoria);
        asignadoACarlos.setTecnicoAsignado(carlos);
        ticketAsignadoACarlosId = ticketRepository.save(asignadoACarlos).getId();

        Ticket asignadoAMarta = new Ticket("Ticket asignado a Marta", "No debe verlo Carlos",
                PrioridadTicket.MEDIA, sofia, categoria);
        asignadoAMarta.setTecnicoAsignado(marta);
        ticketAsignadoAMartaId = ticketRepository.save(asignadoAMarta).getId();
    }

    @Test
    void usuarioNoPuedeConsultarTicketDeOtroUsuario() throws Exception {
        String token = login("usuario@soporte.com", "usuario123");

        mockMvc.perform(get("/api/tickets/{id}", ticketAsignadoACarlosId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void tecnicoPuedeConsultarTicketAsignado() throws Exception {
        String token = login("tecnico@soporte.com", "tecnico123");

        mockMvc.perform(get("/api/tickets/{id}", ticketAsignadoACarlosId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void tecnicoNoPuedeConsultarTicketNoAsignado() throws Exception {
        String token = login("tecnico@soporte.com", "tecnico123");

        mockMvc.perform(get("/api/tickets/{id}", ticketAsignadoAMartaId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void tecnicoNoPuedeCrearTickets() throws Exception {
        String token = login("tecnico@soporte.com", "tecnico123");

        mockMvc.perform(post("/api/tickets")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                                {
                                  "titulo": "Intento de técnico",
                                  "descripcion": "No debe crearse",
                                  "prioridad": "BAJA",
                                  "usuarioId": 2,
                                  "categoriaId": 1
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void usuarioPuedeConsultarCatalogoCategorias() throws Exception {
        String token = login("usuario@soporte.com", "usuario123");

        mockMvc.perform(get("/api/categorias")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
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
