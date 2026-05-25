package com.appchat.repository;

import com.appchat.model.Chat;
import com.appchat.model.Mensaje;
import com.appchat.model.enums.TipoChat;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@ApplicationScoped
public class ChatRepository {

    @PersistenceContext(unitName = "appchatPU")
    private EntityManager em;

    public Chat buscarChatPorId(Long chatId) {
        return em.find(Chat.class, chatId);
    }

    public Chat buscarChatDirectoEntreUsuarios(Long u1, Long u2, Long comunidadId) {
        List<Chat> resultados = em.createQuery("""
            SELECT c FROM Chat c
            JOIN c.participantes p
            WHERE c.tipo = :tipo
              AND c.comunidad.id = :comunidadId
            GROUP BY c
            HAVING COUNT(p) = 2
               AND SUM(CASE WHEN p.usuario.id IN (:u1, :u2) THEN 1 ELSE 0 END) = 2
        """, Chat.class)
        .setParameter("tipo", TipoChat.DIRECTO)
        .setParameter("u1", u1)
        .setParameter("u2", u2)
        .setParameter("comunidadId", comunidadId)
        .getResultList();

        return resultados.isEmpty() ? null : resultados.get(0);
    }

    public List<Chat> listarChatsDeUsuario(Long usuarioId) {
        return em.createQuery("""
            SELECT DISTINCT c FROM Chat c
            JOIN c.participantes p
            WHERE p.usuario.id = :usuarioId
            ORDER BY c.fechaCreacion DESC
        """, Chat.class)
        .setParameter("usuarioId", usuarioId)
        .getResultList();
    }

    public void guardarChat(Chat chat) {
        em.persist(chat);
    }

    public void guardarMensaje(Mensaje mensaje) {
        em.persist(mensaje);
    }

    public Mensaje buscarMensajePorId(Long mensajeId) {
        return em.find(Mensaje.class, mensajeId);
    }

    public Mensaje buscarUltimoMensaje(Long chatId) {
        try {
            return em.createQuery(
                    "SELECT m FROM Mensaje m "
                    + "JOIN FETCH m.emisor "
                    + "WHERE m.chat.id = :chatId "
                    + "ORDER BY m.fechaEnvio DESC",
                    Mensaje.class)
                    .setParameter("chatId", chatId)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<Mensaje> buscarMensajesPagina(Long chatId, int offset, int limit) {
        return em.createQuery(
                "SELECT m FROM Mensaje m "
                + "JOIN FETCH m.emisor "
                + "WHERE m.chat.id = :chatId "
                + "ORDER BY m.fechaEnvio ASC",
                Mensaje.class)
                .setParameter("chatId", chatId)
                .setFirstResult(offset)
                .setMaxResults(limit)
                .getResultList();
    }

    public List<Mensaje> buscarMensajesEnviadosPendientesEntregaParaUsuarioEnChat(Long chatId, Long usuarioId) {
        return em.createQuery(
                "SELECT m FROM Mensaje m "
                + "JOIN FETCH m.emisor "
                + "WHERE m.chat.id = :chatId "
                + "AND m.emisor.id <> :usuarioId "
                + "AND m.estado = :estadoEnviado "
                + "ORDER BY m.fechaEnvio ASC",
                Mensaje.class)
                .setParameter("chatId", chatId)
                .setParameter("usuarioId", usuarioId)
                .setParameter("estadoEnviado", com.appchat.model.enums.EstadoMensaje.ENVIADO)
                .getResultList();
    }

    public List<Mensaje> buscarMensajesPendientesLecturaParaUsuarioEnChat(Long chatId, Long usuarioId) {
        return em.createQuery(
                "SELECT m FROM Mensaje m "
                + "JOIN FETCH m.emisor "
                + "WHERE m.chat.id = :chatId "
                + "AND m.emisor.id <> :usuarioId "
                + "AND m.estado <> :estadoLeido "
                + "ORDER BY m.fechaEnvio ASC",
                Mensaje.class)
                .setParameter("chatId", chatId)
                .setParameter("usuarioId", usuarioId)
                .setParameter("estadoLeido", com.appchat.model.enums.EstadoMensaje.LEIDO)
                .getResultList();
    }

    public long contarMensajes(Long chatId) {
        return em.createQuery(
                "SELECT COUNT(m) FROM Mensaje m WHERE m.chat.id = :chatId",
                Long.class)
                .setParameter("chatId", chatId)
                .getSingleResult();
    }    

    public void guardarReaccion(com.appchat.model.Reaccion reaccion) {
        em.persist(reaccion);
    }

    public java.util.List<com.appchat.model.Reaccion> buscarReaccionesPorMensaje(Long mensajeId) {
        return em.createQuery(
                "SELECT r FROM Reaccion r JOIN FETCH r.usuario WHERE r.mensaje.id = :mensajeId ORDER BY r.fecha ASC",
                com.appchat.model.Reaccion.class)
                .setParameter("mensajeId", mensajeId)
                .getResultList();
    }

    public void flush() {
        em.flush();
    }
}