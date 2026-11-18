package com.utp.sistematickets.controller;

import com.utp.sistematickets.dto.TicketRequest;
import com.utp.sistematickets.dto.TicketResponse;
import com.utp.sistematickets.model.Categoria;
import com.utp.sistematickets.model.Ticket;
import com.utp.sistematickets.model.Usuario;
import com.utp.sistematickets.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> crear(@Valid @RequestBody TicketRequest request) {
        Ticket creado = ticketService.crear(toDomain(request, false));
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponse.from(creado));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> listar() {
        List<TicketResponse> response = ticketService.listar().stream()
                .map(TicketResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> obtener(@PathVariable Long id) {
        return ResponseEntity.ok(TicketResponse.from(ticketService.obtenerPorId(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> actualizar(@PathVariable Long id,
                                                     @Valid @RequestBody TicketRequest request) {
        Ticket actualizado = ticketService.actualizar(id, toDomain(request, true));
        return ResponseEntity.ok(TicketResponse.from(actualizado));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        ticketService.eliminarPorId(id);
        return ResponseEntity.noContent().build();
    }

    private Ticket toDomain(TicketRequest request, boolean updating) {
        Usuario solicitante = new Usuario(request.getUsuarioId(), null, null, null);
        Categoria categoria = new Categoria(request.getCategoriaId(), null, null);
        Ticket ticket = new Ticket(request.getTitulo(), request.getDescripcion(), request.getPrioridad(),
                solicitante, categoria);

        if (request.getTecnicoId() != null) {
            ticket.setTecnicoAsignado(new Usuario(request.getTecnicoId(), null, null, null));
        }
        if (request.getEstado() != null || updating) {
            ticket.setEstado(request.getEstado());
        }
        return ticket;
    }
}
