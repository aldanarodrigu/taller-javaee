package com.appchat.service;

import com.appchat.dto.ComunidadDTO;
import com.appchat.dto.UsuarioResponseDTO;
import com.appchat.model.Comunidad;
import com.appchat.model.Usuario;
import com.appchat.model.enums.RolComunidad;
import com.appchat.repository.ComunidadRepository;
import com.appchat.dto.ComunidadResumenDTO;
import com.appchat.dto.ComunidadDetalleDTO;



import jakarta.inject.Inject;

import jakarta.transaction.Transactional;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class ComunidadService {
    
    @Inject
    private ComunidadRepository comunidadRepository;
    
    @Inject 
    private UsuarioService usuarioService;

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

    public void invitarUsuario(Long comunidadId, String username, Long ownerId) {
        
        Comunidad comunidad = comunidadRepository.buscarPorId(comunidadId);
        
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
           
        //Aca crear una InitacionAComunidad con estado pendiente asi despues el usuario puede aceptar
        
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
    
}
