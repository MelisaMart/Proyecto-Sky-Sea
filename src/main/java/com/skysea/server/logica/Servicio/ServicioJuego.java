package com.skysea.server.logica.Servicio;

import com.skysea.server.logica.model.*;
import com.skysea.server.persistencia.dao.IGameStateDAO;

public class ServicioJuego {

    private final IGameStateDAO dao;

    public ServicioJuego(IGameStateDAO dao) {
        this.dao = dao;
    }

    public EstadoJuego getEstado() {
        return dao.load();
    }

    public EstadoJuego disparar() {
        EstadoJuego estado = dao.load();
        Dron dron = estado.getDron();

        if (dron.puedeDisparar()) {
            dron.disparar();
            estado.setUltimoDisparo(dron.getTipoProyectil());
            dao.save(estado);
        }
        return estado;
    }

    public EstadoJuego mover(int dx, int dy) {
        EstadoJuego estado = dao.load();
        estado.getDron().mover(dx, dy);
        dao.save(estado);
        return estado;
    }

    public void reset() {
        dao.reset();
    }


    public EstadoJuego terminarTurno() {
        EstadoJuego estado = dao.load();

        // 1) Resetea acciones del dron para permitir volver a disparar/mover
        estado.getDron().resetAccionesTurno();

        // 2) Aumenta contador de turnos
        estado.setNumeroTurno(estado.getNumeroTurno() + 1);

        // 3) Cambia el turno al otro equipo (preparado para 2 jugadores)
        if (estado.getTurnoDe() == Equipo.NAVAL) {
            estado.setTurnoDe(Equipo.AEREO);
        } else {
            estado.setTurnoDe(Equipo.NAVAL);
        }

        // 4) Guardar
        dao.save(estado);

        return estado;
    }


}
