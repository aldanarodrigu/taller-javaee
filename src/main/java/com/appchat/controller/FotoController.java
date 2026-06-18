package com.appchat.controller;

import com.appchat.service.UsuarioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import java.util.Arrays;


@MultipartConfig(maxFileSize = 5242880)

@jakarta.ws.rs.Path("/usuarios/{id}/foto")
public class FotoController {

    @Inject
    private UsuarioService usuarioService;

    @Context
    private ContainerRequestContext requestContext;

@POST
@Consumes(MediaType.MULTIPART_FORM_DATA)
@Produces(MediaType.APPLICATION_JSON)
public Response subirFoto(
        @PathParam("id") Long id,
        byte[] body,
        @Context jakarta.ws.rs.core.HttpHeaders headers) {

    Long userId = (Long) requestContext.getProperty("userId");
    if (!userId.equals(id)) {
        return Response.status(Response.Status.FORBIDDEN).build();
    }

    try {
        // extraer el contenido del multipart manualmente
        String contentType = headers.getHeaderString("Content-Type");
        String boundary = contentType.split("boundary=")[1];
        String bodyStr = new String(body);
        
        // encontrar los bytes de la imagen entre los boundaries
        int start = bodyStr.indexOf("\r\n\r\n") + 4;
        int end = bodyStr.lastIndexOf("\r\n--" + boundary);
        byte[] imageBytes = java.util.Arrays.copyOfRange(body, start, end);
        
        // detectar tipo de imagen
        String imgContentType = "image/jpeg";
        if (bodyStr.contains("Content-Type: ")) {
            imgContentType = bodyStr.split("Content-Type: ")[1].split("\r\n")[0].trim();
        }
        
        String base64 = "data:" + imgContentType + ";base64," 
                + java.util.Base64.getEncoder().encodeToString(imageBytes);
        
        usuarioService.actualizarFoto(id, base64);
        
        return Response.ok("{\"url\":\"" + base64 + "\"}").build();
    } catch (Exception e) {
        return Response.serverError().entity("Error: " + e.getMessage()).build();
    }
}

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null) return ".jpg";
        int idx = nombreArchivo.lastIndexOf('.');
        return idx >= 0 ? nombreArchivo.substring(idx) : ".jpg";
    }
}