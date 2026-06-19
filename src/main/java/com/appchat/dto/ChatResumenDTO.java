package com.appchat.dto;

import java.time.LocalDateTime;

public class ChatResumenDTO {

    private Long id;
    private String tipo;
    private String nombre;
    private String fotoUrl;
    private LocalDateTime fechaCreacion;
    private String ultimoMensajeContenido;
    private LocalDateTime ultimoMensajeFecha;
    
    private Long usuarioInterlocutorId; // solo para DIRECTO

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
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

    public String getUltimoMensajeContenido() {
        return ultimoMensajeContenido;
    }

    public void setUltimoMensajeContenido(String ultimoMensajeContenido) {
        this.ultimoMensajeContenido = ultimoMensajeContenido;
    }

    public LocalDateTime getUltimoMensajeFecha() {
        return ultimoMensajeFecha;
    }

    public void setUltimoMensajeFecha(LocalDateTime ultimoMensajeFecha) {
        this.ultimoMensajeFecha = ultimoMensajeFecha;
    }

    public Long getUsuarioInterlocutorId() {
        return usuarioInterlocutorId;
    }

    public void setUsuarioInterlocutorId(Long usuarioInterlocutorId) {
        this.usuarioInterlocutorId = usuarioInterlocutorId;
    }

    
}