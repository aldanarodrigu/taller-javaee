package com.appchat.repository;

import com.appchat.model.MiembroComunidad;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;


@ApplicationScoped
public class MiembroComunidadRepository {
    
    @PersistenceContext(unitName = "appchatPU") 
    private EntityManager em;
    
    public MiembroComunidad buscarPorUsuarioYComunidad(Long usuarioId,Long comunidadId){
        try {
            return em.createQuery("""
                SELECT mc
                FROM MiembroComunidad mc
                WHERE mc.usuario.id = :usuarioId
                AND mc.comunidad.id = :comunidadId
            """, MiembroComunidad.class)
            .setParameter("usuarioId", usuarioId)
            .setParameter("comunidadId", comunidadId)
            .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }
    
    public void eliminar(MiembroComunidad miembro) {
        em.remove(em.contains(miembro)
                ? miembro
                : em.merge(miembro));
    }
    
    public void guardar(MiembroComunidad miembro) {
        em.persist(miembro);
    }
    
}
