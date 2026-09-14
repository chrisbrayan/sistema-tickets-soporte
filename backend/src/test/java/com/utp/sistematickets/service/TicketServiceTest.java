package com.utp.sistematickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.utp.sistema_tickets.exception.TicketNotFoundException;
import com.utp.sistema_tickets.model.Categoria;
import com.utp.sistema_tickets.model.EstadoTicket;
import com.utp.sistema_tickets.model.PrioridadTicket;
import com.utp.sistema_tickets.model.Ticket;
import com.utp.sistema_tickets.model.Usuario;
import com.utp.sistema_tickets.repository.CategoriaRepository;
import com.utp.sistema_tickets.repository.InMemoryCategoriaRepository;
import com.utp.sistema_tickets.repository.InMemoryTicketRepository;
import com.utp.sistema_tickets.repository.InMemoryUsuarioRepository;
import com.utp.sistema_tickets.repository.TicketRepository;
import com.utp.sistema_tickets.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TicketServiceTest {

    private TicketService service;

    @BeforeEach
    void setUp() {
        TicketRepository ticketRepository = new InMemoryTicketRepository();
        UsuarioRepository usuarioRepository = new InMemoryUsuarioRepository();
        CategoriaRepository categoriaRepository = new InMemoryCategoriaRepository();
        service = new TicketService(ticketRepository, usuarioRepository, categoriaRepository);
    }

    @Test
    void crearAsignaIdYValoresIniciales() {
        Ticket creado = service.crear(ticket("No funciona el correo", 1L, 1L));

        assertThat(creado.getId()).isEqualTo(1L);
        assertThat(creado.getEstado()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(creado.getSolicitante().getId()).isEqualTo(1L);
        assertThat(creado.getCategoria().getId()).isEqualTo(1L);
    }

    @Test
    void crearRecortaEspaciosDelTitulo() {
        Ticket creado = service.crear(ticket("  No funciona  ", 1L, 1L));

        assertThat(creado.getTitulo()).isEqualTo("No funciona");
    }

    @Test
    void obtenerUnIdInexistenteLanzaExcepcionDeDominio() {
        assertThatThrownBy(() -> service.obtenerPorId(99L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessage("Ticket no encontrado: 99");
    }

    @Test
    void eliminarUnIdInexistenteLanzaExcepcionDeDominio() {
        assertThatThrownBy(() -> service.eliminarPorId(99L))
                .isInstanceOf(TicketNotFoundException.class);
    }

    private Ticket ticket(String titulo, Long usuarioId, Long categoriaId) {
        Usuario usuario = new Usuario(usuarioId, "Usuario de prueba", "test@example.com", null);
        Categoria categoria = new Categoria(categoriaId, "Categoría de prueba", "Descripción");
        return new Ticket(titulo, "Descripción del ticket", PrioridadTicket.MEDIA, usuario, categoria);
    }
}