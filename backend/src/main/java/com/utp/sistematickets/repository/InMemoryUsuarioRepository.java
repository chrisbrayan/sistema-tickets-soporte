package com.utp.sistematickets.repository;

import com.utp.sistematickets.model.Rol;
import com.utp.sistematickets.model.Usuario;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class InMemoryUsuarioRepository implements UsuarioRepository {

    private final Map<Long, Usuario> usuarios = Map.of(
            1L, new Usuario(1L, "Ana Usuario", "ana.usuario@example.com", Rol.USUARIO),
            2L, new Usuario(2L, "Luis Técnico", "luis.tecnico@example.com", Rol.TECNICO),
            3L, new Usuario(3L, "María Administradora", "maria.admin@example.com", Rol.ADMIN)
    );

    @Override
    public Optional<Usuario> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(usuarios.get(id));
    }
}
