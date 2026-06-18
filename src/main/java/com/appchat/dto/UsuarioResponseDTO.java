package com.appchat.dto;

import com.appchat.model.enums.EstadoUsuario;

public class UsuarioResponseDTO {

    private Long id;
    private String nombre;
    private String apellido;
    private String email;
    private String username;
    private EstadoUsuario estado;

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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public EstadoUsuario getEstado() {
        return estado;
    }

    public void setEstado(EstadoUsuario estado) {
        this.estado = estado;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    
    private String fotoPerfil;

public String getFotoPerfil() {
    return fotoPerfil;
}

public void setFotoPerfil(String fotoPerfil) {
    this.fotoPerfil = fotoPerfil;
}
    
}