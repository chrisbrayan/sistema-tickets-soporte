package com.utp.sistematickets.repository;

import com.utp.sistema_tickets.model.Categoria;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryCategoriaRepository implements CategoriaRepository {

    private final Map<Long, Categoria> categorias = Map.of(
            1L, new Categoria(1L, "Acceso", "Problemas de acceso a sistemas o cuentas"),
            2L, new Categoria(2L, "Hardware", "Incidentes relacionados con equipos"),
            3L, new Categoria(3L, "Software", "Errores o solicitudes de aplicaciones")
    );

    @Override
    public Optional<Categoria> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(categorias.get(id));
    }
}
