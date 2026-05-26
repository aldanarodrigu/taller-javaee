package com.appchat.dto;

import java.time.LocalDateTime;

public class MensajeFijadoDTO {

    private Long id;
    private Long mensajeId;
    private Long chatId;
    private String contenido;
    private Long fijadoPorId;
    private LocalDateTime fechaFijado;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(Long mensajeId) {
        this.mensajeId = mensajeId;
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public Long getFijadoPorId() {
        return fijadoPorId;
    }

    public void setFijadoPorId(Long fijadoPorId) {
        this.fijadoPorId = fijadoPorId;
    }

    public LocalDateTime getFechaFijado() {
        return fechaFijado;
    }

    public void setFechaFijado(LocalDateTime fechaFijado) {
        this.fechaFijado = fechaFijado;
    }
}