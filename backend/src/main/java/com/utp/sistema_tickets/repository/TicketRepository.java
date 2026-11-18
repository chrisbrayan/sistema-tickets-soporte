package com.utp.sistema_tickets.repository;

import com.utp.sistema_tickets.dto.TicketEstadoResumen;
import com.utp.sistema_tickets.model.EstadoTicket;
import com.utp.sistema_tickets.model.PrioridadTicket;
import com.utp.sistema_tickets.model.Ticket;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findBySolicitanteEmailIgnoreCaseOrderByFechaCreacionDesc(String email);

    Optional<Ticket> findByIdAndSolicitanteEmailIgnoreCase(Long id, String email);

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE t.estado = :estado
              AND t.categoria.id = :categoriaId
            ORDER BY t.fechaCreacion DESC
            """)
    List<Ticket> buscarPorEstadoYCategoria(@Param("estado") EstadoTicket estado,
                                           @Param("categoriaId") Long categoriaId);

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE t.tecnicoAsignado.id = :tecnicoId
              AND t.estado <> com.utp.sistema_tickets.model.EstadoTicket.CERRADO
            ORDER BY t.fechaActualizacion DESC
            """)
    List<Ticket> buscarAsignadosNoCerrados(@Param("tecnicoId") Long tecnicoId);

    @Query("""
            SELECT t
            FROM Ticket t
            WHERE (:estado IS NULL OR t.estado = :estado)
              AND (:prioridad IS NULL OR t.prioridad = :prioridad)
              AND (:categoriaId IS NULL OR t.categoria.id = :categoriaId)
              AND (:desde IS NULL OR t.fechaCreacion >= :desde)
              AND (:hasta IS NULL OR t.fechaCreacion <= :hasta)
            ORDER BY t.fechaCreacion DESC
            """)
    List<Ticket> buscarConFiltros(@Param("estado") EstadoTicket estado,
                                  @Param("prioridad") PrioridadTicket prioridad,
                                  @Param("categoriaId") Long categoriaId,
                                  @Param("desde") LocalDateTime desde,
                                  @Param("hasta") LocalDateTime hasta);

    @Query("""
            SELECT new com.utp.sistema_tickets.dto.TicketEstadoResumen(t.estado, COUNT(t))
            FROM Ticket t
            GROUP BY t.estado
            ORDER BY t.estado
            """)
    List<TicketEstadoResumen> resumenPorEstado();
}
