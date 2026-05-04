package com.appchat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class ChatGrupoRequestDTO {

    @NotBlank
    private String nombre;

    private String descripcion;

    private String fotoUrl;

    @NotNull
    private Long comunidadId;

    private List<Long> usuarioIds;

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(Long comunidadId) {
        this.comunidadId = comunidadId;
    }

    public List<Long> getUsuarioIds() {
        return usuarioIds;
    }

    public void setUsuarioIds(List<Long> usuarioIds) {
        this.usuarioIds = usuarioIds;
    }
}