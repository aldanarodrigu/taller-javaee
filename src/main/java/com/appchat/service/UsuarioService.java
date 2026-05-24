package com.appchat.service;

import com.appchat.dto.UsuarioResponseDTO;
import com.appchat.dto.UsuarioDTO;
import com.appchat.model.Usuario;
import com.appchat.model.enums.EstadoUsuario;
import com.appchat.repository.UsuarioRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.appchat.dto.ActualizarUsuarioDTO;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;

import org.mindrot.jbcrypt.BCrypt;

@ApplicationScoped // bean manejado por CDI, basicamente dice que esta clase solo se va a instancias una sola vez
public class UsuarioService{

    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[A-Z])(?=.*\\d).{8,}$");

    @Inject // inyeccion de dependencias, el contenedor automaticamente te da una instancia de UsuarioService (para no hacer UsuarioRepository repository = new UsuarioRepository())
    private UsuarioRepository repository;

    @Transactional // este método corre dentro de una transacción de base de datos, basicamente para que si algo falla aca haga rollback
    public Usuario crearUsuario(UsuarioDTO usuarioDto) {

        validarPassword(usuarioDto.getPassword());

        if (repository.buscarPorEmail(usuarioDto.getEmail()) != null) {
            throw new BadRequestException("El email ya está registrado");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(usuarioDto.getNombre());
        usuario.setApellido(usuarioDto.getApellido());
        usuario.setEmail(usuarioDto.getEmail());
        usuario.setUsername(usuarioDto.getUserName());
        // TODO: hashear la password antes de guardar
        //usuario.setPassword(usuarioDto.getPassword());
        String hashed = BCrypt.hashpw(usuarioDto.getPassword(), BCrypt.gensalt());
        usuario.setPassword(hashed);
        usuario.setEstado(EstadoUsuario.INVISIBLE);

        repository.guardar(usuario); // LLAMAS AL REPOSITORIO DE USUARIO, ACA NO PODES DIRECTAMENTE CON LA BD!!

        return usuario;
    }

    @Transactional
    public List<UsuarioResponseDTO> listarUsuarios() {
        return repository.listarUsuarios().stream()
                .map(this::mapearUsuario)
                .collect(Collectors.toList());
    }

    @Transactional
    public UsuarioResponseDTO obtenerUsuarioPorId(Long id) {
        Usuario usuario = repository.buscarPorId(id);
        if (usuario == null) {
            throw new NotFoundException("Usuario no econtrado.");
        }

        return mapearUsuario(usuario);
    }
    
    @Transactional
public void actualizarFoto(Long id, String url) {
    Usuario usuario = repository.buscarPorId(id);
    if (usuario != null) {
        usuario.setFotoPerfil(url);
        repository.actualizar(usuario);
    }
}

    @Transactional
    public List<UsuarioResponseDTO> buscarUsuarios(String q) {
        return repository.buscarPorNombreOEmail(q).stream()
                .map(this::mapearUsuario)
                .collect(Collectors.toList());
    }

    public UsuarioResponseDTO mapearUsuario(Usuario usuario) {
        UsuarioResponseDTO dto = new UsuarioResponseDTO();
        dto.setId(usuario.getId());
        dto.setNombre(usuario.getNombre());
        dto.setApellido(usuario.getApellido());
        dto.setEmail(usuario.getEmail());
        dto.setEstado(usuario.getEstado());
        dto.setUsername(usuario.getUsername());
        dto.setFotoPerfil(usuario.getFotoPerfil());
        return dto;
    }

    public Usuario obtenerPorEmail(String email) {
        return repository.buscarPorEmail(email);
    }

    private void validarPassword(String password) {
        if (password == null || !PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BadRequestException("La contraseña debe tener al menos 8 caracteres, una mayúscula y un número");
        }
    }

    public Usuario obtenerPorId(Long id){
        return repository.buscarPorId(id);
    }

    public boolean existeUsuario(Long id){
        return repository.existeUsuario(id);
    }

    Usuario buscarPorUsername(String username) {
        return repository.buscarPorUsername(username);
    }

    @Transactional
    public UsuarioResponseDTO actualizarUsuario(Long id, ActualizarUsuarioDTO dto) {
        Usuario usuario = repository.buscarPorId(id);

        if (usuario == null) {
            throw new NotFoundException("Usuario no encontrado");
        }

        if (dto.getNombre() != null) {
            usuario.setNombre(dto.getNombre());
        }

        if (dto.getApellido() != null) {
            usuario.setApellido(dto.getApellido());
        }

        if (dto.getFotoPerfil() != null) {
            usuario.setFotoPerfil(dto.getFotoPerfil());
        }

        Usuario actualizado = repository.actualizar(usuario);

        return mapearUsuario(actualizado);
    }
    
    @Transactional
    public UsuarioResponseDTO actualizarEstado(Long id, String estadoStr) {

        Usuario usuario = repository.buscarPorId(id);

        if (usuario == null) {
            throw new NotFoundException("Usuario no encontrado");
        }

        try {
            EstadoUsuario estado = EstadoUsuario.valueOf(estadoStr.toUpperCase());
            usuario.setEstado(estado);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Estado inválido");
        }

        repository.actualizar(usuario);

        return mapearUsuario(usuario);
    }

}
