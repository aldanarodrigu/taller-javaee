package com.appchat.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

public class ComunidadDTO {
    
    @NotBlank
    private String nombre;
    
    @NotBlank
    private String descripcion;
    
    private String fotoUrl;
    
    private LocalDateTime fechaCreacion; //setearla con LocalDateTime.now()?

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

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }  
    
}
