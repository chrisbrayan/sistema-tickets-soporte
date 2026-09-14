package com.utp.sistematickets.repository;

import com.utp.sistematickets.model.Usuario;
import java.util.Optional;

public interface UsuarioRepository {

    Optional<Usuario> findById(Long id);
}
