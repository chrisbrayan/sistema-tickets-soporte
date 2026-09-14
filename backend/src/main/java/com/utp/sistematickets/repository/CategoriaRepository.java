package com.utp.sistematickets.repository;

import com.utp.sistema_tickets.model.Categoria;
import java.util.Optional;

public interface CategoriaRepository {

    Optional<Categoria> findById(Long id);
}
