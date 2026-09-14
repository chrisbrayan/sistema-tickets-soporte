package com.utp.sistematickets.model;

import java.time.LocalDateTime;
import java.util.Objects;

public class Ticket {

    private Long id;
    private String titulo;
    private String descripcion;
    private PrioridadTicket prioridad;
    private EstadoTicket estado;
    private Usuario solicitante;
    private Categoria categoria;
    private Usuario tecnicoAsignado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public Ticket() {
        this.estado = EstadoTicket.ABIERTO;
        this.fechaCreacion = LocalDateTime.now();
        this.fechaActualizacion = this.fechaCreacion;
    }

    public Ticket(String titulo, String descripcion, PrioridadTicket prioridad,
                  Usuario solicitante, Categoria categoria) {
        this();
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.solicitante = solicitante;
        this.categoria = categoria;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
        marcarActualizacion();
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
        marcarActualizacion();
    }

    public PrioridadTicket getPrioridad() {
        return prioridad;
    }

    public void setPrioridad(PrioridadTicket prioridad) {
        this.prioridad = prioridad;
        marcarActualizacion();
    }

    public EstadoTicket getEstado() {
        return estado;
    }

    public void setEstado(EstadoTicket estado) {
        this.estado = estado;
        marcarActualizacion();
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public void setSolicitante(Usuario solicitante) {
        this.solicitante = solicitante;
        marcarActualizacion();
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public void setCategoria(Categoria categoria) {
        this.categoria = categoria;
        marcarActualizacion();
    }

    public Usuario getTecnicoAsignado() {
        return tecnicoAsignado;
    }

    public void setTecnicoAsignado(Usuario tecnicoAsignado) {
        this.tecnicoAsignado = tecnicoAsignado;
        marcarActualizacion();
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    private void marcarActualizacion() {
        this.fechaActualizacion = LocalDateTime.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Ticket ticket)) {
            return false;
        }
        return id != null && Objects.equals(id, ticket.id);
    }

    @Override
    public int hashCode() {
        return id == null ? getClass().hashCode() : Objects.hash(id);
    }
}
