package com.appchat.dto;

import java.util.List;

public class ComunidadDetalleDTO {
    private Long id;
    private String nombre;
    private String descripcion;
    private String fotoUrl;
    private Long ownerUserId;
    private List<UsuarioResponseDTO> miembros;

    public ComunidadDetalleDTO() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }
    public Long getOwnerUserId() { return ownerUserId; }
    public void setOwnerUserId(Long ownerUserId) { this.ownerUserId = ownerUserId; }
    public List<UsuarioResponseDTO> getMiembros() { return miembros; }
    public void setMiembros(List<UsuarioResponseDTO> miembros) { this.miembros = miembros; }
}