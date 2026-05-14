package com.appchat.dto;

import com.appchat.model.enums.RolGrupo;
import jakarta.validation.constraints.NotNull;

public class ChatRolGrupoRequestDTO {

    @NotNull
    private Long usuarioId;

    @NotNull
    private RolGrupo rol;

    public Long getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(Long usuarioId) {
        this.usuarioId = usuarioId;
    }

    public RolGrupo getRol() {
        return rol;
    }

    public void setRol(RolGrupo rol) {
        this.rol = rol;
    }
}