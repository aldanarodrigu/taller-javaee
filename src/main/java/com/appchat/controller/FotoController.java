package com.appchat.controller;

import com.appchat.service.UsuarioService;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
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
            @Context jakarta.servlet.http.HttpServletRequest request) {

        Long userId = (Long) requestContext.getProperty("userId");
        if (!userId.equals(id)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            jakarta.servlet.http.Part part = request.getPart("foto");
            String nombreArchivo = UUID.randomUUID().toString() + obtenerExtension(part.getSubmittedFileName());

            String uploadDir = System.getProperty("com.sun.aas.instanceRoot")
                    + File.separator + "uploads";
            Files.createDirectories(java.nio.file.Paths.get(uploadDir));

            java.nio.file.Path destino = java.nio.file.Paths.get(uploadDir, nombreArchivo);
            Files.copy(part.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            String url = "/appchat/uploads/" + nombreArchivo;
            usuarioService.actualizarFoto(id, url);

            return Response.ok("{\"url\":\"" + url + "\"}").build();
        } catch (Exception e) {
            return Response.serverError().entity("Error al subir foto: " + e.getMessage()).build();
        }
    }

    private String obtenerExtension(String nombreArchivo) {
        if (nombreArchivo == null) return ".jpg";
        int idx = nombreArchivo.lastIndexOf('.');
        return idx >= 0 ? nombreArchivo.substring(idx) : ".jpg";
    }
}