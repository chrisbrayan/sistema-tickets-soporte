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

    java.util.Optional<Ticket> findByTituloIgnoreCase(String titulo);

    @Query("""
            SELECT t
            FROM Ticket t
            JOIN FETCH t.solicitante
            JOIN FETCH t.categoria
            LEFT JOIN FETCH t.tecnicoAsignado
            ORDER BY t.id DESC
            """)
    List<Ticket> findAllWithRelations();

    @Query("""
            SELECT t
            FROM Ticket t
            JOIN FETCH t.solicitante
            JOIN FETCH t.categoria
            LEFT JOIN FETCH t.tecnicoAsignado
            WHERE t.id = :id
            """)
    Optional<Ticket> findByIdWithRelations(@Param("id") Long id);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"solicitante", "categoria", "tecnicoAsignado"})
    List<Ticket> findBySolicitanteEmailIgnoreCaseOrderByIdDesc(String email);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"solicitante", "categoria", "tecnicoAsignado"})
    Optional<Ticket> findByIdAndSolicitanteEmailIgnoreCase(Long id, String email);

    @Query("""
            SELECT t
            FROM Ticket t
            JOIN FETCH t.solicitante
            JOIN FETCH t.categoria
            LEFT JOIN FETCH t.tecnicoAsignado
            WHERE t.estado = :estado
              AND t.categoria.id = :categoriaId
            ORDER BY t.id DESC
            """)
    List<Ticket> buscarPorEstadoYCategoria(@Param("estado") EstadoTicket estado,
                                           @Param("categoriaId") Long categoriaId);

    @Query("""
            SELECT t
            FROM Ticket t
            JOIN FETCH t.solicitante
            JOIN FETCH t.categoria
            LEFT JOIN FETCH t.tecnicoAsignado
            WHERE t.id = :id
              AND t.tecnicoAsignado.email = :email
            """)
    Optional<Ticket> findByIdAndTecnicoEmailIgnoreCase(@Param("id") Long id,
                                                       @Param("email") String email);

    @Query("""
            SELECT t
            FROM Ticket t
            JOIN FETCH t.solicitante
            JOIN FETCH t.categoria
            LEFT JOIN FETCH t.tecnicoAsignado
            WHERE t.tecnicoAsignado.id = :tecnicoId
            ORDER BY t.id DESC
            """)
    List<Ticket> buscarAsignados(@Param("tecnicoId") Long tecnicoId);

    @Query("""
            SELECT t
            FROM Ticket t
            JOIN FETCH t.solicitante
            JOIN FETCH t.categoria
            LEFT JOIN FETCH t.tecnicoAsignado
            WHERE (:estado IS NULL OR t.estado = :estado)
              AND (:prioridad IS NULL OR t.prioridad = :prioridad)
              AND (:categoriaId IS NULL OR t.categoria.id = :categoriaId)
              AND (:desde IS NULL OR t.fechaCreacion >= :desde)
              AND (:hasta IS NULL OR t.fechaCreacion <= :hasta)
            ORDER BY t.id DESC
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
