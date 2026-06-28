package com.appchat.service;

import com.appchat.dto.ComunidadDTO;
import com.appchat.dto.UsuarioResponseDTO;
import com.appchat.model.Comunidad;
import com.appchat.model.Usuario;
import com.appchat.model.enums.RolComunidad;
import com.appchat.repository.ComunidadRepository;
import com.appchat.dto.ComunidadResumenDTO;
import com.appchat.dto.ComunidadDetalleDTO;
import com.appchat.dto.InvitacionDTO;


import com.appchat.model.InvitacionComunidad;
import com.appchat.model.MiembroComunidad;
import com.appchat.model.enums.EstadoInvitacion;
import com.appchat.repository.InvitacionRepository;
import com.appchat.repository.MiembroComunidadRepository;

import jakarta.inject.Inject;

import jakarta.transaction.Transactional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import com.appchat.model.InvitacionComunidad;

@ApplicationScoped
public class ComunidadService {
    
    @Inject
    private ComunidadRepository comunidadRepository;
    
    @Inject 
    private UsuarioService usuarioService;
    
    @Inject 
    private InvitacionRepository invitacionRepository;
    
    @Inject
    private MiembroComunidadRepository miembroComunidadRepository;

    Comunidad buscarPorId(Long comunidadId) {
        return comunidadRepository.buscarPorId(comunidadId);
    }

    boolean sonMiembros(Long comunidadId, Long usuarioAutenticadoId, Long usuarioDestinoId) {
        return comunidadRepository.sonMiembros(comunidadId, usuarioAutenticadoId, usuarioDestinoId);
    }

    @Transactional
    public ComunidadDetalleDTO obtenerDetalleComunidad(Long id) {
        Comunidad c = comunidadRepository.buscarPorId(id);
        if (c == null) throw new NotFoundException("Comunidad no encontrada");

        Long ownerUserId = c.getMiembros().stream()
            .filter(m -> m.getRol() == RolComunidad.OWNER)
            .map(m -> m.getUsuario().getId())
            .findFirst().orElse(null);

        List<UsuarioResponseDTO> miembros = c.getMiembros().stream()
            .map(m -> usuarioService.mapearUsuario(m.getUsuario()))
            .collect(java.util.stream.Collectors.toList());

        ComunidadDetalleDTO dto = new ComunidadDetalleDTO();
        dto.setId(c.getId());
        dto.setNombre(c.getNombre());
        dto.setDescripcion(c.getDescripcion());
        dto.setFotoUrl(c.getFotoUrl());
        dto.setOwnerUserId(ownerUserId);
        dto.setMiembros(miembros);
        return dto;
    }

    @Transactional
    public Comunidad editarComunidad(Long id, ComunidadDTO dto, Long userId) {
        Comunidad c = comunidadRepository.buscarPorId(id);
        if (c == null) throw new NotFoundException("Comunidad no encontrada");
        if (!c.esAdmin(userId)) throw new ForbiddenException("No autorizado");
        c.setNombre(dto.getNombre());
        c.setDescripcion(dto.getDescripcion());
        if (dto.getFotoUrl() != null) c.setFotoUrl(dto.getFotoUrl());
        return comunidadRepository.actualizar(c);
    }

    @Transactional
    public void eliminarComunidad(Long id, Long userId) {
        Comunidad c = comunidadRepository.buscarPorId(id);
        if (c == null) throw new NotFoundException("Comunidad no encontrada");
        if (!c.esAdmin(userId)) throw new ForbiddenException("No autorizado");
        comunidadRepository.eliminar(id);
    }

    @Transactional
    public Comunidad crearComunidad(ComunidadDTO comunidadDto, Long userId) {
        
        Comunidad comunidadNueva = new Comunidad();
        comunidadNueva.setNombre(comunidadDto.getNombre());
        comunidadNueva.setDescripcion(comunidadDto.getDescripcion());
        comunidadNueva.setFotoUrl(comunidadDto.getFotoUrl());
        comunidadNueva.setFechaCreacion(java.time.LocalDateTime.now());
        
        Usuario creador = usuarioService.obtenerPorId(userId);
        comunidadNueva.agregarMiembro(creador, RolComunidad.OWNER);
        
        comunidadRepository.guardar(comunidadNueva);
        
        return comunidadNueva;
    }

    @Transactional
    public void invitarUsuario(Long comunidadId, String username, Long ownerId) {
        
        Comunidad comunidad = comunidadRepository.buscarPorId(comunidadId);

        if(comunidad == null){
            throw new NotFoundException("Comunidad no existe");
        }

        if(!comunidad.esAdmin(ownerId)){
            throw new ForbiddenException("No Autorizado");
        }

        Usuario u = usuarioService.buscarPorUsername(username);

        if(u == null){
            throw new NotFoundException("Usuario no existe.");
        }

        if(comunidad.esMiembro(u.getId())){
            throw new ClientErrorException("Ya es miembro", Response.Status.CONFLICT);
        }

        boolean yaInvitado = invitacionRepository.existeInvitacionPendiente(comunidadId, u.getId());

        if(yaInvitado){
            throw new ClientErrorException("Ya tiene una invitacion pendiente", Response.Status.CONFLICT);
        }

        Usuario owner = usuarioService.obtenerPorId(ownerId);

        InvitacionComunidad invitacion = new InvitacionComunidad();
        invitacion.setComunidad(comunidad); 
        invitacion.setOwner(owner);       
        invitacion.setUsuarioInvitado(u);  
        invitacion.setEstado(EstadoInvitacion.PENDIENTE);

        invitacionRepository.guardar(invitacion);
    }
    
    public List<UsuarioResponseDTO> listarMiembros(Long comunidadId){
        List<Usuario> miembros = comunidadRepository.listarMiembros(comunidadId);
        
        if(miembros.isEmpty()){
            throw new NotFoundException("No se encontraron miembros.");
        }
        
        List<UsuarioResponseDTO> miembrosComunidad = new ArrayList<>();
        
        for(Usuario u : miembros){
            miembrosComunidad.add(usuarioService.mapearUsuario(u));   
        }
        
        return miembrosComunidad;
    }
    
    public List<ComunidadResumenDTO> listarComunidadesDelUsuario(Long userId) {
        List<Comunidad> comunidades = comunidadRepository.listarPorUsuario(userId);
        List<ComunidadResumenDTO> resultado = new ArrayList<>();
        
        for (Comunidad c : comunidades) {
            resultado.add(new ComunidadResumenDTO(
                c.getId(), c.getNombre(), c.getDescripcion(), c.getFotoUrl()
            ));
        }
        
        return resultado;
    }

    @Transactional
    public void salirComunidad(Long comunidadId, Long userId) {
        Comunidad c = comunidadRepository.buscarPorId(comunidadId);
        
        if(c == null){
            throw new NotFoundException("Comunidad no encontrada");
        }
        
        MiembroComunidad mc = miembroComunidadRepository.buscarPorUsuarioYComunidad(userId, comunidadId);
        
        if(mc == null){
            throw new BadRequestException("El usuario no pertenece a la comunidad");
        }
        
        if(c.esAdmin(userId)){
            throw new BadRequestException("El propietario no puede salir de la comunidad");
        }
        
        miembroComunidadRepository.eliminar(mc);
    }

    @Transactional
    public void eliminarMiembro(Long comunidadId, Long userId, Long adminId) {
        Comunidad c = comunidadRepository.buscarPorId(comunidadId);
        
        if(c == null){
            throw new NotFoundException("Comunidad no encontrada");
        }
        
        if (!c.esAdmin(adminId)) {
            throw new ForbiddenException("No tienes permisos para eliminar miembros");
        }
        
        MiembroComunidad mc = miembroComunidadRepository.buscarPorUsuarioYComunidad(userId, comunidadId);
        
        if(mc == null){
            throw new BadRequestException("El usuario no pertenece a la comunidad");
        }
        
        if (mc.getRol() == RolComunidad.OWNER) {
            throw new BadRequestException("No puedes eliminar a un propietario");
        }

        miembroComunidadRepository.eliminar(mc);   
        
    }

    @Transactional
    public void aceptarInvitacion(Long invitacionId, Long userId) {
        InvitacionComunidad inv = invitacionRepository.buscarPorId(invitacionId);

        if(inv == null) {
            throw new NotFoundException("Invitacion no existe");
        }

        if(!inv.getUsuarioInvitado().getId().equals(userId)){
            throw new ForbiddenException("No autorizado");
        }

        if(inv.getEstado() != EstadoInvitacion.PENDIENTE){
            throw new ClientErrorException("La invitacion ya fue procesada", Response.Status.CONFLICT);
        }

        inv.setEstado(EstadoInvitacion.ACEPTADA);
        invitacionRepository.actualizar(inv);

        MiembroComunidad existente = miembroComunidadRepository.buscarPorUsuarioYComunidad(
                inv.getUsuarioInvitado().getId(), inv.getComunidad().getId()
        );
        if (existente == null) {
            MiembroComunidad miembro = new MiembroComunidad();
            miembro.setComunidad(inv.getComunidad());
            miembro.setUsuario(inv.getUsuarioInvitado());
            miembro.setRol(RolComunidad.MEMBER);
            miembroComunidadRepository.guardar(miembro);
        }
    }

    @Transactional
    public void rechazarInvitacion(Long invitacionId, Long userId) {
        InvitacionComunidad inv = invitacionRepository.buscarPorId(invitacionId);

        if(inv == null) {
            throw new NotFoundException("Invitacion no existe");
        }

        if(!inv.getUsuarioInvitado().getId().equals(userId)){
            throw new ForbiddenException("No autorizado");
        }

        if(inv.getEstado() != EstadoInvitacion.PENDIENTE){
            throw new ClientErrorException("La invitacion ya fue procesada", Response.Status.CONFLICT);
        }

        inv.setEstado(EstadoInvitacion.RECHAZADA);
        
        invitacionRepository.actualizar(inv);
    }
    
    public List<Map<String, Object>> listarInvitacionesPendientes(Long userId) {
    List<InvitacionComunidad> invitaciones = invitacionRepository.listarPendientesPorUsuario(userId);
    List<Map<String, Object>> result = new java.util.ArrayList<>();
    for (InvitacionComunidad inv : invitaciones) {
        Map<String, Object> m = new java.util.HashMap<>();
        m.put("id", inv.getId());
        m.put("comunidadId", inv.getComunidad().getId());
        m.put("comunidadNombre", inv.getComunidad().getNombre());
        m.put("invitadoPor", inv.getOwner().getNombre() + " " + inv.getOwner().getApellido());
        result.add(m);
    }
    return result;
}

    
}

