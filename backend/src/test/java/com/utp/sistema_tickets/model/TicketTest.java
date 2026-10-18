package com.utp.sistema_tickets.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TicketTest {

    @Test
    void ticketNuevoIniciaAbierto() {
        Ticket ticket = new Ticket();

        assertThat(ticket.getEstado()).isEqualTo(EstadoTicket.ABIERTO);
        assertThat(ticket.getFechaCreacion()).isNotNull();
        assertThat(ticket.getFechaActualizacion()).isEqualTo(ticket.getFechaCreacion());
    }

    @Test
    void ticketsConElMismoIdRepresentanLaMismaEntidad() {
        Ticket primero = new Ticket();
        primero.setId(10L);
        Ticket segundo = new Ticket();
        segundo.setId(10L);

        assertThat(primero).isEqualTo(segundo);
        assertThat(primero.hashCode()).isEqualTo(segundo.hashCode());
    }
}
