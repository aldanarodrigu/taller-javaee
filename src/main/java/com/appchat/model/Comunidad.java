package com.appchat.model;

import com.appchat.model.enums.RolComunidad;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "comunidades")
public class Comunidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    private String fotoUrl;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @OneToMany(mappedBy = "comunidad", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<MiembroComunidad> miembros = new ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (fechaCreacion == null) {
            fechaCreacion = LocalDateTime.now();
        }
    }
    
    public void agregarMiembro(Usuario usuario, RolComunidad rol) {
        MiembroComunidad m = new MiembroComunidad();
        m.setUsuario(usuario);
        m.setComunidad(this);
        m.setRol(rol);
        miembros.add(m);
    }

    public boolean esMiembro(Long usuarioId) {
        return miembros.stream()
                .anyMatch(m -> m.getUsuario().getId().equals(usuarioId));
    }

    public boolean esAdmin(Long usuarioId) {
        return miembros.stream()
                .anyMatch(m -> m.getUsuario().getId().equals(usuarioId)
                        && (m.getRol() == RolComunidad.OWNER));
    }
    
   
    public Long getId(){ 
        return id; 
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

    public LocalDateTime getFechaCreacion() { 
        return fechaCreacion; 
    }

    public List<MiembroComunidad> getMiembros() { 
        return miembros; 
    }

 public void setFechaCreacion(LocalDateTime fechaCreacion) {
    this.fechaCreacion = fechaCreacion;
}
}
