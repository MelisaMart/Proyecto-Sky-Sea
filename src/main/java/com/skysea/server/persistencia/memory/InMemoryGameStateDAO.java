package com.skysea.server.persistencia.memory;

import com.skysea.server.logica.model.*;
import com.skysea.server.persistencia.dao.IGameStateDAO;

public class InMemoryGameStateDAO implements IGameStateDAO {

    private EstadoJuego estado;

    public InMemoryGameStateDAO() {
        reset();
    }

    @Override
    public EstadoJuego load() {
        return estado;
    }

    @Override
    public void save(EstadoJuego estado) {
        this.estado = estado;
    }

    @Override
    public void reset() {
        Dron dron = new Dron(Equipo.NAVAL, new Posicion(5, 5));
        EstadoJuego estado = new EstadoJuego(dron);

        estado.setNumeroTurno(1);
        estado.setTurnoDe(Equipo.NAVAL);

        this.estado = estado;
    }



}
