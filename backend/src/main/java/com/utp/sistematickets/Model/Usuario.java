package com.utp.sistematickets.model;

import java.util.Objects;

public class Usuario {

    private Long id;
    private String nombre;
    private String email;
    private Rol rol;

    public Usuario() {
    }

    public Usuario(Long id, String nombre, String email, Rol rol) {
        this.id = id;
        this.nombre = nombre;
        this.email = email;
        this.rol = rol;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Rol getRol() {
        return rol;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Usuario usuario)) {
            return false;
        }
        return id != null && Objects.equals(id, usuario.id);
    }

    @Override
    public int hashCode() {
        return id == null ? getClass().hashCode() : Objects.hash(id);
    }
}
