package com.skysea.server.persistencia.dao;

import com.skysea.server.logica.model.EstadoJuego;

public interface IGameStateDAO {
    EstadoJuego load();
    void save(EstadoJuego estado);
    void reset();
}
