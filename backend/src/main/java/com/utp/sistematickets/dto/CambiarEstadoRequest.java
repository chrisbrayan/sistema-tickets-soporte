package com.utp.sistema_tickets.dto;

import com.utp.sistema_tickets.model.EstadoTicket;
import jakarta.validation.constraints.NotNull;

public class CambiarEstadoRequest {

    @NotNull(message = "El estado es obligatorio")
    private EstadoTicket estado;

    @NotNull(message = "El usuario ejecutor es obligatorio")
    private Long usuarioEjecutorId;

    private String comentario;

    public EstadoTicket getEstado() {
        return estado;
    }

    public void setEstado(EstadoTicket estado) {
        this.estado = estado;
    }

    public Long getUsuarioEjecutorId() {
        return usuarioEjecutorId;
    }

    public void setUsuarioEjecutorId(Long usuarioEjecutorId) {
        this.usuarioEjecutorId = usuarioEjecutorId;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }
}
