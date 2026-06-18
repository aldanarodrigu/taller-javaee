package com.appchat.controller;

import com.appchat.dto.AdjuntoUploadRequestDTO;
import com.appchat.dto.ChatDirectoRequestDTO;
import com.appchat.dto.ChatResumenDTO;
import com.appchat.dto.HistorialMensajesDTO;
import com.appchat.dto.MensajeDTO;
import com.appchat.service.ChatService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.*;

import java.util.List;

@Path("/chats")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChatController {

    @Inject
    private ChatService service;

    @Context
    private ContainerRequestContext requestContext;

    @GET
    @Path("/comunidad/{comunidadId}")
    public Response listarChats(@PathParam("comunidadId") Long comunidadId) {
        Long usuarioId = getUsuarioId();

        List<ChatResumenDTO> chats = service.listarChatsDeComunidad(usuarioId, comunidadId);
        
        return Response.ok(chats).build();
    }

    @GET
    @Path("/comunidad/{id}")
    public Response listarChatsPorComunidad(@PathParam("id") Long comunidadId) {
        Long usuarioId = getUsuarioId();

        List<ChatResumenDTO> chats = service.listarChatsDelUsuarioEnComunidad(usuarioId, comunidadId);

        return Response.ok(chats).build();
    }

    @GET
    @Path("/{id}/mensajes")
    public Response historialMensajes(@PathParam("id") Long chatId, @QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("20") int size) {
        Long usuarioId = getUsuarioId();

        HistorialMensajesDTO historial = service.obtenerHistorialMensajes(chatId, usuarioId, page, size);

        return Response.ok(historial).build();
    }

    @POST
    public Response crearOAbrirChatDirecto(@Valid ChatDirectoRequestDTO request) {
        Long usuarioId = getUsuarioId();

        ChatResumenDTO chat = service.crearOAbrirChatDirecto(usuarioId, request.getUsuarioDestinoId(), request.getComunidadId());

        return Response.status(Response.Status.CREATED).entity(chat).build();
    }

    @POST
    @Path("/{id}/adjuntos")
    public Response subirAdjunto(@PathParam("id") Long chatId, @Valid AdjuntoUploadRequestDTO request) {
        Long usuarioId = getUsuarioId();
        MensajeDTO dto = service.enviarAdjunto(chatId, usuarioId, request);
        return Response.status(Response.Status.CREATED).entity(dto).build();
    }

    @GET
    @Path("/adjuntos/{mensajeId}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response descargarAdjunto(@PathParam("mensajeId") Long mensajeId) {
        Long usuarioId = getUsuarioId();
        ChatService.AdjuntoDescarga descarga = service.obtenerAdjunto(mensajeId, usuarioId);

        String disposition = descarga.inline ? "inline" : "attachment";
        return Response.ok(descarga.bytes, descarga.mimeType)
                .header("Content-Disposition", disposition + "; filename=\"" + descarga.fileName + "\"")
                .header("Cache-Control", "private, max-age=120")
                .build();
    }

    private Long getUsuarioId() {
        Object userIdObj = requestContext.getProperty("userId");

        if (userIdObj == null) {
            throw new NotAuthorizedException("No autenticado");
        }

        return (Long) userIdObj;
    }
}