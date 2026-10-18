package com.utp.sistema_tickets.dto;

import com.utp.sistema_tickets.model.EstadoTicket;

public record TicketEstadoResumen(EstadoTicket estado, long total) {
}
