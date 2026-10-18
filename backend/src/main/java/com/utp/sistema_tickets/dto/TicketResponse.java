package com.utp.sistematickets.dto;

import com.utp.sistematickets.model.EstadoTicket;
import com.utp.sistematickets.model.PrioridadTicket;
import com.utp.sistematickets.model.Ticket;
import java.time.LocalDateTime;

public class TicketResponse {

    private final Long id;
    private final String titulo;
    private final String descripcion;
    private final PrioridadTicket prioridad;
    private final EstadoTicket estado;
    private final Long usuarioId;
    private final Long categoriaId;
    private final Long tecnicoId;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaActualizacion;

    private TicketResponse(Long id, String titulo, String descripcion,
                           PrioridadTicket prioridad, EstadoTicket estado,
                           Long usuarioId, Long categoriaId, Long tecnicoId,
                           LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.estado = estado;
        this.usuarioId = usuarioId;
        this.categoriaId = categoriaId;
        this.tecnicoId = tecnicoId;
        this.fechaCreacion = fechaCreacion;
        this.fechaActualizacion = fechaActualizacion;
    }

    public static TicketResponse from(Ticket ticket) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitulo(),
                ticket.getDescripcion(),
                ticket.getPrioridad(),
                ticket.getEstado(),
                ticket.getSolicitante().getId(),
                ticket.getCategoria().getId(),
                ticket.getTecnicoAsignado() == null ? null : ticket.getTecnicoAsignado().getId(),
                ticket.getFechaCreacion(),
                ticket.getFechaActualizacion()
        );
    }

    public Long getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public PrioridadTicket getPrioridad() {
        return prioridad;
    }

    public EstadoTicket getEstado() {
        return estado;
    }

    public Long getUsuarioId() {
        return usuarioId;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
