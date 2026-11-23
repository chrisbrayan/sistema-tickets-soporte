package com.utp.sistema_tickets.repository;

import com.utp.sistema_tickets.model.Usuario;
import com.utp.sistema_tickets.model.Rol;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);

    List<Usuario> findAllByOrderByIdAsc();

    List<Usuario> findAllByRolOrderByIdAsc(Rol rol);
}
