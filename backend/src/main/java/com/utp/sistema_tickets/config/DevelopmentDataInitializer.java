package com.utp.sistema_tickets.config;

import com.utp.sistema_tickets.model.Categoria;
import com.utp.sistema_tickets.model.EstadoTicket;
import com.utp.sistema_tickets.model.PrioridadTicket;
import com.utp.sistema_tickets.model.Rol;
import com.utp.sistema_tickets.model.Ticket;
import com.utp.sistema_tickets.model.TicketHistorial;
import com.utp.sistema_tickets.model.Usuario;
import com.utp.sistema_tickets.repository.CategoriaRepository;
import com.utp.sistema_tickets.repository.TicketHistorialRepository;
import com.utp.sistema_tickets.repository.TicketRepository;
import com.utp.sistema_tickets.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Carga los usuarios, categorías y tickets de demostración que se muestran en
 * las capturas de referencia del proyecto.
 *
 * <p>Los datos son idempotentes: si un usuario o categoría ya existe se
 * actualiza, y los tickets solo se insertan cuando la tabla está vacía para no
 * duplicar información durante los reinicios.</p>
 */
@Configuration
public class DevelopmentDataInitializer {

    private static final String ADMIN_EMAIL = "admin@soporte.com";
    private static final String TECNICO_EMAIL = "tecnico@soporte.com";
    private static final String TECNICO_2_EMAIL = "tecnico2@soporte.com";
    private static final String USUARIO_EMAIL = "usuario@soporte.com";
    private static final String USUARIO_2_EMAIL = "sofia@soporte.com";

    @Bean
    CommandLineRunner initializeDevelopmentData(UsuarioRepository usuarioRepository,
                                                PasswordEncoder passwordEncoder,
                                                CategoriaRepository categoriaRepository,
                                                TicketRepository ticketRepository,
                                                TicketHistorialRepository ticketHistorialRepository,
                                                Environment environment) {
        return args -> {
            Map<String, Usuario> usuarios = new LinkedHashMap<>();
            usuarios.put(ADMIN_EMAIL, ensureUser(usuarioRepository, passwordEncoder,
                    "Laura Méndez", ADMIN_EMAIL, "admin123", Rol.ADMIN));
            usuarios.put(TECNICO_EMAIL, ensureUser(usuarioRepository, passwordEncoder,
                    "Carlos Ruiz", TECNICO_EMAIL, "tecnico123", Rol.TECNICO));
            usuarios.put(TECNICO_2_EMAIL, ensureUser(usuarioRepository, passwordEncoder,
                    "Marta Solís", TECNICO_2_EMAIL, "tecnico123", Rol.TECNICO));
            usuarios.put(USUARIO_EMAIL, ensureUser(usuarioRepository, passwordEncoder,
                    "Andrés Pérez", USUARIO_EMAIL, "usuario123", Rol.USUARIO));
            usuarios.put(USUARIO_2_EMAIL, ensureUser(usuarioRepository, passwordEncoder,
                    "Sofía Ortega", USUARIO_2_EMAIL, "usuario123", Rol.USUARIO));

            Map<String, Categoria> categorias = new LinkedHashMap<>();
            categorias.put("Hardware", ensureCategory(categoriaRepository, "Hardware",
                    "Problemas con equipos y periféricos."));
            categorias.put("Software", ensureCategory(categoriaRepository, "Software",
                    "Problemas con aplicaciones y sistemas."));
            categorias.put("Redes", ensureCategory(categoriaRepository, "Redes",
                    "Problemas de conectividad e infraestructura de red."));
            categorias.put("Accesos", ensureCategory(categoriaRepository, "Accesos",
                    "Problemas de acceso a cuentas y servicios."));
            categorias.put("Otros", ensureCategory(categoriaRepository, "Otros",
                    "Solicitudes que no corresponden a las demás categorías."));

            boolean seedTickets = Boolean.parseBoolean(
                    environment.getProperty("app.demo.tickets.enabled", "true"));
            if (seedTickets && ticketRepository.count() == 0) {
                seedDemoTickets(ticketRepository, ticketHistorialRepository, usuarios, categorias);
            }
        };
    }

    private void seedDemoTickets(TicketRepository ticketRepository,
                                 TicketHistorialRepository historialRepository,
                                 Map<String, Usuario> usuarios,
                                 Map<String, Categoria> categorias) {
        Usuario admin = usuarios.get(ADMIN_EMAIL);
        Usuario carlos = usuarios.get(TECNICO_EMAIL);
        Usuario marta = usuarios.get(TECNICO_2_EMAIL);
        Usuario andres = usuarios.get(USUARIO_EMAIL);
        Usuario sofia = usuarios.get(USUARIO_2_EMAIL);

        Ticket ticket1 = createTicket(ticketRepository,
                "No puedo acceder a la VPN",
                "Desde ayer la VPN rechaza mis credenciales y no puedo conectarme al trabajo remoto.",
                PrioridadTicket.ALTA, EstadoTicket.EN_PROGRESO, andres,
                categorias.get("Accesos"), carlos,
                LocalDateTime.of(2026, 11, 8, 9, 12),
                LocalDateTime.of(2026, 11, 9, 19, 28));
        addHistory(historialRepository, ticket1, andres, null, EstadoTicket.ABIERTO,
                "Ticket creado", LocalDateTime.of(2026, 11, 8, 9, 12));
        addHistory(historialRepository, ticket1, admin, EstadoTicket.ABIERTO, EstadoTicket.ABIERTO,
                "Técnico asignado", LocalDateTime.of(2026, 11, 8, 11, 15));
        addHistory(historialRepository, ticket1, carlos, EstadoTicket.ABIERTO, EstadoTicket.EN_PROGRESO,
                "Estado cambiado a EN_PROGRESO", LocalDateTime.of(2026, 11, 9, 19, 28));

        Ticket ticket2 = createTicket(ticketRepository,
                "Monitor sin señal en puesto 14",
                "El monitor del puesto 14 no muestra imagen aunque la CPU está encendida.",
                PrioridadTicket.MEDIA, EstadoTicket.ABIERTO, sofia,
                categorias.get("Hardware"), null,
                LocalDateTime.of(2026, 11, 9, 8, 40),
                LocalDateTime.of(2026, 11, 9, 8, 40));
        addHistory(historialRepository, ticket2, sofia, null, EstadoTicket.ABIERTO,
                "Ticket creado", LocalDateTime.of(2026, 11, 9, 8, 40));

        Ticket ticket3 = createTicket(ticketRepository,
                "Instalar suite de diseño",
                "Necesito instalar la suite de diseño aprobada para el equipo de marketing.",
                PrioridadTicket.BAJA, EstadoTicket.RESUELTO, andres,
                categorias.get("Software"), marta,
                LocalDateTime.of(2026, 11, 4, 10, 15),
                LocalDateTime.of(2026, 11, 4, 15, 40));
        addHistory(historialRepository, ticket3, andres, null, EstadoTicket.ABIERTO,
                "Ticket creado", LocalDateTime.of(2026, 11, 4, 10, 15));
        addHistory(historialRepository, ticket3, admin, EstadoTicket.ABIERTO, EstadoTicket.ABIERTO,
                "Técnico asignado", LocalDateTime.of(2026, 11, 4, 11, 0));
        addHistory(historialRepository, ticket3, marta, EstadoTicket.ABIERTO, EstadoTicket.RESUELTO,
                "Estado cambiado a RESUELTO", LocalDateTime.of(2026, 11, 4, 15, 40));

        Ticket ticket4 = createTicket(ticketRepository,
                "Caída de red en el piso 3",
                "Todo el piso 3 está sin conexión a internet desde las 9:00.",
                PrioridadTicket.URGENTE, EstadoTicket.EN_PROGRESO, sofia,
                categorias.get("Redes"), carlos,
                LocalDateTime.of(2026, 11, 9, 20, 32),
                LocalDateTime.of(2026, 11, 9, 22, 32));
        addHistory(historialRepository, ticket4, sofia, null, EstadoTicket.ABIERTO,
                "Ticket creado", LocalDateTime.of(2026, 11, 9, 20, 32));
        addHistory(historialRepository, ticket4, admin, EstadoTicket.ABIERTO, EstadoTicket.ABIERTO,
                "Técnico asignado", LocalDateTime.of(2026, 11, 9, 21, 32));
        addHistory(historialRepository, ticket4, carlos, EstadoTicket.ABIERTO, EstadoTicket.EN_PROGRESO,
                "Estado cambiado a EN_PROGRESO", LocalDateTime.of(2026, 11, 9, 22, 32));

        Ticket ticket5 = createTicket(ticketRepository,
                "Restablecer contraseña de correo",
                "Olvidé mi contraseña y el autoservicio no me envía el enlace.",
                PrioridadTicket.MEDIA, EstadoTicket.CERRADO, andres,
                categorias.get("Accesos"), marta,
                LocalDateTime.of(2026, 11, 3, 20, 32),
                LocalDateTime.of(2026, 11, 4, 22, 32));
        addHistory(historialRepository, ticket5, andres, null, EstadoTicket.ABIERTO,
                "Ticket creado", LocalDateTime.of(2026, 11, 3, 20, 32));
        addHistory(historialRepository, ticket5, admin, EstadoTicket.ABIERTO, EstadoTicket.ABIERTO,
                "Técnico asignado", LocalDateTime.of(2026, 11, 3, 21, 32));
        addHistory(historialRepository, ticket5, marta, EstadoTicket.ABIERTO, EstadoTicket.CERRADO,
                "Estado cambiado a CERRADO", LocalDateTime.of(2026, 11, 4, 22, 32));

        Ticket ticket6 = createTicket(ticketRepository,
                "Impresora atascada constantemente",
                "La impresora del área administrativa se atasca cada vez que imprimimos.",
                PrioridadTicket.BAJA, EstadoTicket.ABIERTO, sofia,
                categorias.get("Hardware"), null,
                LocalDateTime.of(2026, 11, 7, 9, 5),
                LocalDateTime.of(2026, 11, 7, 9, 5));
        addHistory(historialRepository, ticket6, sofia, null, EstadoTicket.ABIERTO,
                "Ticket creado", LocalDateTime.of(2026, 11, 7, 9, 5));

        Ticket ticket7 = createTicket(ticketRepository,
                "Licencia de Office vencida",
                "La licencia de Microsoft Office venció y no permite guardar documentos.",
                PrioridadTicket.ALTA, EstadoTicket.ABIERTO, andres,
                categorias.get("Software"), carlos,
                LocalDateTime.of(2026, 11, 8, 11, 45),
                LocalDateTime.of(2026, 11, 8, 12, 30));
        addHistory(historialRepository, ticket7, andres, null, EstadoTicket.ABIERTO,
                "Ticket creado", LocalDateTime.of(2026, 11, 8, 11, 45));
        addHistory(historialRepository, ticket7, admin, EstadoTicket.ABIERTO, EstadoTicket.ABIERTO,
                "Técnico asignado", LocalDateTime.of(2026, 11, 8, 12, 30));

        Ticket ticket8 = createTicket(ticketRepository,
                "Solicitud de nuevo teclado",
                "El teclado del puesto de recepción presenta teclas que no responden.",
                PrioridadTicket.BAJA, EstadoTicket.CERRADO, sofia,
                categorias.get("Otros"), carlos,
                LocalDateTime.of(2026, 11, 6, 9, 20),
                LocalDateTime.of(2026, 11, 9, 19, 30));
        addHistory(historialRepository, ticket8, sofia, null, EstadoTicket.ABIERTO,
                "Ticket creado", LocalDateTime.of(2026, 11, 6, 9, 20));
        addHistory(historialRepository, ticket8, admin, EstadoTicket.ABIERTO, EstadoTicket.ABIERTO,
                "Técnico asignado", LocalDateTime.of(2026, 11, 6, 10, 15));
        addHistory(historialRepository, ticket8, carlos, EstadoTicket.ABIERTO, EstadoTicket.EN_PROGRESO,
                "Estado cambiado a EN_PROGRESO", LocalDateTime.of(2026, 11, 6, 11, 0));
        addHistory(historialRepository, ticket8, carlos, EstadoTicket.EN_PROGRESO, EstadoTicket.CERRADO,
                "Estado cambiado a CERRADO", LocalDateTime.of(2026, 11, 9, 19, 30));
    }

    private Ticket createTicket(TicketRepository ticketRepository,
                                String titulo,
                                String descripcion,
                                PrioridadTicket prioridad,
                                EstadoTicket estado,
                                Usuario solicitante,
                                Categoria categoria,
                                Usuario tecnico,
                                LocalDateTime fechaCreacion,
                                LocalDateTime fechaActualizacion) {
        Ticket ticket = new Ticket(titulo, descripcion, prioridad, solicitante, categoria);
        ticket.setEstado(estado);
        if (tecnico != null) {
            ticket.setTecnicoAsignado(tecnico);
        }
        ticket.setFechaCreacion(fechaCreacion);
        ticket.setFechaActualizacion(fechaActualizacion);
        return ticketRepository.save(ticket);
    }

    private void addHistory(TicketHistorialRepository historialRepository,
                            Ticket ticket,
                            Usuario executor,
                            EstadoTicket estadoAnterior,
                            EstadoTicket estadoNuevo,
                            String comentario,
                            LocalDateTime fechaEvento) {
        TicketHistorial historial = new TicketHistorial(
                executor, estadoAnterior, estadoNuevo, comentario);
        historial.setTicket(ticket);
        if (estadoAnterior != null && estadoAnterior == estadoNuevo) {
            historial.setTecnicoAsignado(ticket.getTecnicoAsignado());
        }
        historial.setFechaEvento(fechaEvento);
        historialRepository.save(historial);
    }

    private Categoria ensureCategory(CategoriaRepository categoriaRepository,
                                     String nombre,
                                     String descripcion) {
        return categoriaRepository.findAllByOrderByNombreAsc().stream()
                .filter(categoria -> categoria.getNombre().equalsIgnoreCase(nombre))
                .findFirst()
                .map(categoria -> {
                    categoria.setNombre(nombre);
                    categoria.setDescripcion(descripcion);
                    return categoriaRepository.save(categoria);
                })
                .orElseGet(() -> categoriaRepository.save(new Categoria(null, nombre, descripcion)));
    }

    private Usuario ensureUser(UsuarioRepository usuarioRepository,
                               PasswordEncoder passwordEncoder,
                               String nombre,
                               String email,
                               String password,
                               Rol rol) {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseGet(() -> new Usuario(null, nombre, email, passwordEncoder.encode(password), rol));
        usuario.setNombre(nombre);
        usuario.setEmail(email);
        usuario.setRol(rol);
        usuario.setPasswordHash(passwordEncoder.encode(password));
        return usuarioRepository.save(usuario);
    }
}
