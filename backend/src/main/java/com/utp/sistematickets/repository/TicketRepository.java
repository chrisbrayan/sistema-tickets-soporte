package com.utp.sistematickets.repository;

import com.utp.sistematickets.model.Ticket;
import java.util.List;
import java.util.Optional;

public interface TicketRepository {

    Ticket save(Ticket ticket);

    List<Ticket> findAll();

    Optional<Ticket> findById(Long id);

    boolean deleteById(Long id);
}
