package com.appchat.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes_fijados")
public class MensajeFijado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "chat_id")
    private Chat chat;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mensaje_id")
    private Mensaje mensaje;

    @ManyToOne(optional = false)
    @JoinColumn(name = "fijado_por_id")
    private Usuario fijadoPor;

    @Column(nullable = false)
    private LocalDateTime fechaFijado;

    @PrePersist
    public void prePersist() {
        if (fechaFijado == null) {
            fechaFijado = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public Mensaje getMensaje() {
        return mensaje;
    }

    public void setMensaje(Mensaje mensaje) {
        this.mensaje = mensaje;
    }

    public Usuario getFijadoPor() {
        return fijadoPor;
    }

    public void setFijadoPor(Usuario fijadoPor) {
        this.fijadoPor = fijadoPor;
    }

    public LocalDateTime getFechaFijado() {
        return fechaFijado;
    }

    public void setFechaFijado(LocalDateTime fechaFijado) {
        this.fechaFijado = fechaFijado;
    }
}