package com.utp.sistema_tickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.utp.sistema_tickets.dto.TicketHistorialResponse;
import com.utp.sistema_tickets.model.Categoria;
import com.utp.sistema_tickets.model.EstadoTicket;
import com.utp.sistema_tickets.model.PrioridadTicket;
import com.utp.sistema_tickets.model.Rol;
import com.utp.sistema_tickets.model.Ticket;
import com.utp.sistema_tickets.model.TicketHistorial;
import com.utp.sistema_tickets.model.Usuario;
import com.utp.sistema_tickets.repository.CategoriaRepository;
import com.utp.sistema_tickets.repository.TicketHistorialRepository;
import com.utp.sistema_tickets.repository.TicketRepository;
import com.utp.sistema_tickets.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.TransactionSystemException;

@SpringBootTest
@ActiveProfiles("test")
class TicketTransactionTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TicketHistorialRepository ticketHistorialRepository;

    private Usuario solicitante;
    private Usuario tecnico;
    private Usuario admin;
    private Categoria categoria;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        ticketHistorialRepository.deleteAll();
        ticketRepository.deleteAll();
        usuarioRepository.deleteAll();
        categoriaRepository.deleteAll();

        solicitante = usuarioRepository.saveAndFlush(
                new Usuario(null, "Ana", "ana.transaction@example.com",
                        passwordEncoder.encode("test"), Rol.USUARIO));
        tecnico = usuarioRepository.saveAndFlush(
                new Usuario(null, "Luis", "luis.transaction@example.com",
                        passwordEncoder.encode("test"), Rol.TECNICO));
        admin = usuarioRepository.saveAndFlush(
                new Usuario(null, "María", "maria.transaction@example.com",
                        passwordEncoder.encode("test"), Rol.ADMIN));
        categoria = categoriaRepository.saveAndFlush(
                new Categoria(null, "Acceso", "Problemas de acceso"));
    }

    @Test
    void crearRegistraElEventoInicialEnLaMismaOperacion() {
        Ticket creado = ticketService.crear(ticket("No funciona el correo"));

        assertThat(ticketHistorialRepository.count()).isEqualTo(1L);
        var historial = ticketHistorialRepository.findAll().get(0);
        assertThat(historial.getTicket().getId()).isEqualTo(creado.getId());
        assertThat(historial.getEstadoAnterior()).isNull();
        assertThat(historial.getEstadoNuevo()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(historial.getUsuarioEjecutor().getId()).isEqualTo(solicitante.getId());
    }

    @Test
    void asignarTecnicoActualizaTicketYRegistraHistorial() {
        Ticket creado = ticketService.crear(ticket("Asignar técnico"));

        Ticket actualizado = ticketService.asignarTecnico(
                creado.getId(), tecnico.getId(), admin.getId(), "Asignación inicial");

        assertThat(actualizado.getTecnicoAsignado().getId()).isEqualTo(tecnico.getId());
        assertThat(ticketHistorialRepository.count()).isEqualTo(2L);
        assertThat(ticketHistorialRepository.findAll()).anySatisfy(historial ->
                assertThat(historial.getComentario()).isEqualTo("Asignación inicial"));
    }

    @Test
    void elHistorialDeAsignacionMuestraAlTecnicoAsignado() {
        Ticket creado = ticketService.crear(ticket("Historial de asignación"));
        ticketService.asignarTecnico(creado.getId(), tecnico.getId(), admin.getId(), null);

        List<TicketHistorialResponse> historial = ticketService.historialVisible(
                creado.getId(), admin.getEmail(), true);

        assertThat(historial).anySatisfy(evento -> {
            assertThat(evento.estadoAnterior()).isEqualTo(EstadoTicket.ABIERTO);
            assertThat(evento.texto()).isEqualTo("Asignado a " + tecnico.getNombre());
            assertThat(evento.tecnicoNombre()).isEqualTo(tecnico.getNombre());
        });
    }

    @Test
    void alReasignarSeConservaElTecnicoDeLasAsignacionesAnteriores() {
        Ticket creado = ticketService.crear(ticket("Reasignar técnico"));
        ticketService.asignarTecnico(creado.getId(), tecnico.getId(), admin.getId(), null);
        for (TicketHistorial historial : ticketHistorialRepository.findAll()) {
            if (historial.getEstadoAnterior() == EstadoTicket.ABIERTO
                    && historial.getEstadoNuevo() == EstadoTicket.ABIERTO) {
                historial.setTecnicoAsignado(null);
                ticketHistorialRepository.save(historial);
            }
        }
        ticketHistorialRepository.flush();

        Usuario tecnico2 = usuarioRepository.saveAndFlush(
                new Usuario(null, "Elena", "elena.transaction@example.com",
                        passwordEncoder.encode("test"), Rol.TECNICO));
        ticketService.asignarTecnico(creado.getId(), tecnico2.getId(), admin.getId(), null);

        List<TicketHistorialResponse> historial = ticketService.historialVisible(
                creado.getId(), admin.getEmail(), true);

        assertThat(historial)
                .filteredOn(evento -> evento.estadoAnterior() == EstadoTicket.ABIERTO
                        && evento.estadoNuevo() == EstadoTicket.ABIERTO)
                .extracting(TicketHistorialResponse::tecnicoNombre)
                .containsExactly(tecnico.getNombre(), tecnico2.getNombre());
    }

    @Test
    void cambiarEstadoRegistraLaTransicion() {
        Ticket creado = ticketService.crear(ticket("Cambiar estado"));

        Ticket actualizado = ticketService.cambiarEstado(
                creado.getId(), EstadoTicket.EN_PROGRESO, tecnico.getId(), "Inicio de atención");

        assertThat(actualizado.getEstado()).isEqualTo(EstadoTicket.EN_PROGRESO);
        assertThat(ticketHistorialRepository.findAll()).anySatisfy(historial -> {
            assertThat(historial.getEstadoAnterior()).isEqualTo(EstadoTicket.ABIERTO);
            assertThat(historial.getEstadoNuevo()).isEqualTo(EstadoTicket.EN_PROGRESO);
        });
    }

    @Test
    void cerrarRegistraElCierreDelTicket() {
        Ticket creado = ticketService.crear(ticket("Cerrar ticket"));

        Ticket cerrado = ticketService.cerrar(creado.getId(), tecnico.getId(), "Atención finalizada");

        assertThat(cerrado.getEstado()).isEqualTo(EstadoTicket.CERRADO);
        assertThat(ticketHistorialRepository.findAll()).anySatisfy(historial -> {
            assertThat(historial.getEstadoNuevo()).isEqualTo(EstadoTicket.CERRADO);
            assertThat(historial.getComentario()).isEqualTo("Atención finalizada");
        });
    }

    @Test
    void actualizarConservaTecnicoEstadoEHistorial() {
        Ticket creado = ticketService.crear(ticket("Editar datos"));
        Ticket asignado = ticketService.asignarTecnico(
                creado.getId(), tecnico.getId(), admin.getId(), "Asignación inicial");
        Ticket enProgreso = ticketService.cambiarEstado(
                asignado.getId(), EstadoTicket.EN_PROGRESO, tecnico.getId(), "Inicio de atención");
        long historialAntes = ticketHistorialRepository.count();

        Ticket datos = new Ticket("Título editado", "Descripción editada",
                PrioridadTicket.ALTA, solicitante, categoria);
        datos.setEstado(null);
        Ticket actualizado = ticketService.actualizar(enProgreso.getId(), datos);

        assertThat(actualizado.getTitulo()).isEqualTo("Título editado");
        assertThat(actualizado.getDescripcion()).isEqualTo("Descripción editada");
        assertThat(actualizado.getPrioridad()).isEqualTo(PrioridadTicket.ALTA);
        assertThat(actualizado.getTecnicoAsignado().getId()).isEqualTo(tecnico.getId());
        assertThat(actualizado.getEstado()).isEqualTo(EstadoTicket.EN_PROGRESO);
        assertThat(ticketHistorialRepository.count()).isEqualTo(historialAntes);
    }

    @Test
    void siFallaElHistorialSeRevierteLaAsignacionDelTicket() {
        Ticket creado = ticketService.crear(ticket("Rollback de asignación"));
        String comentarioDemasiadoLargo = "x".repeat(501);

        assertThatThrownBy(() -> ticketService.asignarTecnico(
                creado.getId(), tecnico.getId(), admin.getId(), comentarioDemasiadoLargo))
                .isInstanceOfAny(TransactionSystemException.class, RuntimeException.class);

        Optional<Ticket> recuperado = ticketRepository.findById(creado.getId());
        assertThat(recuperado).isPresent();
        assertThat(recuperado.orElseThrow().getTecnicoAsignado()).isNull();
        assertThat(ticketHistorialRepository.count()).isEqualTo(1L);
    }

    private Ticket ticket(String titulo) {
        return new Ticket(titulo, "Descripción", PrioridadTicket.MEDIA, solicitante, categoria);
    }
}
