package com.utp.sistema_tickets.service;

import com.utp.sistema_tickets.model.Categoria;
import com.utp.sistema_tickets.exception.TicketNotFoundException;
import com.utp.sistema_tickets.exception.TicketValidationException;
import com.utp.sistema_tickets.repository.CategoriaRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    public List<Categoria> listar() {
        return categoriaRepository.findAllByOrderByNombreAsc();
    }

    @Transactional
    public Categoria crear(String nombre, String descripcion) {
        validarNombreDisponible(nombre, null);
        return categoriaRepository.save(new Categoria(null, nombre.trim(), normalizarDescripcion(descripcion)));
    }

    @Transactional
    public Categoria actualizar(Long id, String nombre, String descripcion) {
        Categoria categoria = categoriaRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        validarNombreDisponible(nombre, id);
        categoria.setNombre(nombre.trim());
        categoria.setDescripcion(normalizarDescripcion(descripcion));
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void eliminar(Long id) {
        if (!categoriaRepository.existsById(id)) {
            throw new TicketNotFoundException(id);
        }
        categoriaRepository.deleteById(id);
    }

    private void validarNombreDisponible(String nombre, Long idActual) {
        if (nombre == null || nombre.isBlank()) {
            throw new TicketValidationException("El nombre de la categoría es obligatorio");
        }
        boolean duplicado = categoriaRepository.findAllByOrderByNombreAsc().stream()
                .anyMatch(categoria -> categoria.getNombre().equalsIgnoreCase(nombre.trim())
                        && !java.util.Objects.equals(categoria.getId(), idActual));
        if (duplicado) {
            throw new TicketValidationException("Ya existe una categoría con el nombre: " + nombre.trim());
        }
    }

    private String normalizarDescripcion(String descripcion) {
        return descripcion == null ? null : descripcion.trim();
    }
}
