package com.appchat.controller;

import com.appchat.dto.ComunidadDTO;
import com.appchat.dto.InvitacionDTO;
import com.appchat.dto.UsuarioResponseDTO;
import com.appchat.model.Comunidad;
import com.appchat.service.ComunidadService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/comunidades")
@Produces(MediaType.APPLICATION_JSON) 
@Consumes(MediaType.APPLICATION_JSON) 
public class ComunidadController {
    
    @Inject
    private ComunidadService comunidadService;
    
    @Context
    private ContainerRequestContext requestContext;
    
    @POST
    public Response crearComunidad(@Valid ComunidadDTO dto) {

        Long userId = (Long) requestContext.getProperty("userId"); //trae el usuario logueado

        Comunidad comunidad = comunidadService.crearComunidad(dto, userId);

        return Response.ok().build();
    }
    
    @POST
    @Path("/{id}/invitar")
    public Response invitarUsuario(@PathParam("id") Long comunidadId, @Valid InvitacionDTO invitacion){
        
        Long ownerId = (Long) requestContext.getProperty("userId");
        
        comunidadService.invitarUsuario(comunidadId, invitacion.getUsername(), ownerId);
        
        return Response.ok("Invitacion Enviada").build();
    }
    
    @GET
    @Path("/{id}/miembros")
    public Response listarMimebros(@PathParam("id") Long comunidadId){
        List<UsuarioResponseDTO> miembros = comunidadService.listarMiembros(comunidadId);
        return Response.ok(miembros).build();
    }
    
}
