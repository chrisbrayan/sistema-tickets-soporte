package com.utp.sistema_tickets.dto;

import jakarta.validation.constraints.NotNull;

public class AsignarTecnicoRequest {

    @NotNull(message = "El técnico es obligatorio")
    private Long tecnicoId;

    @NotNull(message = "El usuario ejecutor es obligatorio")
    private Long usuarioEjecutorId;

    private String comentario;

    public Long getTecnicoId() {
        return tecnicoId;
    }

    public void setTecnicoId(Long tecnicoId) {
        this.tecnicoId = tecnicoId;
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
