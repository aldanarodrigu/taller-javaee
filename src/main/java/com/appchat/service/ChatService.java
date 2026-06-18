package com.appchat.service;

import com.appchat.dto.ChatGrupoActualizacionDTO;
import com.appchat.dto.ChatGrupoRequestDTO;
import com.appchat.dto.ChatMiembrosRequestDTO;
import com.appchat.dto.ChatRolGrupoRequestDTO;
import com.appchat.dto.ChatResumenDTO;
import com.appchat.dto.HistorialMensajesDTO;
import com.appchat.dto.MensajeDTO;
import com.appchat.dto.MensajeWSDTO;
import com.appchat.model.Chat;
import com.appchat.model.Comunidad;
import com.appchat.model.Mensaje;
import com.appchat.model.Participa;
import com.appchat.model.Usuario;
import com.appchat.model.enums.EstadoMensaje;
import com.appchat.model.enums.RolGrupo;
import com.appchat.model.enums.TipoChat;
import com.appchat.model.enums.TipoMensaje;
import com.appchat.repository.ChatRepository;
import com.appchat.repository.UsuarioRepository;
import com.appchat.websocket.ChatHub;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@ApplicationScoped
@Transactional(Transactional.TxType.REQUIRED)
public class ChatService {

    @Inject
    private ChatRepository chatRepository;

    @Inject
    private UsuarioRepository usuarioRepository;
    
    @Inject 
    private ChatHub chatHub;
    
    @Inject
    private ComunidadService comunidadService;
    
    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Transactional
    public MensajeDTO enviarMensaje(Long chatId, Long emisorId, String contenido) {
        return enviarMensaje(chatId, emisorId, contenido, null);
    }

    @Transactional
    public MensajeDTO enviarMensaje(Long chatId, Long emisorId, String contenido, Long parentMensajeId) {

        Chat chat = chatRepository.buscarChatPorId(chatId);
        
        if (chat == null) {
            throw new NotFoundException("Chat no existe.");
        }

        validarParticipacion(chat, emisorId);

        Usuario emisor = usuarioRepository.buscarPorId(emisorId);

        if (emisor == null) {
            throw new NotFoundException("Usuario no existe.");
    }

        Mensaje mensaje = new Mensaje();
        mensaje.setContenido(contenido);
        mensaje.setChat(chat);      
        mensaje.setEmisor(emisor);
        mensaje.setTipo(TipoMensaje.TEXTO);
        mensaje.setEstado(EstadoMensaje.ENVIADO);
        if (parentMensajeId != null) {
            Mensaje parent = chatRepository.buscarMensajePorId(parentMensajeId);
            if (parent == null) {
                throw new NotFoundException("Mensaje padre no encontrado");
            }
            if (!parent.getChat().getId().equals(chatId)) {
                throw new BadRequestException("parentId no pertenece al mismo chat");
            }
            mensaje.setParentMessage(parent);
        }
        
        chatRepository.guardarMensaje(mensaje);

        chatRepository.flush();

        MensajeDTO dto = mapearMensaje(mensaje);

        notificarChat(chat, mensaje, dto);
        
        return dto;
    }

    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public com.appchat.dto.ReaccionDTO agregarReaccion(Long usuarioId, Long mensajeId, String tipo) {
        // chatRepository.procesarReaccion usa REQUIRES_NEW → transacción propia que commitea antes de retornar
        com.appchat.repository.ChatRepository.ReaccionResultado resultado =
                chatRepository.procesarReaccion(usuarioId, mensajeId, tipo);

        // En este punto la transacción ya commitó. Enviamos WS de forma segura.
        String eventType = resultado.removido ? "REACTION_REMOVED" : "REACTION_ADDED";
        try {
            String json = mapper.writeValueAsString(java.util.Map.of(
                "type", eventType,
                "messageId", mensajeId,
                "chatId", resultado.chatId,
                "reaction", resultado.dto
            ));
            for (Long uid : resultado.participanteIds) {
                if (!chatHub.obtenerSesiones(uid).isEmpty()) {
                    chatHub.enviarAUsuario(uid, json);
                }
            }
        } catch (Exception e) {
            // El WS no afecta la persistencia (ya commitada)
        }
        return resultado.dto;
    }
    
    private void notificarChat(Chat chat, Mensaje mensaje, MensajeDTO dtoMensaje) {

        List<Long> destinatarios = obtenerUsuariosDelChat(chat, dtoMensaje.getEmisorId());
        boolean entregado = false;

        String json;
        try {
            json = mapper.writeValueAsString(dtoMensaje);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando mensaje", e);
        }

        for (Long userId : destinatarios) {
            if (!chatHub.obtenerSesiones(userId).isEmpty()) {
                chatHub.enviarAUsuario(userId, json);
                entregado = true;
            }
        }

        // Siempre notificar al emisor con el estado actual del mensaje
        if (entregado && mensaje.getEstado() == EstadoMensaje.ENVIADO) {
            mensaje.setEstado(EstadoMensaje.ENTREGADO);
            chatRepository.flush();
        }

        try {
            MensajeDTO dtoEmisor = mapearMensaje(mensaje);
            String jsonEmisor = mapper.writeValueAsString(dtoEmisor);
            chatHub.enviarAUsuario(mensaje.getEmisor().getId(), jsonEmisor);
        } catch (Exception e) {
            // No lanzar excepción: el mensaje ya fue guardado, solo falla la notificación
        }
    }
    
    private List<Long> obtenerUsuariosDelChat(Chat chat, Long emisorId) {
        return chat.getParticipantes().stream().map(Usuario::getId).filter(id -> !id.equals(emisorId)).toList();
    }
    
    @Transactional
    public List<ChatResumenDTO> listarChatsDelUsuario(Long usuarioId) {
        verificarUsuarioExiste(usuarioId);

        List<Chat> chats = chatRepository.listarChatsDeUsuario(usuarioId);
        List<ChatResumenDTO> respuestas = new ArrayList<>();

        for (Chat chat : chats) {
            respuestas.add(mapearResumen(chat, usuarioId));
        }

        return respuestas;
    }

    @Transactional
    public List<ChatResumenDTO> listarChatsDelUsuarioEnComunidad(Long usuarioId, Long comunidadId) {
        verificarUsuarioExiste(usuarioId);

        Comunidad comunidad = comunidadService.buscarPorId(comunidadId);
        if (comunidad == null) {
            throw new NotFoundException("Comunidad no existe");
        }

        List<Chat> chats = chatRepository.listarChatsDeUsuarioEnComunidad(usuarioId, comunidadId);
        List<ChatResumenDTO> respuestas = new ArrayList<>();

        for (Chat chat : chats) {
            respuestas.add(mapearResumen(chat, usuarioId));
        }

        return respuestas;
    }
    
    @Transactional
    public HistorialMensajesDTO obtenerHistorialMensajes(Long chatId, Long usuarioId, int page, int size) {
        Chat chat = chatRepository.buscarChatPorId(chatId);
        if (chat == null) {
            throw new NotFoundException("Chat no existe.");
        }
        validarParticipacion(chat, usuarioId);

        int paginaNormalizada = Math.max(page, 0);
        int tamanoNormalizado = size <= 0 ? 20 : size;
        long total = chatRepository.contarMensajes(chatId);

        // La pagina 0 devuelve los mensajes mas recientes para evitar que
        // los mensajes nuevos "desaparezcan" al refrescar historial.
        long inicioLong = Math.max(total - ((long) tamanoNormalizado * (paginaNormalizada + 1)), 0);
        long finLong = Math.max(total - ((long) tamanoNormalizado * paginaNormalizada), 0);

        int inicio = (int) inicioLong;
        int limite = (int) Math.max(finLong - inicioLong, 0);

        List<Mensaje> mensajes = limite > 0
                ? chatRepository.buscarMensajesPagina(chatId, inicio, limite)
                : java.util.Collections.emptyList();

        HistorialMensajesDTO respuesta = new HistorialMensajesDTO();
        respuesta.setMensajes(mapearMensajes(mensajes));
        respuesta.setPage(paginaNormalizada);
        respuesta.setSize(tamanoNormalizado);
        respuesta.setTotalElementos(total);
        respuesta.setTotalPaginas((int) Math.ceil((double) total / tamanoNormalizado));
        return respuesta;
    }

    @Transactional
    public ChatResumenDTO crearOAbrirChatDirecto(Long usuarioAutenticadoId, Long usuarioDestinoId, Long comunidadId) {
        
        if (usuarioAutenticadoId.equals(usuarioDestinoId)) {
            throw new ForbiddenException("No se puede crear un chat directo contigo mismo");
        }

        Usuario usuarioAutenticado = verificarUsuarioExiste(usuarioAutenticadoId);
        Usuario usuarioDestino = verificarUsuarioExiste(usuarioDestinoId);

        Comunidad comunidad = comunidadService.buscarPorId(comunidadId);
        if(comunidad == null){
            throw new NotFoundException("Comunidad no existe");
        }
        
        if (!comunidadService.sonMiembros(comunidadId, usuarioAutenticadoId, usuarioDestinoId)) {
            throw new ForbiddenException("Ambos usuarios deben pertenecer a la comunidad");
        }
        
        Chat chatExistente = chatRepository.buscarChatDirectoEntreUsuarios(usuarioAutenticadoId, usuarioDestinoId, comunidadId);
        if (chatExistente != null) {
            return mapearResumen(chatExistente, usuarioAutenticadoId);
        }

        Chat chatNuevo = new Chat();
        chatNuevo.setTipo(TipoChat.DIRECTO);
        chatNuevo.setComunidad(comunidad);

        chatNuevo.agregarParticipante(usuarioAutenticado, null);
        chatNuevo.agregarParticipante(usuarioDestino, null);

        chatRepository.guardarChat(chatNuevo);

        return mapearResumen(chatNuevo, usuarioAutenticadoId);
    }

    @Transactional
    public ChatResumenDTO crearGrupo(Long usuarioAutenticadoId, ChatGrupoRequestDTO request) {
        if (request == null) {
            throw new BadRequestException("Datos requeridos");
        }

        Usuario creador = verificarUsuarioExiste(usuarioAutenticadoId);

        Comunidad comunidad = comunidadService.buscarPorId(request.getComunidadId());
        if (comunidad == null) {
            throw new NotFoundException("Comunidad no existe");
        }

        Chat chatNuevo = new Chat();
        chatNuevo.setTipo(TipoChat.GRUPAL);
        chatNuevo.setNombre(request.getNombre());
        chatNuevo.setDescripcion(request.getDescripcion());
        chatNuevo.setFotoUrl(request.getFotoUrl());
        chatNuevo.setComunidad(comunidad);

        chatNuevo.agregarParticipante(creador, RolGrupo.ADMIN);

        Set<Long> idsIniciales = normalizarIds(request.getUsuarioIds());
        idsIniciales.remove(usuarioAutenticadoId);

        for (Long usuarioId : idsIniciales) {
            Usuario usuario = verificarUsuarioExiste(usuarioId);

            chatNuevo.agregarParticipante(usuario, RolGrupo.MIEMBRO);
        }

        chatRepository.guardarChat(chatNuevo);
        chatRepository.flush();

        return mapearResumen(chatNuevo, usuarioAutenticadoId);
    }

    @Transactional
    public ChatResumenDTO editarGrupo(Long chatId, Long usuarioSolicitanteId, ChatGrupoActualizacionDTO request) {
        if (request == null) {
            throw new BadRequestException("Datos requeridos");
        }

        Chat chat = chatRepository.buscarChatPorId(chatId);
        if (chat == null) {
            throw new NotFoundException("Chat no existe.");
        }

        if (chat.getTipo() != TipoChat.GRUPAL) {
            throw new BadRequestException("Solo se pueden editar grupos");
        }

        validarAdminGrupo(chat, usuarioSolicitanteId);

        boolean actualizado = false;

        if (request.getNombre() != null) {
            String nombre = request.getNombre().trim();
            if (nombre.isEmpty()) {
                throw new BadRequestException("nombre no puede estar vacío");
            }
            chat.setNombre(nombre);
            actualizado = true;
        }

        if (request.getDescripcion() != null) {
            chat.setDescripcion(request.getDescripcion().trim());
            actualizado = true;
        }

        if (request.getFotoUrl() != null) {
            chat.setFotoUrl(request.getFotoUrl().trim());
            actualizado = true;
        }

        if (!actualizado) {
            throw new BadRequestException("Debe enviar al menos un campo para actualizar");
        }

        chatRepository.flush();

        return mapearResumen(chat, usuarioSolicitanteId);
    }

    @Transactional
    public ChatResumenDTO cambiarRolMiembroAGrupo(Long chatId, Long usuarioSolicitanteId, ChatRolGrupoRequestDTO request) {
        if (request == null) {
            throw new BadRequestException("Datos requeridos");
        }

        Chat chat = chatRepository.buscarChatPorId(chatId);
        if (chat == null) {
            throw new NotFoundException("Chat no existe.");
        }

        if (chat.getTipo() != TipoChat.GRUPAL) {
            throw new BadRequestException("Solo se pueden cambiar roles en grupos");
        }

        validarAdminGrupo(chat, usuarioSolicitanteId);

        Long usuarioObjetivoId = request.getUsuarioId();
        RolGrupo nuevoRol = request.getRol();

        Participa participacion = chat.getListaParticipaciones().stream()
                .filter(p -> p.getUsuario().getId().equals(usuarioObjetivoId))
                .findFirst()
                .orElse(null);

        if (participacion == null) {
            throw new NotFoundException("Usuario no es miembro del grupo");
        }

        if (participacion.getRol() == nuevoRol) {
            return mapearResumen(chat, usuarioSolicitanteId);
        }

        if (participacion.getRol() == RolGrupo.ADMIN && nuevoRol == RolGrupo.MIEMBRO && esUnicoAdmin(chat, participacion.getUsuario().getId())) {
            throw new BadRequestException("El ultimo admin no puede perder su rol");
        }

        participacion.setRol(nuevoRol);

        chatRepository.flush();

        return mapearResumen(chat, usuarioSolicitanteId);
    }

    @Transactional
    public ChatResumenDTO agregarMiembrosAGrupo(Long chatId, Long usuarioSolicitanteId, ChatMiembrosRequestDTO request) {
        if (request == null) {
            throw new BadRequestException("Datos requeridos");
        }

        Chat chat = chatRepository.buscarChatPorId(chatId);
        if (chat == null) {
            throw new NotFoundException("Chat no existe.");
        }

        if (chat.getTipo() != TipoChat.GRUPAL) {
            throw new BadRequestException("Solo se pueden agregar miembros a un grupo");
        }

        validarAdminGrupo(chat, usuarioSolicitanteId);

        Set<Long> idsNuevos = normalizarIds(request.getUsuarioIds());
        if (idsNuevos.isEmpty()) {
            throw new BadRequestException("usuarioIds requerido");
        }

        Comunidad comunidad = chat.getComunidad();

        for (Long usuarioId : idsNuevos) {
            if (usuarioId.equals(usuarioSolicitanteId)) {
                continue;
            }

            if (chat.esParticipante(usuarioId)) {
                throw new ClientErrorException("Usuario ya es miembro", Response.Status.CONFLICT);
            }

            Usuario usuario = verificarUsuarioExiste(usuarioId);

            chat.agregarParticipante(usuario, RolGrupo.MIEMBRO);
        }

        chatRepository.flush();

        return mapearResumen(chat, usuarioSolicitanteId);
    }

    @Transactional
    public void eliminarMiembroDeGrupo(Long chatId, Long usuarioSolicitanteId, Long usuarioObjetivoId) {
        Chat chat = chatRepository.buscarChatPorId(chatId);
        if (chat == null) {
            throw new NotFoundException("Chat no existe.");
        }

        if (chat.getTipo() != TipoChat.GRUPAL) {
            throw new BadRequestException("Solo se pueden eliminar miembros de un grupo");
        }

        if (!chat.esParticipante(usuarioSolicitanteId)) {
            throw new ForbiddenException("Debe ser participante del grupo");
        }

        if (!chat.esParticipante(usuarioObjetivoId)) {
            throw new NotFoundException("Usuario no es miembro del grupo");
        }

        if (!usuarioSolicitanteId.equals(usuarioObjetivoId)) {
            validarAdminGrupo(chat, usuarioSolicitanteId);
        } else if (chat.getListaParticipaciones().stream()
                .anyMatch(participacion -> participacion.getUsuario().getId().equals(usuarioObjetivoId)
                        && participacion.getRol() == RolGrupo.ADMIN)
                && esUnicoAdmin(chat, usuarioObjetivoId)) {
            throw new BadRequestException("El ultimo admin no puede salir del grupo");
        }

        chat.removerParticipante(usuarioObjetivoId);
        chatRepository.flush();
    }

    @Transactional
    public void eliminarGrupo(Long chatId, Long usuarioSolicitanteId) {
        Chat chat = chatRepository.buscarChatPorId(chatId);
        if (chat == null) {
            throw new NotFoundException("Chat no existe.");
        }

        if (chat.getTipo() != TipoChat.GRUPAL) {
            throw new BadRequestException("Solo se pueden eliminar grupos");
        }

        validarAdminGrupo(chat, usuarioSolicitanteId);

        chatRepository.eliminarChat(chat);
        chatRepository.flush();
    }

    @Transactional
    public void fijarMensaje(Long chatId, Long mensajeId, Long usuarioId) {
        Chat chat = chatRepository.buscarChatPorId(chatId);
        if (chat == null) {
            throw new NotFoundException("Chat no existe.");
        }

        validarParticipacion(chat, usuarioId);

        Mensaje mensaje = chatRepository.buscarMensajePorId(mensajeId);
        if (mensaje == null || !mensaje.getChat().getId().equals(chatId)) {
            throw new NotFoundException("Mensaje no existe en el chat");
        }
    }

    @Transactional
    public void desfijarMensaje(Long chatId, Long mensajeId, Long usuarioId) {
        Chat chat = chatRepository.buscarChatPorId(chatId);
        if (chat == null) {
            throw new NotFoundException("Chat no existe.");
        }

        validarParticipacion(chat, usuarioId);

        Mensaje mensaje = chatRepository.buscarMensajePorId(mensajeId);
        if (mensaje == null || !mensaje.getChat().getId().equals(chatId)) {
            throw new NotFoundException("Mensaje no existe en el chat");
        }
    }

    @Transactional
    public List<MensajeDTO> obtenerMensajesFijados(Long chatId, Long usuarioId) {
        Chat chat = chatRepository.buscarChatPorId(chatId);
        if (chat == null) {
            throw new NotFoundException("Chat no existe.");
        }

        validarParticipacion(chat, usuarioId);

        return java.util.Collections.emptyList();
    }

    public Usuario resolverUsuarioAutenticado(String principal) {

        if (principal == null || principal.isBlank()) {
            throw new NotAuthorizedException("No autorizado");
        }

        Usuario usuario = null;

        try {
            usuario = usuarioRepository.buscarPorId(Long.valueOf(principal));
        } catch (NumberFormatException ignored) {
        }

        if (usuario == null) {
            usuario = usuarioRepository.buscarPorEmail(principal);
        }

        if (usuario == null) {
            throw new NotAuthorizedException("No autorizado");
        }

        return usuario;
    }

    private Usuario verificarUsuarioExiste(Long usuarioId) {
        Usuario usuario = usuarioRepository.buscarPorId(usuarioId);
        if (usuario == null) {
            throw new NotFoundException("Usuario no existe.");
        }
        return usuario;
    }

    private void validarParticipacion(Chat chat, Long usuarioId) {
        if (!chat.esParticipante(usuarioId)) {
            throw new ForbiddenException("Debe ser participante de la comunidad.");
        }
    }

    private void validarAdminGrupo(Chat chat, Long usuarioId) {
        boolean esAdmin = chat.getListaParticipaciones().stream()
                .anyMatch(participacion -> participacion.getUsuario().getId().equals(usuarioId)
                        && participacion.getRol() == RolGrupo.ADMIN);

        if (!esAdmin) {
            throw new ForbiddenException("Solo un admin de grupo puede realizar esta acción");
        }
    }

    private boolean esUnicoAdmin(Chat chat, Long usuarioId) {
        long admins = chat.getListaParticipaciones().stream()
                .filter(participacion -> participacion.getRol() == RolGrupo.ADMIN)
                .count();

        if (admins > 1) {
            return false;
        }

        return chat.getListaParticipaciones().stream()
                .anyMatch(participacion -> participacion.getUsuario().getId().equals(usuarioId)
                        && participacion.getRol() == RolGrupo.ADMIN);
    }

    private Set<Long> normalizarIds(List<Long> usuarioIds) {
        Set<Long> ids = new LinkedHashSet<>();

        if (usuarioIds == null) {
            return ids;
        }

        for (Long usuarioId : usuarioIds) {
            if (usuarioId != null) {
                ids.add(usuarioId);
            }
        }

        return ids;
    }

    private ChatResumenDTO mapearResumen(Chat chat, Long usuarioId) {
        ChatResumenDTO dto = new ChatResumenDTO();
        dto.setId(chat.getId());
        dto.setFechaCreacion(chat.getFechaCreacion());

        if (chat.getTipo() == TipoChat.DIRECTO) {

            Usuario otro = chat.getParticipantes().stream()
                    .filter(u -> !u.getId().equals(usuarioId))
                    .findFirst()
                    .orElse(null);

            if (otro != null) {
                dto.setNombre(otro.getNombre() + " " + otro.getApellido());
            }

            dto.setTipo("DIRECTO");

        } else { // canal o grupo

            dto.setNombre(chat.getNombre());
            dto.setDescripcion(chat.getDescripcion());
            dto.setFotoUrl(chat.getFotoUrl());
            dto.setTipo("GRUPO");
        }

        Mensaje ultimo = chatRepository.buscarUltimoMensaje(chat.getId());
        if (ultimo != null) {
            dto.setUltimoMensajeContenido(ultimo.getContenido());
            dto.setUltimoMensajeFecha(ultimo.getFechaEnvio());
        }

    return dto;
}

    private List<MensajeDTO> mapearMensajes(List<Mensaje> mensajes) {
        List<MensajeDTO> respuestas = new ArrayList<>();

        for (Mensaje mensaje : mensajes) {
            respuestas.add(mapearMensaje(mensaje));
        }

        return respuestas;
    }
    
    private MensajeDTO mapearMensaje(Mensaje mensaje) {
    
        MensajeDTO dto = new MensajeDTO(); 

        dto.setId(mensaje.getId());                      
        dto.setContenido(mensaje.getContenido());        
        dto.setFechaEnvio(mensaje.getFechaEnvio());
        dto.setTipo(mensaje.getTipo());
        dto.setEstado(mensaje.getEstado());

        dto.setEmisorId(mensaje.getEmisor().getId());
        dto.setEmisorNombre(mensaje.getEmisor().getNombre());
        dto.setEmisorApellido(mensaje.getEmisor().getApellido());
        dto.setChatId(mensaje.getChat().getId());

        // Nombre del chat: para grupos usar el nombre del grupo, para directos usar el nombre del emisor
        Chat chatMensaje = mensaje.getChat();
        if (chatMensaje.getTipo() == TipoChat.GRUPAL) {
            dto.setChatNombre(chatMensaje.getNombre());
        } else {
            dto.setChatNombre(mensaje.getEmisor().getNombre() + " " + mensaje.getEmisor().getApellido());
        }

        if (mensaje.getParentMessage() != null) {
            dto.setParentId(mensaje.getParentMessage().getId());
            dto.setParentContenido(mensaje.getParentMessage().getContenido());
            String parentNombre = mensaje.getParentMessage().getEmisor().getNombre()
                + " " + mensaje.getParentMessage().getEmisor().getApellido();
            dto.setParentEmisorNombre(parentNombre.trim());
        }

        // cargar reacciones
        java.util.List<com.appchat.model.Reaccion> reacciones = chatRepository.buscarReaccionesPorMensaje(mensaje.getId());
        java.util.List<com.appchat.dto.ReaccionDTO> ra = new java.util.ArrayList<>();
        for (com.appchat.model.Reaccion r : reacciones) {
            com.appchat.dto.ReaccionDTO rd = new com.appchat.dto.ReaccionDTO();
            rd.setId(r.getId());
            rd.setTipo(r.getTipo());
            rd.setUsuarioId(r.getUsuario().getId());
            rd.setUsuarioNombre(r.getUsuario().getNombre());
            rd.setUsuarioApellido(r.getUsuario().getApellido());
            rd.setFecha(r.getFecha());
            ra.add(rd);
        }
        dto.setReacciones(ra);

        return dto;
    }

    public void procesarMensajeWebSocket(Long userId, String message) {

    if (message == null || message.isBlank()) {
        throw new BadRequestException("Mensaje vacío");
    }

    MensajeWSDTO dto;
    try {
        dto = mapper.readValue(message, MensajeWSDTO.class);
    } catch (Exception e) {
        throw new BadRequestException("Formato de mensaje inválido");
    }

    if (esAcuseLecturaPorChat(dto)) {
        if (dto.getChatId() == null) {
            throw new BadRequestException("chatId requerido");
        }

        marcarChatComoLeidoDesdeWebSocket(userId, dto.getChatId());
        return;
    }

    if (esAcuseLecturaPorMensaje(dto)) {
        if (dto.getMensajeId() == null) {
            throw new BadRequestException("mensajeId requerido");
        }

        marcarMensajeLeidoDesdeWebSocket(userId, dto.getMensajeId(), dto.getChatId());
        return;
    }

    if (esAcuseLectura(dto)) {
        if (dto.getMensajeId() == null && dto.getChatId() == null) {
            throw new BadRequestException("mensajeId o chatId requerido");
        }

        if (dto.getChatId() != null) {
            marcarChatComoLeidoDesdeWebSocket(userId, dto.getChatId());
        } else {
            marcarMensajeLeidoDesdeWebSocket(userId, dto.getMensajeId(), null);
        }
        return;
    }

    // Las reacciones no necesitan chatId (lo obtiene del mensaje)
    if (dto.getAccion() != null && "REACCION".equalsIgnoreCase(dto.getAccion())) {
        if (dto.getMensajeId() == null) {
            throw new BadRequestException("mensajeId requerido para reacción");
        }
        if (dto.getContenido() == null || dto.getContenido().isBlank()) {
            throw new BadRequestException("contenido requerido para reacción");
        }
        agregarReaccion(userId, dto.getMensajeId(), dto.getContenido());
        return;
    }

    if (dto.getChatId() == null) {
        throw new BadRequestException("chatId requerido");
    }

    if (dto.getContenido() == null || dto.getContenido().isBlank()) {
        throw new BadRequestException("contenido requerido");
    }

    enviarMensaje(
        dto.getChatId(),
        userId,
        dto.getContenido(),
        dto.getParentId()
    );
}

    @Transactional
    public MensajeDTO marcarMensajeLeidoDesdeWebSocket(Long usuarioId, Long mensajeId, Long chatId) {
        Mensaje mensaje = chatRepository.buscarMensajePorId(mensajeId);

        if (mensaje == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        if (chatId != null && !mensaje.getChat().getId().equals(chatId)) {
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        validarParticipacion(mensaje.getChat(), usuarioId);

        if (mensaje.getEmisor().getId().equals(usuarioId)) {
            return mapearMensaje(mensaje);
        }

        if (mensaje.getEstado() != EstadoMensaje.LEIDO) {
            mensaje.setEstado(EstadoMensaje.LEIDO);
            chatRepository.flush();
        }

        MensajeDTO dto = mapearMensaje(mensaje);
        
        try {
            String json = mapper.writeValueAsString(dto);
            chatHub.enviarAUsuario(mensaje.getEmisor().getId(), json);
        } catch (Exception e) {
            throw new RuntimeException("Error serializando mensaje", e);
        }

        return dto;
    }

    @Transactional
    public void marcarMensajesPendientesAlConectar(Long usuarioId) {
        verificarUsuarioExiste(usuarioId);

        List<Chat> chats = chatRepository.listarChatsDeUsuario(usuarioId);
        List<MensajeDTO> actualizados = new ArrayList<>();

        for (Chat chat : chats) {
            List<Mensaje> pendientes = chatRepository
                    .buscarMensajesEnviadosPendientesEntregaParaUsuarioEnChat(chat.getId(), usuarioId);

            for (Mensaje mensaje : pendientes) {
                mensaje.setEstado(EstadoMensaje.ENTREGADO);
                actualizados.add(mapearMensaje(mensaje));
            }
        }

        if (actualizados.isEmpty()) {
            return;
        }

        chatRepository.flush();

        for (MensajeDTO dto : actualizados) {
            try {
                String json = mapper.writeValueAsString(dto);
                chatHub.enviarAUsuario(dto.getEmisorId(), json);
            } catch (Exception e) {
                throw new RuntimeException("Error serializando mensaje", e);
            }
        }
    }

    @Transactional
    public void marcarChatComoLeidoDesdeWebSocket(Long usuarioId, Long chatId) {
        Chat chat = chatRepository.buscarChatPorId(chatId);

        if (chat == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        validarParticipacion(chat, usuarioId);

        List<Mensaje> pendientes = chatRepository
                .buscarMensajesPendientesLecturaParaUsuarioEnChat(chatId, usuarioId);

        if (pendientes.isEmpty()) {
            return;
        }

        List<MensajeDTO> actualizados = new ArrayList<>();

        for (Mensaje mensaje : pendientes) {
            mensaje.setEstado(EstadoMensaje.LEIDO);
            actualizados.add(mapearMensaje(mensaje));
        }

        chatRepository.flush();

        for (MensajeDTO dto : actualizados) {
            try {
                String json = mapper.writeValueAsString(dto);
                chatHub.enviarAUsuario(dto.getEmisorId(), json);
            } catch (Exception e) {
                throw new RuntimeException("Error serializando mensaje", e);
            }
        }
    }

    private boolean esAcuseLectura(MensajeWSDTO dto) {
        return dto.getAccion() != null && "LEIDO".equalsIgnoreCase(dto.getAccion());
    }

    private boolean esAcuseLecturaPorChat(MensajeWSDTO dto) {
        return dto.getAccion() != null && "LEIDO_CHAT".equalsIgnoreCase(dto.getAccion());
    }

    private boolean esAcuseLecturaPorMensaje(MensajeWSDTO dto) {
        return dto.getAccion() != null && "LEIDO_MENSAJE".equalsIgnoreCase(dto.getAccion());
    }
    
}