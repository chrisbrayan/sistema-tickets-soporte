package com.utp.sistema_tickets.controller;

import com.utp.sistema_tickets.dto.TicketRequest;
import com.utp.sistema_tickets.dto.TicketEstadoResumen;
import com.utp.sistema_tickets.dto.AsignarTecnicoRequest;
import com.utp.sistema_tickets.dto.CambiarEstadoRequest;
import com.utp.sistema_tickets.dto.TicketResponse;
import com.utp.sistema_tickets.dto.TicketHistorialResponse;
import com.utp.sistema_tickets.model.Categoria;
import com.utp.sistema_tickets.model.EstadoTicket;
import com.utp.sistema_tickets.model.PrioridadTicket;
import com.utp.sistema_tickets.model.Ticket;
import com.utp.sistema_tickets.model.Usuario;
import com.utp.sistema_tickets.service.TicketService;
import com.utp.sistema_tickets.service.UsuarioService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final UsuarioService usuarioService;

    public TicketController(TicketService ticketService, UsuarioService usuarioService) {
        this.ticketService = ticketService;
        this.usuarioService = usuarioService;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> crear(@Valid @RequestBody TicketRequest request,
                                                Authentication authentication) {
        Ticket ticket = toDomain(request, false);
        Ticket creado = authentication == null || esAdmin(authentication)
                ? ticketService.crear(ticket)
                : ticketService.crearComoUsuario(ticket, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(TicketResponse.from(creado));
    }

    @GetMapping
    public ResponseEntity<List<TicketResponse>> listar(Authentication authentication) {
        List<Ticket> tickets;
        if (authentication == null || esAdmin(authentication)) {
            tickets = ticketService.listar();
        } else if (esTecnico(authentication)) {
            tickets = ticketService.buscarAsignados(usuarioAutenticado(authentication).getId());
        } else {
            tickets = ticketService.listarVisibles(authentication.getName(), false);
        }
        List<TicketResponse> response = tickets.stream()
                .map(TicketResponse::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/search")
    public ResponseEntity<List<TicketResponse>> buscarPorEstadoYCategoria(
            @RequestParam EstadoTicket estado,
            @RequestParam Long categoriaId) {
        return ResponseEntity.ok(ticketService.buscarPorEstadoYCategoria(estado, categoriaId).stream()
                .map(TicketResponse::from)
                .toList());
    }

    @GetMapping("/mis-asignados")
    public ResponseEntity<List<TicketResponse>> buscarAsignados(
            @RequestParam Long tecnicoId,
            Authentication authentication) {
        Long tecnicoIdConsultado = esTecnico(authentication)
                ? usuarioAutenticado(authentication).getId()
                : tecnicoId;
        return ResponseEntity.ok(ticketService.buscarAsignados(tecnicoIdConsultado).stream()
                .map(TicketResponse::from)
                .toList());
    }

    @GetMapping("/filtrar")
    public ResponseEntity<List<TicketResponse>> buscarConFiltros(
            @RequestParam(required = false) EstadoTicket estado,
            @RequestParam(required = false) PrioridadTicket prioridad,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) LocalDateTime desde,
            @RequestParam(required = false) LocalDateTime hasta) {
        return ResponseEntity.ok(ticketService.buscarConFiltros(
                        estado, prioridad, categoriaId, desde, hasta).stream()
                .map(TicketResponse::from)
                .toList());
    }

    @GetMapping("/resumen-por-estado")
    public ResponseEntity<List<TicketEstadoResumen>> resumenPorEstado() {
        return ResponseEntity.ok(ticketService.resumenPorEstado());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TicketResponse> obtener(@PathVariable Long id,
                                                 Authentication authentication) {
        Ticket ticket;
        if (authentication == null || esAdmin(authentication)) {
            ticket = ticketService.obtenerPorId(id);
        } else if (esTecnico(authentication)) {
            ticket = ticketService.obtenerAsignado(id, authentication.getName());
        } else {
            ticket = ticketService.obtenerVisible(id, authentication.getName(), false);
        }
        return ResponseEntity.ok(TicketResponse.from(ticket));
    }

    @GetMapping("/{id}/historial")
    public ResponseEntity<List<TicketHistorialResponse>> historial(@PathVariable Long id,
                                                                    Authentication authentication) {
        if (authentication == null || esAdmin(authentication)) {
            return ResponseEntity.ok(ticketService.historialVisible(id, authentication == null
                    ? null
                    : authentication.getName(), true));
        }
        if (esTecnico(authentication)) {
            return ResponseEntity.ok(ticketService.historialParaTecnico(id, authentication.getName()));
        }
        return ResponseEntity.ok(ticketService.historialVisible(id, authentication.getName(), false));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TicketResponse> actualizar(@PathVariable Long id,
                                                     @Valid @RequestBody TicketRequest request) {
        Ticket actualizado = ticketService.actualizar(id, toDomain(request, true));
        return ResponseEntity.ok(TicketResponse.from(actualizado));
    }

    @PatchMapping("/{id}/asignacion")
    public ResponseEntity<TicketResponse> asignarTecnico(
            @PathVariable Long id,
            @Valid @RequestBody AsignarTecnicoRequest request,
            Authentication authentication) {
        Ticket actualizado = ticketService.asignarTecnico(
                id, request.getTecnicoId(), idEjecutor(authentication, request.getUsuarioEjecutorId()),
                request.getComentario());
        return ResponseEntity.ok(TicketResponse.from(actualizado));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<TicketResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequest request,
            Authentication authentication) {
        if (esTecnico(authentication)) {
            ticketService.obtenerAsignado(id, authentication.getName());
        }
        Ticket actualizado = ticketService.cambiarEstado(
                id, request.getEstado(), idEjecutor(authentication, request.getUsuarioEjecutorId()),
                request.getComentario());
        return ResponseEntity.ok(TicketResponse.from(actualizado));
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<TicketResponse> cerrar(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequest request,
            Authentication authentication) {
        Ticket cerrado;
        if (authentication == null) {
            cerrado = ticketService.cerrar(id, request.getUsuarioEjecutorId(), request.getComentario());
        } else {
            Usuario autenticado = usuarioAutenticado(authentication);
            if (esAdmin(authentication)) {
                cerrado = ticketService.cerrar(id, autenticado.getId(), request.getComentario());
            } else if (esTecnico(authentication)) {
                ticketService.obtenerAsignado(id, autenticado.getEmail());
                cerrado = ticketService.cerrar(id, autenticado.getId(), request.getComentario());
            } else {
                cerrado = ticketService.cerrarComoSolicitante(id, autenticado.getEmail(), request.getComentario());
            }
        }
        return ResponseEntity.ok(TicketResponse.from(cerrado));
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

    private boolean esAdmin(Authentication authentication) {
        return tieneRol(authentication, "ROLE_ADMIN");
    }

    private boolean esTecnico(Authentication authentication) {
        return tieneRol(authentication, "ROLE_TECNICO");
    }

    private boolean tieneRol(Authentication authentication, String authority) {
        return authentication != null && authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority::equals);
    }

    private Usuario usuarioAutenticado(Authentication authentication) {
        return usuarioService.obtenerPorEmail(authentication.getName());
    }

    private Long idEjecutor(Authentication authentication, Long fallbackId) {
        return authentication == null ? fallbackId : usuarioAutenticado(authentication).getId();
    }
}
