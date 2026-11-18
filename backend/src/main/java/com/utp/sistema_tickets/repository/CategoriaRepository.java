package com.utp.sistema_tickets.repository;

import com.utp.sistema_tickets.model.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
