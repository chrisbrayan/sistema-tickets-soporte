package com.utp.sistematickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.utp.sistematickets.exception.TicketNotFoundException;
import com.utp.sistematickets.exception.TicketValidationException;
import com.utp.sistematickets.model.Categoria;
import com.utp.sistematickets.model.EstadoTicket;
import com.utp.sistematickets.model.PrioridadTicket;
import com.utp.sistematickets.model.Rol;
import com.utp.sistematickets.model.Ticket;
import com.utp.sistematickets.model.Usuario;
import com.utp.sistematickets.repository.CategoriaRepository;
import com.utp.sistematickets.repository.TicketHistorialRepository;
import com.utp.sistematickets.repository.TicketRepository;
import com.utp.sistematickets.repository.UsuarioRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TicketServiceTest {

    private TicketService service;
    private Map<Long, Ticket> persistedTickets;
    private AtomicLong ticketIds;

    @BeforeEach
    void setUp() {
        TicketRepository ticketRepository = mock(TicketRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        CategoriaRepository categoriaRepository = mock(CategoriaRepository.class);
        TicketHistorialRepository ticketHistorialRepository = mock(TicketHistorialRepository.class);
        persistedTickets = new HashMap<>();
        ticketIds = new AtomicLong();

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            if (ticket.getId() == null) {
                ticket.setId(ticketIds.incrementAndGet());
            }
            persistedTickets.put(ticket.getId(), ticket);
            return ticket;
        });
        when(ticketRepository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(persistedTickets.get(invocation.getArgument(0))));
        doAnswer(invocation -> {
            persistedTickets.remove(invocation.getArgument(0));
            return null;
        }).when(ticketRepository).deleteById(anyLong());

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(new Usuario(1L, "Ana Usuario", "ana@example.com", Rol.USUARIO)));
        when(usuarioRepository.findById(2L))
                .thenReturn(Optional.of(new Usuario(2L, "Luis Técnico", "luis@example.com", Rol.TECNICO)));
        when(usuarioRepository.findById(3L))
                .thenReturn(Optional.of(new Usuario(3L, "María Admin", "maria@example.com", Rol.ADMIN)));
        when(categoriaRepository.findById(1L))
                .thenReturn(Optional.of(new Categoria(1L, "Acceso", "Problemas de acceso")));
        when(categoriaRepository.findById(2L))
                .thenReturn(Optional.of(new Categoria(2L, "Hardware", "Problemas de equipos")));
        service = new TicketService(ticketRepository, usuarioRepository, categoriaRepository,
                ticketHistorialRepository);
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
