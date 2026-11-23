package com.utp.sistema_tickets.exception;

public class TicketNotFoundException extends RuntimeException {

    public TicketNotFoundException(Long id) {
        super("Ticket no encontrado: " + id);
    }
}
