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
    private Long comunidadId;
    
    @ManyToOne
    @JoinColumn(name = "usuario_invitado_id")
    private Long usuarioInvitadoId;
    
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private Long ownerId;

    @Enumerated(EnumType.STRING)
    private EstadoInvitacion estado;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getComunidadId() {
        return comunidadId;
    }

    public void setComunidadId(Long comunidadId) {
        this.comunidadId = comunidadId;
    }

    public Long getUsuarioInvitadoId() {
        return usuarioInvitadoId;
    }

    public void setUsuarioInvitadoId(Long usuarioInvitadoId) {
        this.usuarioInvitadoId = usuarioInvitadoId;
    }

    public Long getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(Long ownerId) {
        this.ownerId = ownerId;
    }

    public EstadoInvitacion getEstado() {
        return estado;
    }

    public void setEstado(EstadoInvitacion estado) {
        this.estado = estado;
    }  
}