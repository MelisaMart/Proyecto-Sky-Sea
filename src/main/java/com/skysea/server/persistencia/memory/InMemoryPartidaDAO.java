package com.skysea.server.persistencia.memory;

import com.skysea.server.logica.model.Partida;
import com.skysea.server.persistencia.dao.IPartidaDAO;

public class InMemoryPartidaDAO implements IPartidaDAO {

    private Partida partidaActiva;

    public InMemoryPartidaDAO() {
        reset();
    }

    @Override
    public Partida loadActiva() {
        return partidaActiva;
    }

    @Override
    public void save(Partida partida) {
        this.partidaActiva = partida;
    }

    @Override
    public void reset() {
        this.partidaActiva = new Partida();
    }
}
