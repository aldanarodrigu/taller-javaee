package com.appchat.model;

import com.appchat.model.enums.EstadoUsuario;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String apellido;
    
    @Column(nullable = false, unique = true)
    private String username;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private EstadoUsuario estado;

    @Column(name = "foto_perfil")
    private String fotoPerfil;

    @Column(name = "public_key", columnDefinition = "TEXT")
    private String publicKey;

    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<Participa> participaciones = new ArrayList<>(); //estas son las participaciones de chat tanto grupales como directos
    
    @OneToMany(mappedBy = "usuario", fetch = FetchType.LAZY)
    private List<MiembroComunidad> comunidades = new ArrayList<>();
    
    public Long getId(){ 
        return id; 
    }
    
    public void setId(Long id){ 
        this.id = id; 
    }
    
    public String getNombre(){ 
     return nombre; 
    }
    
    public void setNombre(String nombre){ 
        this.nombre = nombre; 
    }
    
    public String getApellido(){ 
        return apellido; 
    }
    
    public void setApellido(String apellido){ 
        this.apellido = apellido; 
    }
    
    public String getEmail(){ 
        return email; 
    }
    
    public void setEmail(String email){ 
        this.email = email; 
    }
    
    public String getPassword(){ 
        return password; 
    }
    
    public void setPassword(String password){ 
        this.password = password; 
    }
    
    public EstadoUsuario getEstado(){ 
        return estado; 
    }
    
    public void setEstado(EstadoUsuario estado){ 
        this.estado = estado; 
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<Participa> getParticipaciones() {
        return participaciones;
    }

    public void setParticipaciones(List<Participa> participaciones) {
        this.participaciones = participaciones;
    }

    public List<MiembroComunidad> getComunidades() {
        return comunidades;
    }

    public void setComunidades(List<MiembroComunidad> comunidades) {
        this.comunidades = comunidades;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

}