package com.appchat.websocket;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.websocket.Session;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
public class ChatHub {

    private static final Logger log = Logger.getLogger(ChatHub.class.getName());

    private final Map<Long, Set<Session>> sesiones = new ConcurrentHashMap<>();

    public void registrar(Long userId, Session session) {
        sesiones.computeIfAbsent(userId, k -> ConcurrentHashMap.newKeySet()).add(session);
    }

    public void remover(Long userId, Session session) {
        Set<Session> userSessions = sesiones.get(userId);
        if (userSessions != null) {
            userSessions.remove(session);
            if (userSessions.isEmpty()) {
                sesiones.remove(userId);
            }
        }
    }

    public void enviarAUsuario(Long userId, String mensaje) {
        Set<Session> sessions = sesiones.get(userId);
        
        if (sessions == null) 
            return;

        for (Session s : Set.copyOf(sessions)) {
            
            if(!s.isOpen()){ //para evitar acumular sesiones muertas
                remover(userId,s);
                continue;
            }
            
            s.getAsyncRemote().sendText(mensaje, result -> {
                if (!result.isOK()){
                    log.log(Level.WARNING, "Fallo al enviar a usuario " + userId, result.getException());
                    remover(userId, s);
                }
            });
        }
    }

    public Set<Session> obtenerSesiones(Long userId) {
        return sesiones.getOrDefault(userId, Set.of());
    }

    public int cantidadSesiones() {
        return sesiones.values().stream().mapToInt(Set::size).sum();
    }
    
    public boolean estaConectado(Long userId) {
        Set<Session> sessions = sesiones.get(userId);
        return sessions != null && !sessions.isEmpty();
    }
}