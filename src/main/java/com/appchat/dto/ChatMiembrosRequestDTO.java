package com.appchat.dto;

import java.util.List;

public class ChatMiembrosRequestDTO {

    private List<Long> usuarioIds;

    public List<Long> getUsuarioIds() {
        return usuarioIds;
    }

    public void setUsuarioIds(List<Long> usuarioIds) {
        this.usuarioIds = usuarioIds;
    }
}