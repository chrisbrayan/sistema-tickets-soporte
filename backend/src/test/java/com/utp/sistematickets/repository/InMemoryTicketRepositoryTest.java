package com.utp.sistematickets.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.utp.sistematickets.model.Categoria;
import com.utp.sistematickets.model.PrioridadTicket;
import com.utp.sistematickets.model.Ticket;
import com.utp.sistematickets.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryTicketRepositoryTest {

    private TicketRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryTicketRepository();
    }

    @Test
    void saveGeneraIdsSecuencialesYFindAllLosDevuelveOrdenados() {
        Ticket primero = repository.save(ticket("Primero"));
        Ticket segundo = repository.save(ticket("Segundo"));

        assertThat(primero.getId()).isEqualTo(1L);
        assertThat(segundo.getId()).isEqualTo(2L);
        assertThat(repository.findAll()).containsExactly(primero, segundo);
    }

    @Test
    void saveConIdExistenteActualizaSinCrearOtroRegistro() {
        Ticket ticket = repository.save(ticket("Original"));
        ticket.setTitulo("Actualizado");

        Ticket guardado = repository.save(ticket);

        assertThat(guardado.getId()).isEqualTo(1L);
        assertThat(repository.findAll()).hasSize(1);
        assertThat(repository.findById(1L)).get().extracting(Ticket::getTitulo)
                .isEqualTo("Actualizado");
    }

    @Test
    void findAndDeleteManejanIdsInexistentesSinLanzarExcepciones() {
        assertThat(repository.findById(99L)).isEmpty();
        assertThat(repository.findById(null)).isEmpty();
        assertThat(repository.deleteById(99L)).isFalse();
        assertThat(repository.deleteById(null)).isFalse();

        Ticket ticket = repository.save(ticket("Eliminar"));
        assertThat(repository.deleteById(ticket.getId())).isTrue();
        assertThat(repository.findById(ticket.getId())).isEmpty();
    }

    private Ticket ticket(String titulo) {
        Usuario usuario = new Usuario(1L, "Ana", "ana@example.com", null);
        Categoria categoria = new Categoria(1L, "Acceso", "Problemas de acceso");
        return new Ticket(titulo, "Descripción", PrioridadTicket.MEDIA, usuario, categoria);
    }
}
