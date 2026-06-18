package com.appchat.repository;

import com.appchat.model.Chat;
import com.appchat.model.Mensaje;
import com.appchat.model.enums.TipoChat;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<Chat> listarChatsDeUsuarioEnComunidad(Long usuarioId, Long comunidadId) {
        return em.createQuery("""
            SELECT DISTINCT c FROM Chat c
            JOIN c.participantes p
            WHERE p.usuario.id = :usuarioId
              AND c.comunidad.id = :comunidadId
            ORDER BY c.fechaCreacion DESC
        """, Chat.class)
        .setParameter("usuarioId", usuarioId)
        .setParameter("comunidadId", comunidadId)
        .getResultList();
    }

    public void guardarChat(Chat chat) {
        em.persist(chat);
    }

    public void eliminarChat(Chat chat) {
        if (chat == null) {
            return;
        }
        Chat managed = em.contains(chat) ? chat : em.merge(chat);
        em.remove(managed);
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

    // ─── Reacciones ───────────────────────────────────────────────────────────

    public static class ReaccionResultado {
        public final boolean removido;
        public final com.appchat.dto.ReaccionDTO dto;
        public final Long chatId;
        public final List<Long> participanteIds;

        public ReaccionResultado(boolean removido, com.appchat.dto.ReaccionDTO dto,
                                  Long chatId, List<Long> participanteIds) {
            this.removido = removido;
            this.dto = dto;
            this.chatId = chatId;
            this.participanteIds = participanteIds;
        }
    }

    /**
     * Persiste o elimina (toggle) una reacción en su PROPIA transacción REQUIRES_NEW.
     * Commitea antes de retornar, garantizando persistencia
     * independientemente de cualquier transacción externa.
     */
    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public ReaccionResultado procesarReaccion(Long usuarioId, Long mensajeId, String tipo) {
        Mensaje mensaje = em.find(Mensaje.class, mensajeId);
        if (mensaje == null) {
            throw new jakarta.ws.rs.NotFoundException("Mensaje no encontrado: " + mensajeId);
        }

        boolean participa = mensaje.getChat().getListaParticipaciones()
                .stream().anyMatch(p -> p.getUsuario().getId().equals(usuarioId));
        if (!participa) {
            throw new jakarta.ws.rs.ForbiddenException("No participás en este chat");
        }

        Long chatId = mensaje.getChat().getId();
        List<Long> participanteIds = mensaje.getChat().getListaParticipaciones()
                .stream().map(p -> p.getUsuario().getId()).collect(Collectors.toList());

        // Toggle: si ya existe la misma reacción del mismo usuario, eliminarla
        java.util.List<com.appchat.model.Reaccion> existentes = em.createQuery(
                "SELECT r FROM Reaccion r WHERE r.mensaje.id = :mid AND r.usuario.id = :uid AND r.tipo = :tipo",
                com.appchat.model.Reaccion.class)
                .setParameter("mid", mensajeId)
                .setParameter("uid", usuarioId)
                .setParameter("tipo", tipo)
                .getResultList();

        if (!existentes.isEmpty()) {
            com.appchat.model.Reaccion existente = existentes.get(0);
            em.remove(em.contains(existente) ? existente : em.merge(existente));
            em.flush();

            com.appchat.dto.ReaccionDTO dto = new com.appchat.dto.ReaccionDTO();
            dto.setId(existente.getId());
            dto.setTipo(existente.getTipo());
            dto.setUsuarioId(usuarioId);
            return new ReaccionResultado(true, dto, chatId, participanteIds);
        }

        // Nueva reacción
        com.appchat.model.Usuario usuario = em.find(com.appchat.model.Usuario.class, usuarioId);
        com.appchat.model.Reaccion reaccion = new com.appchat.model.Reaccion();
        reaccion.setMensaje(mensaje);
        reaccion.setTipo(tipo);
        reaccion.setUsuario(usuario);
        em.persist(reaccion);
        em.flush();

        com.appchat.dto.ReaccionDTO dto = new com.appchat.dto.ReaccionDTO();
        dto.setId(reaccion.getId());
        dto.setTipo(reaccion.getTipo());
        dto.setUsuarioId(usuarioId);
        dto.setUsuarioNombre(usuario.getNombre());
        dto.setUsuarioApellido(usuario.getApellido());
        dto.setFecha(reaccion.getFecha());
        return new ReaccionResultado(false, dto, chatId, participanteIds);
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