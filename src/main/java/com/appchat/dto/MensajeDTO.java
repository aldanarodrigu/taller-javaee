package com.appchat.dto;

import com.appchat.model.enums.EstadoMensaje;
import com.appchat.model.enums.TipoMensaje;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

public class MensajeDTO {

    private Long id;
    private LocalDateTime fechaEnvio;
    private TipoMensaje tipo;
    private EstadoMensaje estado;
    private String contenido;
    private Long emisorId;
    private String emisorNombre;
    private String emisorApellido;
    private Long parentId;
    private List<ReaccionDTO> reacciones;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public void setFechaEnvio(LocalDateTime fechaEnvio) {
        this.fechaEnvio = fechaEnvio;
    }

    public TipoMensaje getTipo() {
        return tipo;
    }

    public void setTipo(TipoMensaje tipo) {
        this.tipo = tipo;
    }

    public EstadoMensaje getEstado() {
        return estado;
    }

    public void setEstado(EstadoMensaje estado) {
        this.estado = estado;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Long getEmisorId() {
        return emisorId;
    }

    public void setEmisorId(Long emisorId) {
        this.emisorId = emisorId;
    }

    public String getEmisorNombre() {
        return emisorNombre;
    }

    public void setEmisorNombre(String emisorNombre) {
        this.emisorNombre = emisorNombre;
    }

    public String getEmisorApellido() {
        return emisorApellido;
    }

    public void setEmisorApellido(String emisorApellido) {
        this.emisorApellido = emisorApellido;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public List<ReaccionDTO> getReacciones() {
        return reacciones;
    }

    public void setReacciones(List<ReaccionDTO> reacciones) {
        this.reacciones = reacciones;
    }
}