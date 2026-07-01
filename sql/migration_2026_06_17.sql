-- Invitaciones a comunidades

CREATE TABLE invitacion_comunidad (
                                      id BIGSERIAL PRIMARY KEY,
                                      estado VARCHAR(255),

                                      comunidad_id BIGINT,
                                      owner_id BIGINT,
                                      usuario_invitado_id BIGINT,

                                      CONSTRAINT fk_invitacion_comunidad
                                          FOREIGN KEY (comunidad_id)
                                              REFERENCES comunidades(id),

                                      CONSTRAINT fk_invitacion_owner
                                          FOREIGN KEY (owner_id)
                                              REFERENCES usuarios(id),

                                      CONSTRAINT fk_invitacion_usuario
                                          FOREIGN KEY (usuario_invitado_id)
                                              REFERENCES usuarios(id)
);

-- Respuestas a mensajes

ALTER TABLE mensajes
    ADD COLUMN parent_id BIGINT;

ALTER TABLE mensajes
    ADD CONSTRAINT fk_mensajes_parent_id
        FOREIGN KEY (parent_id)
            REFERENCES mensajes(id);