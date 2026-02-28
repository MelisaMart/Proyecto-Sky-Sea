package com.skysea.server.persistencia.memory;

import com.skysea.server.logica.model.Partida;
import com.skysea.server.persistencia.dao.IPartidaDAO;

public class InMemoryPartidaDAO implements IPartidaDAO {

    private volatile Partida partidaActiva;

    public InMemoryPartidaDAO() {
        reset();
    }

    @Override
    public synchronized Partida loadActiva() {
        return partidaActiva;
    }

    @Override
    public synchronized void save(Partida partida) {
        this.partidaActiva = partida;
    }

    @Override
    public synchronized void reset() {
        this.partidaActiva = new Partida();
    }
}
