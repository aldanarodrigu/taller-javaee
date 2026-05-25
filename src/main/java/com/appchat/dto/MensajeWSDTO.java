package com.appchat.dto;

public class MensajeWSDTO {

    private String accion;
    private Long chatId;
    private Long mensajeId;
    private Long parentId;
    private String contenido;

    public String getAccion() {
        return accion;
    }

    public void setAccion(String accion) {
        this.accion = accion;
    }

    public Long getChatId(){ 
        return chatId; 
    }
    
    public void setChatId(Long chatId){ 
        this.chatId = chatId; 
    }

    public Long getMensajeId() {
        return mensajeId;
    }

    public void setMensajeId(Long mensajeId) {
        this.mensajeId = mensajeId;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getContenido() { 
        return contenido; 
    }
    public void setContenido(String contenido) { 
        this.contenido = contenido; 
    }
}