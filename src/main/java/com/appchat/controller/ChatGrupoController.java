package com.appchat.controller;

import com.appchat.dto.ChatGrupoActualizacionDTO;
import com.appchat.dto.ChatGrupoRequestDTO;
import com.appchat.dto.ChatMiembrosRequestDTO;
import com.appchat.dto.ChatRolGrupoRequestDTO;
import com.appchat.dto.ChatResumenDTO;
import com.appchat.service.ChatService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/chat")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatGrupoController {

    @Inject
    private ChatService service;

    @Context
    private ContainerRequestContext requestContext;

    @POST
    public Response crearGrupo(@Valid ChatGrupoRequestDTO request) {
        Long usuarioId = getUsuarioId();

        ChatResumenDTO chat = service.crearGrupo(usuarioId, request);

        return Response.status(Response.Status.CREATED).entity(chat).build();
    }

    @POST
    @Path("/{id}/miembros")
    public Response agregarMiembros(@PathParam("id") Long chatId, @Valid ChatMiembrosRequestDTO request) {
        Long usuarioId = getUsuarioId();

        ChatResumenDTO chat = service.agregarMiembrosAGrupo(chatId, usuarioId, request);

        return Response.ok(chat).build();
    }

    @PUT
    @Path("/{id}")
    public Response editarGrupo(@PathParam("id") Long chatId, @Valid ChatGrupoActualizacionDTO request) {
        Long usuarioId = getUsuarioId();

        ChatResumenDTO chat = service.editarGrupo(chatId, usuarioId, request);

        return Response.ok(chat).build();
    }

    @PUT
    @Path("/{id}/roles")
    public Response cambiarRolMiembro(@PathParam("id") Long chatId, @Valid ChatRolGrupoRequestDTO request) {
        Long usuarioId = getUsuarioId();

        ChatResumenDTO chat = service.cambiarRolMiembroAGrupo(chatId, usuarioId, request);

        return Response.ok(chat).build();
    }

    @DELETE
    @Path("/{id}/miembros/{userId}")
    public Response eliminarMiembro(@PathParam("id") Long chatId, @PathParam("userId") Long userId) {
        Long usuarioId = getUsuarioId();

        service.eliminarMiembroDeGrupo(chatId, usuarioId, userId);

        return Response.noContent().build();
    }

    private Long getUsuarioId() {
        Object userIdObj = requestContext.getProperty("userId");

        if (userIdObj == null) {
            throw new jakarta.ws.rs.NotAuthorizedException("No autenticado");
        }

        return (Long) userIdObj;
    }
}