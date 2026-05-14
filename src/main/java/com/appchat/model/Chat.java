package com.appchat.model;

import com.appchat.model.enums.RolGrupo;
import com.appchat.model.enums.TipoChat;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoChat tipo; 

    private String nombre;
    private String descripcion;
    private String fotoUrl;

    @ManyToOne
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Participa> participantes = new ArrayList<>();

    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fechaEnvio ASC")
    private List<Mensaje> mensajes = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public TipoChat getTipo() {
        return tipo;
    }

    public void setTipo(TipoChat tipo) {
        this.tipo = tipo;
    }

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

    public Comunidad getComunidad() {
        return comunidad;
    }

    public void setComunidad(Comunidad comunidad) {
        this.comunidad = comunidad;
    }

    public List<Participa> getListaParticipaciones() {
        return participantes;
    }

    public void setParticipantes(List<Participa> participantes) {
        this.participantes = participantes;
    }

    public List<Mensaje> getMensajes() {
        return mensajes;
    }

    public void setMensajes(List<Mensaje> mensajes) {
        this.mensajes = mensajes;
    }



    public List<Usuario> getParticipantes() {
        return participantes.stream().map(Participa::getUsuario).toList();
    }

    public void agregarParticipante(Usuario usuario, RolGrupo rol) {
        Participa p = new Participa();
        p.setUsuario(usuario);
        p.setChat(this);
        p.setRol(rol);
        participantes.add(p);
    }

    public boolean removerParticipante(Long usuarioId) {
        return participantes.removeIf(participacion -> participacion.getUsuario().getId().equals(usuarioId));
    }

    public boolean esParticipante(Long usuarioId) {
        return participantes.stream()
                .anyMatch(p -> p.getUsuario().getId().equals(usuarioId));
    }
}