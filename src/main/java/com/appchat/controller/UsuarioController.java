package com.appchat.controller;

import com.appchat.dto.UsuarioResponseDTO;
import com.appchat.dto.ActualizarUsuarioDTO;
import com.appchat.dto.ActualizarEstadoDTO;
import com.appchat.service.UsuarioService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;

@Path("/usuarios")
@Produces(MediaType.APPLICATION_JSON) // todo lo que devuelva este controller es JSON 
@Consumes(MediaType.APPLICATION_JSON) // y recibe JSON tambien
public class UsuarioController {

    @Inject
    private UsuarioService service;

    @Context
    private ContainerRequestContext requestContext;
    
    @GET
    public Response listarUsuarios() {
        List<UsuarioResponseDTO> usuarios = service.listarUsuarios();
        return Response.ok(usuarios).build();
    }

    @GET
    @Path("/buscar")
    public Response buscarUsuarios(@QueryParam("q") @NotBlank String q) {
        List<UsuarioResponseDTO> usuarios = service.buscarUsuarios(q);
        return Response.ok(usuarios).build();
    }

    @GET
    @Path("/{id}")
    public Response obtenerUsuario(@PathParam("id") Long id) {
        UsuarioResponseDTO usuario = service.obtenerUsuarioPorId(id);

        if (usuario == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Usuario no encontrado").build();
        }

        return Response.ok(usuario).build();
    }

    @PUT
    @Path("/{id}")
    public Response actualizarUsuario(@PathParam("id") Long id, @Valid ActualizarUsuarioDTO dto) {
        UsuarioResponseDTO usuarioActualizado = service.actualizarUsuario(id, dto);

        if (usuarioActualizado == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Usuario no encontrado").build();
        }

        return Response.ok(usuarioActualizado).build();
    }

    @PUT
    @Path("/{id}/publicKey")
    @Consumes(MediaType.TEXT_PLAIN)
    public Response guardarPublicKey(@PathParam("id") Long id, String publicKey) {
        Long userIdAutenticado = (Long) requestContext.getProperty("userId");
        if (!id.equals(userIdAutenticado)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }
        service.guardarPublicKey(id, publicKey);
        return Response.noContent().build();
    }

    @PUT
    @Path("/{id}/estado")
    public Response actualizarEstado(@PathParam("id") Long id, @Valid ActualizarEstadoDTO dto) {
        
        Long userIdAutenticado = (Long) requestContext.getProperty("userId");
        
        UsuarioResponseDTO usuarioAEditar = service.obtenerUsuarioPorId(id);
        
        if (usuarioAEditar == null) {
            return Response.status(Response.Status.NOT_FOUND).entity("Usuario no encontrado").build();
        }
        
        if (!usuarioAEditar.getId().equals(userIdAutenticado)) {
            return Response.status(Response.Status.FORBIDDEN).entity("No tienes permiso para modificar el estado de otro usuario").build();
        }
        
        UsuarioResponseDTO usuario = service.actualizarEstado(id, dto.getEstado());
       
        return Response.ok(usuario).build();
    }
}
