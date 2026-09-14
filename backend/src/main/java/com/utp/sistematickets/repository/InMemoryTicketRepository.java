package com.utp.sistematickets.repository;

import com.utp.sistematickets.model.Ticket;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryTicketRepository implements TicketRepository {

    private final ConcurrentMap<Long, Ticket> tickets = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong();

    @Override
    public Ticket save(Ticket ticket) {
        Objects.requireNonNull(ticket, "ticket no puede ser null");

        if (ticket.getId() == null) {
            ticket.setId(idSequence.incrementAndGet());
        } else {
            idSequence.accumulateAndGet(ticket.getId(), Math::max);
        }

        tickets.put(ticket.getId(), ticket);
        return ticket;
    }

    @Override
    public List<Ticket> findAll() {
        return tickets.values().stream()
                .sorted(Comparator.comparing(Ticket::getId))
                .toList();
    }

    @Override
    public Optional<Ticket> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(tickets.get(id));
    }

    @Override
    public boolean deleteById(Long id) {
        if (id == null) {
            return false;
        }
        return tickets.remove(id) != null;
    }
}
