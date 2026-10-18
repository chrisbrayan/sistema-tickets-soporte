package com.utp.sistematickets.service;

import com.utp.sistematickets.exception.TicketNotFoundException;
import com.utp.sistematickets.exception.TicketValidationException;
import com.utp.sistematickets.dto.TicketEstadoResumen;
import com.utp.sistematickets.model.Categoria;
import com.utp.sistematickets.model.EstadoTicket;
import com.utp.sistematickets.model.PrioridadTicket;
import com.utp.sistematickets.model.Rol;
import com.utp.sistematickets.model.Ticket;
import com.utp.sistematickets.model.TicketHistorial;
import com.utp.sistematickets.model.Usuario;
import java.time.LocalDateTime;
import com.utp.sistematickets.repository.CategoriaRepository;
import com.utp.sistematickets.repository.TicketHistorialRepository;
import com.utp.sistematickets.repository.TicketRepository;
import com.utp.sistematickets.repository.UsuarioRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;
    private final TicketHistorialRepository ticketHistorialRepository;

    public TicketService(TicketRepository ticketRepository,
                         UsuarioRepository usuarioRepository,
                         CategoriaRepository categoriaRepository,
                         TicketHistorialRepository ticketHistorialRepository) {
        this.ticketRepository = Objects.requireNonNull(ticketRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.categoriaRepository = Objects.requireNonNull(categoriaRepository);
        this.ticketHistorialRepository = Objects.requireNonNull(ticketHistorialRepository);
    }

    @Transactional
    public Ticket crear(Ticket ticket) {
        validarTicket(ticket);
        if (ticket.getId() != null) {
            throw new TicketValidationException("Un ticket nuevo no puede tener ID");
        }

        normalizarCamposTexto(ticket);
        normalizarReferencias(ticket);
        Ticket creado = ticketRepository.save(ticket);
        registrarHistorial(creado, null, creado.getEstado(), creado.getSolicitante(), "Ticket creado");
        return creado;
    }

    public List<Ticket> listar() {
        return ticketRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Ticket> listarVisibles(String email, boolean puedeVerTodos) {
        return puedeVerTodos
                ? ticketRepository.findAll()
                : ticketRepository.findBySolicitanteEmailIgnoreCaseOrderByFechaCreacionDesc(email);
    }

    public List<Ticket> buscarPorEstadoYCategoria(EstadoTicket estado, Long categoriaId) {
        return ticketRepository.buscarPorEstadoYCategoria(estado, categoriaId);
    }

    public List<Ticket> buscarAsignadosNoCerrados(Long tecnicoId) {
        return ticketRepository.buscarAsignadosNoCerrados(tecnicoId);
    }

    public List<Ticket> buscarConFiltros(EstadoTicket estado,
                                         PrioridadTicket prioridad,
                                         Long categoriaId,
                                         LocalDateTime desde,
                                         LocalDateTime hasta) {
        return ticketRepository.buscarConFiltros(estado, prioridad, categoriaId, desde, hasta);
    }

    public List<TicketEstadoResumen> resumenPorEstado() {
        return ticketRepository.resumenPorEstado();
    }

    public Ticket obtenerPorId(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Ticket obtenerVisible(Long id, String email, boolean puedeVerTodos) {
        if (puedeVerTodos) {
            return obtenerPorId(id);
        }
        return ticketRepository.findByIdAndSolicitanteEmailIgnoreCase(id, email)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Transactional
    public Ticket actualizar(Long id, Ticket datosActualizados) {
        Ticket actual = obtenerPorId(id);
        validarEstadoActualizable(actual);
        EstadoTicket estadoAnterior = actual.getEstado();
        validarTicket(datosActualizados);
        normalizarReferencias(datosActualizados);

        datosActualizados.setId(id);
        datosActualizados.setFechaCreacion(actual.getFechaCreacion());
        if (datosActualizados.getEstado() == null) {
            datosActualizados.setEstado(actual.getEstado());
        }

        Ticket actualizado = ticketRepository.save(datosActualizados);
        if (!Objects.equals(estadoAnterior, actualizado.getEstado())) {
            registrarHistorial(actualizado, estadoAnterior, actualizado.getEstado(),
                    actualizado.getSolicitante(), "Estado actualizado");
        }
        return actualizado;
    }

    @Transactional
    public Ticket asignarTecnico(Long ticketId, Long tecnicoId,
                                 Long usuarioEjecutorId, String comentario) {
        Ticket ticket = obtenerPorId(ticketId);
        validarEstadoActualizable(ticket);

        Usuario tecnico = usuarioRepository.findById(tecnicoId)
                .orElseThrow(() -> new TicketValidationException(
                        "Técnico asignado no encontrado: " + tecnicoId));
        if (tecnico.getRol() != Rol.TECNICO) {
            throw new TicketValidationException("El usuario asignado no tiene rol TECNICO");
        }

        Usuario usuarioEjecutor = obtenerUsuarioEjecutor(usuarioEjecutorId);
        ticket.setTecnicoAsignado(tecnico);
        Ticket actualizado = ticketRepository.save(ticket);
        registrarHistorial(actualizado, actualizado.getEstado(), actualizado.getEstado(),
                usuarioEjecutor, comentarioNormalizado(comentario, "Técnico asignado"));
        return actualizado;
    }

    @Transactional
    public Ticket cambiarEstado(Long ticketId, EstadoTicket estadoNuevo,
                                Long usuarioEjecutorId, String comentario) {
        if (estadoNuevo == null) {
            throw new TicketValidationException("El estado nuevo es obligatorio");
        }

        Ticket ticket = obtenerPorId(ticketId);
        validarEstadoActualizable(ticket);
        Usuario usuarioEjecutor = obtenerUsuarioEjecutor(usuarioEjecutorId);
        EstadoTicket estadoAnterior = ticket.getEstado();
        ticket.setEstado(estadoNuevo);
        Ticket actualizado = ticketRepository.save(ticket);
        registrarHistorial(actualizado, estadoAnterior, estadoNuevo, usuarioEjecutor,
                comentarioNormalizado(comentario, "Estado actualizado"));
        return actualizado;
    }

    @Transactional
    public Ticket cerrar(Long ticketId, Long usuarioEjecutorId, String comentario) {
        return cambiarEstado(ticketId, EstadoTicket.CERRADO, usuarioEjecutorId, comentario);
    }

    public void eliminarPorId(Long id) {
        obtenerPorId(id);
        ticketRepository.deleteById(id);
    }

    private void validarTicket(Ticket ticket) {
        if (ticket == null) {
            throw new TicketValidationException("El ticket es obligatorio");
        }
        if (ticket.getTitulo() == null || ticket.getTitulo().isBlank()) {
            throw new TicketValidationException("El título del ticket es obligatorio");
        }
        if (ticket.getDescripcion() == null || ticket.getDescripcion().isBlank()) {
            throw new TicketValidationException("La descripción del ticket es obligatoria");
        }
        if (ticket.getPrioridad() == null) {
            throw new TicketValidationException("La prioridad del ticket es obligatoria");
        }
        if (ticket.getSolicitante() == null || ticket.getSolicitante().getId() == null) {
            throw new TicketValidationException("El usuario solicitante es obligatorio");
        }
        if (ticket.getCategoria() == null || ticket.getCategoria().getId() == null) {
            throw new TicketValidationException("La categoría es obligatoria");
        }
    }

    private void normalizarCamposTexto(Ticket ticket) {
        ticket.setTitulo(ticket.getTitulo().trim());
    }

    private void registrarHistorial(Ticket ticket, EstadoTicket estadoAnterior,
                                    EstadoTicket estadoNuevo, Usuario usuarioEjecutor,
                                    String comentario) {
        TicketHistorial historial = new TicketHistorial(
                usuarioEjecutor, estadoAnterior, estadoNuevo, comentario);
        historial.setTicket(ticket);
        ticketHistorialRepository.save(historial);
    }

    private Usuario obtenerUsuarioEjecutor(Long usuarioEjecutorId) {
        if (usuarioEjecutorId == null) {
            throw new TicketValidationException("El usuario ejecutor es obligatorio");
        }
        return usuarioRepository.findById(usuarioEjecutorId)
                .orElseThrow(() -> new TicketValidationException(
                        "Usuario ejecutor no encontrado: " + usuarioEjecutorId));
    }

    private String comentarioNormalizado(String comentario, String valorPorDefecto) {
        return comentario == null || comentario.isBlank() ? valorPorDefecto : comentario.trim();
    }

    private void validarEstadoActualizable(Ticket ticket) {
        if (ticket.getEstado() == EstadoTicket.CERRADO) {
            throw new TicketValidationException("Un ticket cerrado no puede modificarse");
        }
    }

    private void normalizarReferencias(Ticket ticket) {
        Usuario solicitante = usuarioRepository.findById(ticket.getSolicitante().getId())
                .orElseThrow(() -> new TicketValidationException(
                        "Usuario solicitante no encontrado: " + ticket.getSolicitante().getId()));
        Categoria categoria = categoriaRepository.findById(ticket.getCategoria().getId())
                .orElseThrow(() -> new TicketValidationException(
                        "Categoría no encontrada: " + ticket.getCategoria().getId()));

        ticket.setSolicitante(solicitante);
        ticket.setCategoria(categoria);

        if (ticket.getTecnicoAsignado() != null) {
            ticket.setTecnicoAsignado(normalizarTecnico(ticket.getTecnicoAsignado()));
        }
    }

    private Usuario normalizarTecnico(Usuario tecnicoSolicitado) {
        if (tecnicoSolicitado.getId() == null) {
            throw new TicketValidationException("El técnico asignado debe tener ID");
        }

        Usuario tecnico = usuarioRepository.findById(tecnicoSolicitado.getId())
                .orElseThrow(() -> new TicketValidationException(
                        "Técnico asignado no encontrado: " + tecnicoSolicitado.getId()));
        if (tecnico.getRol() != Rol.TECNICO) {
            throw new TicketValidationException("El usuario asignado no tiene rol TECNICO");
        }
        return tecnico;
    }
}
