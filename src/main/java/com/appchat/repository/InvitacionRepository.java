package com.appchat.repository;

import com.appchat.model.InvitacionComunidad;
import com.appchat.model.enums.EstadoInvitacion;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@ApplicationScoped
public class InvitacionRepository {
    
    @PersistenceContext(unitName = "appchatPU")
    private EntityManager em;
    
    
    public void guardar(InvitacionComunidad ic){
        em.persist(ic);
    }
    
    public boolean existeInvitacionPendiente(Long comunidadId, Long userId){
        Long count = em.createQuery(
                "SELECT COUNT(i) FROM InvitacionComunidad i " +
                "WHERE i.comunidad.id = :comunidadId " +
                "AND i.usuarioInvitado.id = :userId " +
                "AND i.estado = :estado", Long.class)
                .setParameter("comunidadId", comunidadId)
                .setParameter("userId", userId)
                .setParameter("estado", EstadoInvitacion.PENDIENTE)
                .getSingleResult();
        
        return count > 0;
    }
    
    public InvitacionComunidad buscarPorId(Long id) {
        return em.find(InvitacionComunidad.class, id);
    }

    public void actualizar(InvitacionComunidad inv) {
        em.merge(inv);
    }
    
    public List<InvitacionComunidad> listarPendientesPorUsuario(Long userId) {
    return em.createQuery(
            "SELECT i FROM InvitacionComunidad i " +
            "WHERE i.usuarioInvitado.id = :userId " +
            "AND i.estado = :estado", InvitacionComunidad.class)
            .setParameter("userId", userId)
            .setParameter("estado", EstadoInvitacion.PENDIENTE)
            .getResultList();
}
}
