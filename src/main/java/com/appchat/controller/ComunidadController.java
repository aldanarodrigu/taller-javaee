package com.appchat.controller;

import com.appchat.dto.ComunidadDTO;
import com.appchat.dto.ComunidadDetalleDTO;
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
import com.appchat.dto.ComunidadResumenDTO;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;

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

        return Response.status(Response.Status.CREATED).build();
    }
    
    @POST
    @Path("/{id}/invitar")
    public Response invitarUsuario(@PathParam("id") Long comunidadId, @Valid InvitacionDTO invitacion){
        
        Long ownerId = (Long) requestContext.getProperty("userId");
        
        comunidadService.invitarUsuario(comunidadId, invitacion.getUsername(), ownerId);
        
        return Response.ok("Invitacion Enviada").build();
    }
    
   @GET
    @Path("/{id}")
    public Response obtenerComunidad(@PathParam("id") Long id) {
        ComunidadDetalleDTO dto = comunidadService.obtenerDetalleComunidad(id);
        return Response.ok(dto).build();
    }

    @PUT
    @Path("/{id}")
    public Response editarComunidad(@PathParam("id") Long id, @Valid ComunidadDTO dto) {
        Long userId = (Long) requestContext.getProperty("userId");
        Comunidad c = comunidadService.editarComunidad(id, dto, userId);
        return Response.ok(c).build();
    }

    @DELETE
    @Path("/{id}")
    public Response eliminarComunidad(@PathParam("id") Long id) {
        Long userId = (Long) requestContext.getProperty("userId");
        comunidadService.eliminarComunidad(id, userId);
        return Response.noContent().build();
    }
    
    @GET
    @Path("/{id}/miembros")
    public Response listarMimebros(@PathParam("id") Long comunidadId){
        List<UsuarioResponseDTO> miembros = comunidadService.listarMiembros(comunidadId);
        return Response.ok(miembros).build();
    }
    
    @GET
    public Response listarComunidades() {
        Long userId = (Long) requestContext.getProperty("userId");
        List<ComunidadResumenDTO> comunidades = comunidadService.listarComunidadesDelUsuario(userId);
        return Response.ok(comunidades).build();
    }
    
    @DELETE
    @Path("/{id}/salir")
    public Response salirDeComunidad(@PathParam("id") Long comunidadId){
        Long userId = (Long) requestContext.getProperty("userId");
        
        comunidadService.salirComunidad(comunidadId, userId);   
        
        return Response.noContent().build();
    }
    
    @DELETE
    @Path("/{id}/mimebros/{idm}")
    public Response eliminarMiembro(@PathParam("id") Long comunidadId, @PathParam("idm") Long userId){
        
        Long adminId = (Long) requestContext.getProperty("userId");
        
        comunidadService.eliminarMiembro(comunidadId, userId, adminId);   
        
        return Response.noContent().build();
    }
    @GET
@Path("/invitaciones/pendientes")
public Response listarInvitacionesPendientes() {
    Long userId = (Long) requestContext.getProperty("userId");
    return Response.ok(comunidadService.listarInvitacionesPendientes(userId)).build();
}
    
    @PUT
    @Path("/invitaciones/{invitacionId}/aceptar")
    public Response aceptar(@PathParam("invitacionId") Long invitacionId, @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        comunidadService.aceptarInvitacion(invitacionId, userId);
        return Response.ok().build();
    }

    @PUT
    @Path("/invitaciones/{invitacionId}/rechazar")
    public Response rechazar(@PathParam("invitacionId") Long invitacionId, @Context ContainerRequestContext requestContext) {
        Long userId = (Long) requestContext.getProperty("userId");
        comunidadService.rechazarInvitacion(invitacionId, userId);
        return Response.ok().build();
    }
    
}
