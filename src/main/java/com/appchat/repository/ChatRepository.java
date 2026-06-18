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

    // ─── Mensajes fijados (pin) ───────────────────────────────────────────────

    public void asegurarTablaMensajesFijados() {
        em.createNativeQuery("""
            CREATE TABLE IF NOT EXISTS mensajes_fijados (
                chat_id BIGINT NOT NULL,
                mensaje_id BIGINT NOT NULL,
                fijado_por BIGINT NULL,
                fecha_fijado TIMESTAMP NOT NULL DEFAULT NOW(),
                PRIMARY KEY (chat_id, mensaje_id)
            )
        """).executeUpdate();

        // Migración defensiva: si la tabla ya existía con esquema antiguo, agregar columnas faltantes.
        em.createNativeQuery("ALTER TABLE mensajes_fijados ADD COLUMN IF NOT EXISTS fijado_por BIGINT NULL")
                .executeUpdate();
        em.createNativeQuery("ALTER TABLE mensajes_fijados ADD COLUMN IF NOT EXISTS fecha_fijado TIMESTAMP NOT NULL DEFAULT NOW()")
                .executeUpdate();
        // Compatibilidad con columnas de esquema legado.
        em.createNativeQuery("""
            DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM information_schema.columns
                    WHERE table_schema = 'public'
                      AND table_name = 'mensajes_fijados'
                      AND column_name = 'fechafijado'
                ) THEN
                    ALTER TABLE mensajes_fijados ALTER COLUMN fechafijado SET DEFAULT NOW();
                END IF;
            END $$;
        """)
                .executeUpdate();
    }

    public void fijarMensaje(Long chatId, Long mensajeId, Long usuarioId) {
        asegurarTablaMensajesFijados();
        // Compatibilidad con esquemas antiguos sin constraint UNIQUE/PK.
        em.createNativeQuery("DELETE FROM mensajes_fijados WHERE chat_id = ?1 AND mensaje_id = ?2")
                .setParameter(1, chatId)
                .setParameter(2, mensajeId)
                .executeUpdate();

        try {
            em.createNativeQuery("""
                INSERT INTO mensajes_fijados (chat_id, mensaje_id, fijado_por, fecha_fijado)
                VALUES (?1, ?2, ?3, NOW())
            """)
                    .setParameter(1, chatId)
                    .setParameter(2, mensajeId)
                    .setParameter(3, usuarioId)
                    .executeUpdate();
        } catch (RuntimeException ex) {
            // Fallback para tablas heredadas con columnas antiguas (fechafijado/usuarioid).
            try {
                em.createNativeQuery("""
                    INSERT INTO mensajes_fijados (chat_id, mensaje_id, fijado_por, fecha_fijado, fechafijado, usuarioid)
                    VALUES (?1, ?2, ?3, NOW(), NOW(), ?3)
                """)
                        .setParameter(1, chatId)
                        .setParameter(2, mensajeId)
                        .setParameter(3, usuarioId)
                        .executeUpdate();
            } catch (RuntimeException ignored) {
                em.createNativeQuery("""
                    INSERT INTO mensajes_fijados (chat_id, mensaje_id, fijado_por, fecha_fijado, fechafijado)
                    VALUES (?1, ?2, ?3, NOW(), NOW())
                """)
                        .setParameter(1, chatId)
                        .setParameter(2, mensajeId)
                        .setParameter(3, usuarioId)
                        .executeUpdate();
            }
        }
    }

    public void desfijarMensaje(Long chatId, Long mensajeId) {
        asegurarTablaMensajesFijados();
        em.createNativeQuery("DELETE FROM mensajes_fijados WHERE chat_id = ?1 AND mensaje_id = ?2")
                .setParameter(1, chatId)
                .setParameter(2, mensajeId)
                .executeUpdate();
    }

    public List<Mensaje> buscarMensajesFijados(Long chatId) {
        asegurarTablaMensajesFijados();
        @SuppressWarnings("unchecked")
        List<Number> ids = em.createNativeQuery(
            "SELECT mensaje_id FROM mensajes_fijados WHERE chat_id = ?1 ORDER BY fecha_fijado ASC")
            .setParameter(1, chatId)
            .getResultList();

        if (ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        List<Long> mensajeIds = ids.stream().map(Number::longValue).toList();
        return em.createQuery(
            "SELECT m FROM Mensaje m "
            + "JOIN FETCH m.emisor "
            + "WHERE m.chat.id = :chatId AND m.id IN :ids "
            + "ORDER BY m.fechaEnvio ASC",
            Mensaje.class)
            .setParameter("chatId", chatId)
            .setParameter("ids", mensajeIds)
            .getResultList();
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