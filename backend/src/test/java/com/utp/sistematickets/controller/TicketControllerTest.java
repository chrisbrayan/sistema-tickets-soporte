package com.utp.sistematickets.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.utp.sistema_tickets.exception.TicketNotFoundException;
import com.utp.sistema_tickets.model.Categoria;
import com.utp.sistema_tickets.model.PrioridadTicket;
import com.utp.sistema_tickets.model.Ticket;
import com.utp.sistema_tickets.model.Usuario;
import com.utp.sistema_tickets.service.TicketService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@ExtendWith(MockitoExtension.class)
class TicketControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TicketService ticketService;

    @InjectMocks
    private TicketController ticketController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
        mockMvc = MockMvcBuilders.standaloneSetup(ticketController)
                .setControllerAdvice(new ApiExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    void postCreaTicketYResponde201() throws Exception {
        Ticket creado = ticket(10L, "Correo sin acceso");
        when(ticketService.crear(any(Ticket.class))).thenReturn(creado);

        mockMvc.perform(post("/api/tickets")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(requestBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.titulo").value("Correo sin acceso"))
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.usuarioId").value(1))
                .andExpect(jsonPath("$.categoriaId").value(1));

        verify(ticketService).crear(any(Ticket.class));
    }

    @Test
    void getTicketInexistenteResponde404() throws Exception {
        when(ticketService.obtenerPorId(99L)).thenThrow(new TicketNotFoundException(99L));

        mockMvc.perform(get("/api/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("ticket no encontrado: 99"))
                .andExpect(jsonPath("$.path").value("/api/tickets/99"));
    }

    @Test
    void postConDatosInvalidosResponde400() throws Exception {
        String body = """
                {
                  "titulo": "",
                  "descripcion": "Descripción",
                  "prioridad": "MEDIA",
                  "usuarioId": 1,
                  "categoriaId": 1
                }
                """;

        mockMvc.perform(post("/api/tickets")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("titulo: El título es obligatorio"));
    }

    @Test
    void getListaTicketsResponde200() throws Exception {
        when(ticketService.listar()).thenReturn(List.of(ticket(10L, "Primer ticket")));

        mockMvc.perform(get("/api/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].titulo").value("Primer ticket"));
    }

    private Ticket ticket(Long id, String titulo) {
        Usuario usuario = new Usuario(1L, "Ana", "ana@example.com", null);
        Categoria categoria = new Categoria(1L, "Acceso", "Problemas de acceso");
        Ticket ticket = new Ticket(titulo, "Descripción", PrioridadTicket.MEDIA, usuario, categoria);
        ticket.setId(id);
        return ticket;
    }

    private Object requestBody() {
        return new Object() {
            public final String titulo = "No puedo acceder al correo";
            public final String descripcion = "El sistema rechaza mis credenciales.";
            public final String prioridad = "MEDIA";
            public final Long usuarioId = 1L;
            public final Long categoriaId = 1L;
        };
    }
}