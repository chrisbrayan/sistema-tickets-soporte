package com.utp.sistema_tickets.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.utp.sistema_tickets.model.Categoria;
import com.utp.sistema_tickets.model.EstadoTicket;
import com.utp.sistema_tickets.model.PrioridadTicket;
import com.utp.sistema_tickets.model.Rol;
import com.utp.sistema_tickets.model.Ticket;
import com.utp.sistema_tickets.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private Usuario usuario;
    private Usuario tecnico;
    private Categoria categoria;
    private Categoria hardware;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeEach
    void setUp() {
        ticketRepository.deleteAll();
        usuarioRepository.deleteAll();
        categoriaRepository.deleteAll();

        usuario = usuarioRepository.save(new Usuario(
                null, "Ana Usuario", "ana.repository@example.com",
                passwordEncoder.encode("test"), Rol.USUARIO));
        tecnico = usuarioRepository.save(new Usuario(
                null, "Luis Técnico", "luis.repository@example.com",
                passwordEncoder.encode("test"), Rol.TECNICO));
        categoria = categoriaRepository.save(new Categoria(
                null, "Acceso", "Problemas de acceso"));
        hardware = categoriaRepository.save(new Categoria(
                null, "Hardware", "Problemas de equipos"));
    }

    @Test
    void savePersisteYFindByIdRecuperaElTicket() {
        Ticket ticket = ticketRepository.saveAndFlush(ticket("No funciona el correo"));
        entityManager.clear();

        Ticket recuperado = ticketRepository.findById(ticket.getId()).orElseThrow();

        assertThat(recuperado.getId()).isNotNull();
        assertThat(recuperado.getTitulo()).isEqualTo("No funciona el correo");
        assertThat(recuperado.getEstado().name()).isEqualTo("ABIERTO");
        assertThat(recuperado.getSolicitante().getId()).isEqualTo(usuario.getId());
        assertThat(recuperado.getCategoria().getId()).isEqualTo(categoria.getId());
    }

    @Test
    void saveActualizaUnTicketPersistido() {
        Ticket ticket = ticketRepository.saveAndFlush(ticket("Título original"));

        ticket.setTitulo("Título actualizado");
        ticketRepository.saveAndFlush(ticket);

        assertThat(ticketRepository.findById(ticket.getId()))
                .get()
                .extracting(Ticket::getTitulo)
                .isEqualTo("Título actualizado");
    }

    @Test
    void findAllYDeleteByIdTrabajanSobreLaBaseDeDatos() {
        Ticket primero = ticketRepository.save(ticket("Primero"));
        Ticket segundo = ticketRepository.save(ticket("Segundo"));
        ticketRepository.flush();

        assertThat(ticketRepository.findAll()).extracting(Ticket::getTitulo)
                .containsExactlyInAnyOrder("Primero", "Segundo");

        ticketRepository.deleteById(primero.getId());
        ticketRepository.flush();

        assertThat(ticketRepository.findById(primero.getId())).isEmpty();
        assertThat(ticketRepository.findById(segundo.getId())).isPresent();
    }

    @Test
    void findByIdInexistenteDevuelveOptionalVacio() {
        assertThat(ticketRepository.findById(999999L)).isEmpty();
    }

    @Test
    void buscarPorEstadoYCategoriaUsaLosFiltrosDelDominio() {
        ticketRepository.save(ticket("Acceso pendiente"));
        Ticket otro = ticket("Hardware pendiente");
        otro.setCategoria(hardware);
        ticketRepository.save(otro);
        Ticket cerrado = ticket("Acceso cerrado");
        cerrado.setEstado(EstadoTicket.CERRADO);
        ticketRepository.save(cerrado);
        ticketRepository.flush();

        List<Ticket> resultado = ticketRepository.buscarPorEstadoYCategoria(
                EstadoTicket.ABIERTO, categoria.getId());

        assertThat(resultado).extracting(Ticket::getTitulo)
                .containsExactly("Acceso pendiente");
    }

    @Test
    void buscarAsignadosNoCerradosExcluyeTicketsCerrados() {
        Ticket enProgreso = ticket("Atender incidencia");
        enProgreso.setTecnicoAsignado(tecnico);
        enProgreso.setEstado(EstadoTicket.EN_PROGRESO);
        ticketRepository.save(enProgreso);

        Ticket cerrado = ticket("Incidencia cerrada");
        cerrado.setTecnicoAsignado(tecnico);
        cerrado.setEstado(EstadoTicket.CERRADO);
        ticketRepository.save(cerrado);
        ticketRepository.flush();

        assertThat(ticketRepository.buscarAsignadosNoCerrados(tecnico.getId()))
                .extracting(Ticket::getTitulo)
                .containsExactly("Atender incidencia");
    }

    @Test
    void buscarConFiltrosAceptaCriteriosOpcionales() {
        Ticket urgente = ticket("Incidente urgente");
        urgente.setPrioridad(PrioridadTicket.URGENTE);
        ticketRepository.save(urgente);
        ticketRepository.save(ticket("Incidente normal"));
        ticketRepository.flush();

        List<Ticket> resultado = ticketRepository.buscarConFiltros(
                EstadoTicket.ABIERTO, PrioridadTicket.URGENTE, categoria.getId(), null, null);

        assertThat(resultado).extracting(Ticket::getTitulo)
                .containsExactly("Incidente urgente");
    }

    @Test
    void resumenPorEstadoAgrupaLosTickets() {
        ticketRepository.save(ticket("Abierto 1"));
        ticketRepository.save(ticket("Abierto 2"));
        Ticket cerrado = ticket("Cerrado 1");
        cerrado.setEstado(EstadoTicket.CERRADO);
        ticketRepository.save(cerrado);
        ticketRepository.flush();

        assertThat(ticketRepository.resumenPorEstado())
                .anySatisfy(resumen -> {
                    if (resumen.estado() == EstadoTicket.ABIERTO) {
                        assertThat(resumen.total()).isEqualTo(2L);
                    }
                })
                .anySatisfy(resumen -> {
                    if (resumen.estado() == EstadoTicket.CERRADO) {
                        assertThat(resumen.total()).isEqualTo(1L);
                    }
                });
    }

    private Ticket ticket(String titulo) {
        return new Ticket(titulo, "Descripción del ticket", PrioridadTicket.MEDIA, usuario, categoria);
    }
}
