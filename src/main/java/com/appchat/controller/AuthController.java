package com.appchat.controller;

import com.appchat.dto.LoginDTO;
import com.appchat.dto.UsuarioDTO;
import com.appchat.model.Usuario;
import com.appchat.security.JwtUtil;
import com.appchat.service.AuthService;

import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Collections;


@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthController {

    @Inject
    private AuthService authService;

    @POST
    @Path("/login")
    public Response login(@Valid LoginDTO dto) {
        Usuario u = authService.login(dto.getEmail(), dto.getPassword());

        String token = JwtUtil.generarToken(u.getId(), u.getEmail());

        return Response.ok(Collections.singletonMap("token", token)).build();    
    }
    
    @POST
    @Path("/registro")
    public Response registrarUsuario(@Valid UsuarioDTO dto) {

        Usuario nuevo = authService.registrarUsuario(dto.getEmail(), dto);

        return Response.status(Response.Status.CREATED).entity(Collections.singletonMap("id", nuevo.getId())).build();
    }
}    
