
package com.appchat.repository;

import com.appchat.dto.UsuarioResponseDTO;
import com.appchat.model.Comunidad;
import com.appchat.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;

@ApplicationScoped
public class ComunidadRepository {

    @PersistenceContext(unitName = "appchatPU") 
    private EntityManager em;
    
    public Comunidad buscarPorId(Long comunidadId) {
        return em.find(Comunidad.class, comunidadId);
    }
    
    public Comunidad actualizar(Comunidad comunidad) {
    return em.merge(comunidad);
    }
    
    public void eliminar(Long id) {
    Comunidad c = em.find(Comunidad.class, id);
    if (c != null) em.remove(c);
    }
    
    public boolean sonMiembros(Long comunidadId, Long u1, Long u2) {

        Long count = em.createQuery("""
            SELECT COUNT(mc) FROM MiembroComunidad mc
            WHERE mc.comunidad.id = :comunidadId
            AND mc.usuario.id IN (:u1, :u2)
        """, Long.class)
        .setParameter("comunidadId", comunidadId)
        .setParameter("u1", u1)
        .setParameter("u2", u2)
        .getSingleResult();

        return count == 2;
    }

    public void guardar(Comunidad comunidadNueva) {
        em.persist(comunidadNueva);
    }

    public List<Usuario> listarMiembros(Long comunidadId) {
        List<Usuario> usuarios = em.createQuery(
            "SELECT u FROM MiembroComunidad mc JOIN mc.usuario u WHERE mc.comunidad.id = :comunidadId",
            Usuario.class
        )
        .setParameter("comunidadId", comunidadId)
        .getResultList();
        
        return usuarios;
    }
    
    public List<Comunidad> listarPorUsuario(Long userId) {
    return em.createQuery(
        "SELECT c FROM MiembroComunidad mc JOIN mc.comunidad c WHERE mc.usuario.id = :userId",
        Comunidad.class
    )
    .setParameter("userId", userId)
    .getResultList();
}
    
}
