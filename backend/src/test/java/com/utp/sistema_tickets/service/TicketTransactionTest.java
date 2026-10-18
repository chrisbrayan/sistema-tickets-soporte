package com.utp.sistematickets.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(ticketHistorialRepository.findAll()).anySatisfy(historial ->
                assertThat(historial.getEstadoNuevo()).isEqualTo(EstadoTicket.CERRADO));
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
