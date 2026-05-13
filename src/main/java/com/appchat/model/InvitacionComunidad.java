package com.appchat.model;

import com.appchat.model.enums.EstadoInvitacion;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "invitacion_comunidad")
public class InvitacionComunidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "comunidad_id")
    private Comunidad comunidad;

    @ManyToOne
    @JoinColumn(name = "usuario_invitado_id")
    private Usuario usuarioInvitado;

    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Usuario owner;

    @Enumerated(EnumType.STRING)
    private EstadoInvitacion estado;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Comunidad getComunidad() { return comunidad; }
    public void setComunidad(Comunidad comunidad) { this.comunidad = comunidad; }

    public Usuario getUsuarioInvitado() { return usuarioInvitado; }
    public void setUsuarioInvitado(Usuario usuarioInvitado) { this.usuarioInvitado = usuarioInvitado; }

    public Usuario getOwner() { return owner; }
    public void setOwner(Usuario owner) { this.owner = owner; }

    public EstadoInvitacion getEstado() { return estado; }
    public void setEstado(EstadoInvitacion estado) { this.estado = estado; }
}