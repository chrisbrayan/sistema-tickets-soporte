package com.utp.sistematickets.service;

import com.utp.sistematickets.exception.TicketNotFoundException;
import com.utp.sistematickets.exception.TicketValidationException;
import com.utp.sistematickets.model.Categoria;
import com.utp.sistematickets.model.Ticket;
import com.utp.sistematickets.model.Usuario;
import com.utp.sistematickets.repository.CategoriaRepository;
import com.utp.sistematickets.repository.TicketRepository;
import com.utp.sistematickets.repository.UsuarioRepository;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UsuarioRepository usuarioRepository;
    private final CategoriaRepository categoriaRepository;

    public TicketService(TicketRepository ticketRepository,
                         UsuarioRepository usuarioRepository,
                         CategoriaRepository categoriaRepository) {
        this.ticketRepository = Objects.requireNonNull(ticketRepository);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
        this.categoriaRepository = Objects.requireNonNull(categoriaRepository);
    }

    public Ticket crear(Ticket ticket) {
        validarTicket(ticket);
        if (ticket.getId() != null) {
            throw new TicketValidationException("Un ticket nuevo no puede tener ID");
        }

        normalizarReferencias(ticket);
        return ticketRepository.save(ticket);
    }

    public List<Ticket> listar() {
        return ticketRepository.findAll();
    }

    public Ticket obtenerPorId(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
    }

    public Ticket actualizar(Long id, Ticket datosActualizados) {
        Ticket actual = obtenerPorId(id);
        validarTicket(datosActualizados);
        normalizarReferencias(datosActualizados);

        datosActualizados.setId(id);
        datosActualizados.setFechaCreacion(actual.getFechaCreacion());
        if (datosActualizados.getEstado() == null) {
            datosActualizados.setEstado(actual.getEstado());
        }

        return ticketRepository.save(datosActualizados);
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
            if (ticket.getTecnicoAsignado().getId() == null) {
                throw new TicketValidationException("El técnico asignado debe tener ID");
            }
            Usuario tecnico = usuarioRepository.findById(ticket.getTecnicoAsignado().getId())
                    .orElseThrow(() -> new TicketValidationException(
                            "Técnico asignado no encontrado: " + ticket.getTecnicoAsignado().getId()));
            ticket.setTecnicoAsignado(tecnico);
        }
    }
}
