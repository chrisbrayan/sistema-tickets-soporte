package com.utp.sistema_tickets.repository;

import com.utp.sistema_tickets.model.TicketHistorial;
import com.utp.sistema_tickets.model.Usuario;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketHistorialRepository extends JpaRepository<TicketHistorial, Long> {

    @EntityGraph(attributePaths = "usuarioEjecutor")
    List<TicketHistorial> findByTicketIdOrderByFechaEventoAsc(Long ticketId);

    @Modifying
    @Query("""
            UPDATE TicketHistorial h
            SET h.tecnicoAsignado = :tecnico
            WHERE h.ticket.id = :ticketId
              AND h.tecnicoAsignado IS NULL
              AND h.estadoAnterior IS NOT NULL
              AND h.estadoAnterior = h.estadoNuevo
            """)
    int vincularTecnicoAnterior(@Param("ticketId") Long ticketId,
                                @Param("tecnico") Usuario tecnico);
}
