package com.utp.sistema_tickets.model;

import java.time.LocalDateTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "ticket_historial")
public class TicketHistorial {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_ejecutor_id", nullable = false)
    private Usuario usuarioEjecutor;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", length = 20)
    private EstadoTicket estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoTicket estadoNuevo;

    @Column(length = 500)
    private String comentario;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaEvento;

    protected TicketHistorial() {
    }

    public TicketHistorial(Usuario usuarioEjecutor,
                           EstadoTicket estadoAnterior,
                           EstadoTicket estadoNuevo,
                           String comentario) {
        this.usuarioEjecutor = usuarioEjecutor;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.comentario = comentario;
    }

    @PrePersist
    private void antesDePersistir() {
        if (fechaEvento == null) {
            fechaEvento = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Ticket getTicket() {
        return ticket;
    }

    public void setTicket(Ticket ticket) {
        this.ticket = ticket;
    }

    public Usuario getUsuarioEjecutor() {
        return usuarioEjecutor;
    }

    public EstadoTicket getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoTicket getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getComentario() {
        return comentario;
    }

    public LocalDateTime getFechaEvento() {
        return fechaEvento;
    }
}
