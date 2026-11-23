package com.utp.sistema_tickets.service;

import com.utp.sistema_tickets.exception.TicketNotFoundException;
import com.utp.sistema_tickets.exception.TicketValidationException;
import com.utp.sistema_tickets.dto.TicketEstadoResumen;
import com.utp.sistema_tickets.dto.TicketHistorialResponse;
import com.utp.sistema_tickets.model.Categoria;
import com.utp.sistema_tickets.model.EstadoTicket;
import com.utp.sistema_tickets.model.PrioridadTicket;
import com.utp.sistema_tickets.model.Rol;
import com.utp.sistema_tickets.model.Ticket;
import com.utp.sistema_tickets.model.TicketHistorial;
import com.utp.sistema_tickets.model.Usuario;
import java.time.LocalDateTime;
import com.utp.sistema_tickets.repository.CategoriaRepository;
import com.utp.sistema_tickets.repository.TicketHistorialRepository;
import com.utp.sistema_tickets.repository.TicketRepository;
import com.utp.sistema_tickets.repository.UsuarioRepository;
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

    @Transactional
    public Ticket crearComoUsuario(Ticket ticket, String emailAutenticado) {
        Usuario autenticado = usuarioRepository.findByEmailIgnoreCase(emailAutenticado)
                .orElseThrow(() -> new TicketValidationException(
                        "Usuario autenticado no encontrado: " + emailAutenticado));
        if (ticket == null || ticket.getSolicitante() == null
                || !Objects.equals(ticket.getSolicitante().getId(), autenticado.getId())) {
            throw new TicketValidationException(
                    "El usuario solicitante debe coincidir con el usuario autenticado");
        }
        ticket.setTecnicoAsignado(null);
        ticket.setEstado(EstadoTicket.ABIERTO);
        return crear(ticket);
    }

    public List<Ticket> listar() {
        return ticketRepository.findAllWithRelations();
    }

    @Transactional(readOnly = true)
    public List<Ticket> listarVisibles(String email, boolean puedeVerTodos) {
        return puedeVerTodos
                ? ticketRepository.findAllWithRelations()
                : ticketRepository.findBySolicitanteEmailIgnoreCaseOrderByIdDesc(email);
    }

    public List<Ticket> buscarPorEstadoYCategoria(EstadoTicket estado, Long categoriaId) {
        return ticketRepository.buscarPorEstadoYCategoria(estado, categoriaId);
    }

    public List<Ticket> buscarAsignados(Long tecnicoId) {
        return ticketRepository.buscarAsignados(tecnicoId);
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
        return ticketRepository.findByIdWithRelations(id)
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

    @Transactional(readOnly = true)
    public Ticket obtenerAsignado(Long id, String email) {
        return ticketRepository.findByIdAndTecnicoEmailIgnoreCase(id, email)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    @Transactional
    public Ticket actualizar(Long id, Ticket datosActualizados) {
        Ticket actual = obtenerPorId(id);
        validarEstadoActualizable(actual);
        EstadoTicket estadoAnterior = actual.getEstado();
        validarTicket(datosActualizados);
        normalizarCamposTexto(datosActualizados);
        normalizarReferencias(datosActualizados);

        actual.setTitulo(datosActualizados.getTitulo());
        actual.setDescripcion(datosActualizados.getDescripcion());
        actual.setPrioridad(datosActualizados.getPrioridad());
        actual.setSolicitante(datosActualizados.getSolicitante());
        actual.setCategoria(datosActualizados.getCategoria());
        if (datosActualizados.getTecnicoAsignado() != null) {
            actual.setTecnicoAsignado(datosActualizados.getTecnicoAsignado());
        }
        if (datosActualizados.getEstado() != null) {
            actual.setEstado(datosActualizados.getEstado());
        }

        Ticket actualizado = ticketRepository.save(actual);
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
        if (usuarioEjecutor.getRol() != Rol.ADMIN) {
            throw new TicketValidationException("Solo un ADMIN puede asignar técnicos");
        }
        Usuario tecnicoAnterior = ticket.getTecnicoAsignado();
        if (tecnicoAnterior != null
                && !Objects.equals(tecnicoAnterior.getId(), tecnico.getId())) {
            ticketHistorialRepository.vincularTecnicoAnterior(ticketId, tecnicoAnterior);
        }
        ticket.setTecnicoAsignado(tecnico);
        Ticket actualizado = ticketRepository.save(ticket);
        registrarHistorial(actualizado, actualizado.getEstado(), actualizado.getEstado(),
                usuarioEjecutor, comentarioNormalizado(comentario, "Técnico asignado"), tecnico);
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

    @Transactional
    public Ticket cerrarComoSolicitante(Long ticketId, String emailAutenticado, String comentario) {
        Ticket ticket = obtenerVisible(ticketId, emailAutenticado, false);
        validarEstadoActualizable(ticket);
        Usuario solicitante = usuarioRepository.findByEmailIgnoreCase(emailAutenticado)
                .orElseThrow(() -> new TicketValidationException(
                        "Usuario autenticado no encontrado: " + emailAutenticado));
        EstadoTicket estadoAnterior = ticket.getEstado();
        ticket.setEstado(EstadoTicket.CERRADO);
        Ticket cerrado = ticketRepository.save(ticket);
        registrarHistorial(cerrado, estadoAnterior, EstadoTicket.CERRADO, solicitante,
                comentarioNormalizado(comentario, "Solicitud cerrada por el solicitante"));
        return cerrado;
    }

    @Transactional(readOnly = true)
    public List<TicketHistorialResponse> historialVisible(Long ticketId, String email, boolean puedeVerTodos) {
        if (puedeVerTodos) {
            obtenerPorId(ticketId);
        } else {
            obtenerVisible(ticketId, email, false);
        }
        return ticketHistorialRepository.findByTicketIdOrderByFechaEventoAsc(ticketId).stream()
                .map(TicketHistorialResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TicketHistorialResponse> historialParaTecnico(Long ticketId, String email) {
        obtenerAsignado(ticketId, email);
        return ticketHistorialRepository.findByTicketIdOrderByFechaEventoAsc(ticketId).stream()
                .map(TicketHistorialResponse::from)
                .toList();
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
        ticket.setDescripcion(ticket.getDescripcion().trim());
    }

    private void registrarHistorial(Ticket ticket, EstadoTicket estadoAnterior,
                                    EstadoTicket estadoNuevo, Usuario usuarioEjecutor,
                                    String comentario) {
        registrarHistorial(ticket, estadoAnterior, estadoNuevo, usuarioEjecutor, comentario, null);
    }

    private void registrarHistorial(Ticket ticket, EstadoTicket estadoAnterior,
                                    EstadoTicket estadoNuevo, Usuario usuarioEjecutor,
                                    String comentario, Usuario tecnicoAsignado) {
        TicketHistorial historial = new TicketHistorial(
                usuarioEjecutor, estadoAnterior, estadoNuevo, comentario);
        historial.setTicket(ticket);
        historial.setTecnicoAsignado(tecnicoAsignado);
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
