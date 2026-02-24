package com.skysea.server.persistencia.dao;

import com.skysea.server.logica.model.Partida;

public interface IPartidaDAO {
    Partida loadActiva();
    void save(Partida partida);
    void reset();
}
