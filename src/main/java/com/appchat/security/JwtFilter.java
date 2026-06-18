package com.appchat.security;

import jakarta.annotation.Priority;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import jakarta.ws.rs.core.Response;
import java.util.List;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

@Provider
@Priority(Priorities.AUTHENTICATION)
public class JwtFilter implements ContainerRequestFilter {

    @Override
    public void filter(ContainerRequestContext requestContext) {

        if ("OPTIONS".equalsIgnoreCase(requestContext.getMethod())) {
            return;
        }

        String path = requestContext.getUriInfo().getPath();

        if (path.contains("auth/login") || path.contains("auth/registro")) {
            return;
        }

        String token = null;
        String authHeader = requestContext.getHeaderString("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            token = authHeader.substring("Bearer ".length());
        }
        if (token == null || token.isBlank()) {
            List<String> queryTokens = requestContext.getUriInfo().getQueryParameters().get("token");
            if (queryTokens != null && !queryTokens.isEmpty()) {
                token = queryTokens.get(0);
            }
        }
        if (token == null || token.isBlank()) {
            requestContext.abortWith(buildUnauthorized("Falta token"));
            return;
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(JwtUtil.getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String email = claims.getSubject();
            Long userId = claims.get("userId", Long.class);
            requestContext.setProperty("email", email);
            
            if (userId == null) {
                requestContext.abortWith(buildUnauthorized("Token inválido (sin userId)"));
                return;
            }

            requestContext.setProperty("userId", userId);


        } catch (Exception e) {
            requestContext.abortWith(buildUnauthorized("Token inválido"));
        }
    }

    private Response buildUnauthorized(String message) {
        return Response.status(401)
                .entity(message)
                .header("Access-Control-Allow-Origin", "*")
                .header("Access-Control-Allow-Headers", "origin, content-type, accept, authorization")
                .header("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD")
                .build();
    }
}