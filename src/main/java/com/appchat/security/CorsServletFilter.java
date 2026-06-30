package com.appchat.security;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Servlet Filter que agrega headers CORS a TODAS las respuestas,
 * incluyendo errores 4xx/5xx generados fuera de JAX-RS.
 * Cubre casos que el ContainerResponseFilter de JAX-RS no alcanza.
 */
@WebFilter("/*")
public class CorsServletFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {}

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) request;
        HttpServletResponse httpRes = (HttpServletResponse) response;

        httpRes.setHeader("Access-Control-Allow-Origin", "*");
        httpRes.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, HEAD");
        httpRes.setHeader("Access-Control-Allow-Headers",
                "origin, content-type, accept, authorization, ngrok-skip-browser-warning");
        httpRes.setHeader("Access-Control-Max-Age", "1209600");

        // Responder inmediatamente a preflight OPTIONS sin pasar al servlet/JAX-RS
        if ("OPTIONS".equalsIgnoreCase(httpReq.getMethod())) {
            httpRes.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {}
}
