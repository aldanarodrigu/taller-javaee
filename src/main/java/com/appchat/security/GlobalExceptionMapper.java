package com.appchat.security;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.Map;

/**
 * Captura todas las excepciones no manejadas y devuelve una respuesta JSON.
 * Al pasar por JAX-RS, el CorsFilter aplica los headers Access-Control-Allow-Origin
 * incluso en respuestas de error 500.
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Throwable> {

    @Override
    public Response toResponse(Throwable exception) {
        if (exception instanceof WebApplicationException wae) {
            Response original = wae.getResponse();
            String msg = exception.getMessage() != null ? exception.getMessage() : "Error";
            return Response.status(original.getStatus())
                    .entity(Map.of("error", msg))
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        }

        // Excepción inesperada (ej. fallo JDBC, NPE): log y devuelve 500 JSON
        System.err.println("[GlobalExceptionMapper] Excepción no manejada: " + exception);
        return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("error", "Error interno del servidor"))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
