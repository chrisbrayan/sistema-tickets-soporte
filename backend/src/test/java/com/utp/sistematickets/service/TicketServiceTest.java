package com.utp.sistematickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.utp.sistema_tickets.exception.TicketNotFoundException;
import com.utp.sistema_tickets.exception.TicketValidationException;
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

    @Test
    void actualizarConservaIdYFechaDeCreacion() {
        Ticket creado = service.crear(ticket("Título original", 1L, 1L));
        var fechaCreacion = creado.getFechaCreacion();

        Ticket datos = ticket("Título actualizado", 1L, 2L);
        datos.setPrioridad(PrioridadTicket.ALTA);
        Ticket actualizado = service.actualizar(creado.getId(), datos);

        assertThat(actualizado.getId()).isEqualTo(creado.getId());
        assertThat(actualizado.getTitulo()).isEqualTo("Título actualizado");
        assertThat(actualizado.getPrioridad()).isEqualTo(PrioridadTicket.ALTA);
        assertThat(actualizado.getFechaCreacion()).isEqualTo(fechaCreacion);
        assertThat(actualizado.getCategoria().getId()).isEqualTo(2L);
    }

    @Test
    void actualizarTicketCerradoNoPuedeModificarse() {
        Ticket creado = service.crear(ticket("Ticket cerrado", 1L, 1L));
        creado.setEstado(EstadoTicket.CERRADO);
        Ticket datosActualizados = ticket("Intento de reapertura", 1L, 1L);
        datosActualizados.setEstado(EstadoTicket.EN_PROGRESO);

        assertThatThrownBy(() -> service.actualizar(creado.getId(), datosActualizados))
                .isInstanceOf(TicketValidationException.class)
                .hasMessage("Un ticket cerrado no puede modificarse");
    }

    @Test
    void crearRechazaUnaReferenciaInexistente() {
        assertThatThrownBy(() -> service.crear(ticket("Referencia inválida", 1L, 99L)))
                .isInstanceOf(TicketValidationException.class)
                .hasMessage("Categoría no encontrada: 99");
    }

    @Test
    void crearRechazaTecnicoQueNoTieneRolTecnico() {
        Ticket ticket = ticket("Técnico inválido", 1L, 1L);
        ticket.setTecnicoAsignado(new Usuario(1L, "Ana", "ana@example.com", null));

        assertThatThrownBy(() -> service.crear(ticket))
                .isInstanceOf(TicketValidationException.class)
                .hasMessage("El usuario asignado no tiene rol TECNICO");
    }

    private Ticket ticket(String titulo, Long usuarioId, Long categoriaId) {
        Usuario usuario = new Usuario(usuarioId, "Usuario de prueba", "test@example.com", null);
        Categoria categoria = new Categoria(categoriaId, "Categoría de prueba", "Descripción");
        return new Ticket(titulo, "Descripción del ticket", PrioridadTicket.MEDIA, usuario, categoria);
    }
}
