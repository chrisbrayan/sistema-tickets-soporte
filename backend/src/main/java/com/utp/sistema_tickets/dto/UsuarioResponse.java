package com.utp.sistema_tickets.dto;

import com.utp.sistema_tickets.model.Usuario;

public record UsuarioResponse(Long id, String nombre, String email, String rol) {

    public static UsuarioResponse from(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRol().name());
    }
}
