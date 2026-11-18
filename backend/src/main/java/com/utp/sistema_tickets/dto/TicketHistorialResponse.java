package com.utp.sistema_tickets.dto;

import com.utp.sistema_tickets.model.EstadoTicket;
import com.utp.sistema_tickets.model.TicketHistorial;
import com.utp.sistema_tickets.model.Usuario;
import java.time.LocalDateTime;

public record TicketHistorialResponse(
        Long id,
        LocalDateTime fecha,
        String texto,
        String comentario,
        String usuarioNombre,
        EstadoTicket estadoAnterior,
        EstadoTicket estadoNuevo,
        String tecnicoNombre) {

    public static TicketHistorialResponse from(TicketHistorial historial) {
        String tecnicoNombre = nombreTecnico(historial);
        String texto;
        if (historial.getEstadoAnterior() == null) {
            texto = "Ticket creado";
        } else if (historial.getEstadoAnterior() == historial.getEstadoNuevo()) {
            texto = tecnicoNombre != null ? "Asignado a " + tecnicoNombre : "Técnico asignado";
        } else {
            texto = "Estado cambiado a " + historial.getEstadoNuevo();
        }
        return new TicketHistorialResponse(
                historial.getId(),
                historial.getFechaEvento(),
                texto,
                historial.getComentario(),
                historial.getUsuarioEjecutor().getNombre(),
                historial.getEstadoAnterior(),
                historial.getEstadoNuevo(),
                tecnicoNombre);
    }

    private static String nombreTecnico(TicketHistorial historial) {
        Usuario tecnico = historial.getTecnicoAsignado();
        boolean asignacion = historial.getEstadoAnterior() != null
                && historial.getEstadoAnterior() == historial.getEstadoNuevo();
        if (tecnico == null && asignacion && historial.getTicket() != null) {
            tecnico = historial.getTicket().getTecnicoAsignado();
        }
        return tecnico != null ? tecnico.getNombre() : null;
    }
}
