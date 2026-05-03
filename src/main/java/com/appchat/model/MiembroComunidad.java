package com.appchat.model;

import com.appchat.model.enums.RolComunidad;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "miembro_comunidad",
    uniqueConstraints = @UniqueConstraint(columnNames = {"usuario_id", "comunidad_id"})
)
public class MiembroComunidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "comunidad_id", nullable = false)
    private Comunidad comunidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RolComunidad rol;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaUnion;

    @PrePersist
    public void prePersist() {
        if (fechaUnion == null) {
            fechaUnion = LocalDateTime.now();
        }
        if (rol == null) {
            rol = RolComunidad.MEMBER;
        }
    }

    public Long getId() { 
        return id; 
    }

    public Usuario getUsuario() {
        return usuario; 
    }

    public void setUsuario(Usuario usuario) { 
        this.usuario = usuario;
    }

    public Comunidad getComunidad() { 
        return comunidad;
    }

    public void setComunidad(Comunidad comunidad) { 
        this.comunidad = comunidad;
    }

    public RolComunidad getRol() { 
        return rol; 
    }

    public void setRol(RolComunidad rol) { 
        this.rol = rol; 
    }

    public LocalDateTime getFechaUnion(){ 
        return fechaUnion;
    }
}