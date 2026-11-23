package com.utp.sistema_tickets.dto;

import com.utp.sistema_tickets.model.EstadoTicket;
import com.utp.sistema_tickets.model.PrioridadTicket;
import com.utp.sistema_tickets.model.Ticket;
import java.time.LocalDateTime;

public class TicketResponse {

    private final Long id;
    private final String titulo;
    private final String descripcion;
    private final PrioridadTicket prioridad;
    private final EstadoTicket estado;
    private final Long usuarioId;
    private final String usuarioNombre;
    private final String usuarioEmail;
    private final Long categoriaId;
    private final String categoriaNombre;
    private final Long tecnicoId;
    private final String tecnicoNombre;
    private final String tecnicoEmail;
    private final LocalDateTime fechaCreacion;
    private final LocalDateTime fechaActualizacion;

    private TicketResponse(Long id, String titulo, String descripcion,
                           PrioridadTicket prioridad, EstadoTicket estado,
                           Long usuarioId, String usuarioNombre, String usuarioEmail,
                           Long categoriaId, String categoriaNombre,
                           Long tecnicoId, String tecnicoNombre, String tecnicoEmail,
                           LocalDateTime fechaCreacion, LocalDateTime fechaActualizacion) {
        this.id = id;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.estado = estado;
        this.usuarioId = usuarioId;
        this.usuarioNombre = usuarioNombre;
        this.usuarioEmail = usuarioEmail;
        this.categoriaId = categoriaId;
        this.categoriaNombre = categoriaNombre;
        this.tecnicoId = tecnicoId;
        this.tecnicoNombre = tecnicoNombre;
        this.tecnicoEmail = tecnicoEmail;
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
                ticket.getSolicitante().getNombre(),
                ticket.getSolicitante().getEmail(),
                ticket.getCategoria().getId(),
                ticket.getCategoria().getNombre(),
                ticket.getTecnicoAsignado() == null ? null : ticket.getTecnicoAsignado().getId(),
                ticket.getTecnicoAsignado() == null ? null : ticket.getTecnicoAsignado().getNombre(),
                ticket.getTecnicoAsignado() == null ? null : ticket.getTecnicoAsignado().getEmail(),
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

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public String getUsuarioEmail() {
        return usuarioEmail;
    }

    public Long getCategoriaId() {
        return categoriaId;
    }

    public String getCategoriaNombre() {
        return categoriaNombre;
    }

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public String getTecnicoNombre() {
        return tecnicoNombre;
    }

    public String getTecnicoEmail() {
        return tecnicoEmail;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
