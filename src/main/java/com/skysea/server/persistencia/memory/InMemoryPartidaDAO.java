package com.skysea.server.persistencia.memory;

import com.skysea.server.logica.model.EstadoPartida;
import com.skysea.server.logica.model.Partida;
import com.skysea.server.persistencia.dao.IPartidaDAO;

import java.util.List;
import java.util.Optional;

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
    public synchronized Optional<Partida> loadById(String idPartida) {
        if (partidaActiva != null && partidaActiva.getIdPartida().equals(idPartida)) {
            return Optional.of(partidaActiva);
        }
        return Optional.empty();
    }

    @Override
    public synchronized List<PartidaReanudableInfo> findReanudablesByNombre(String nombreJugador) {
        if (partidaActiva == null || !partidaActiva.isReanudable()) {
            return List.of();
        }
        boolean esJugador = partidaActiva.buscarJugadorPorNombre(nombreJugador) != null;
        if (!esJugador) {
            return List.of();
        }
        return List.of(new PartidaReanudableInfo(
                partidaActiva.getIdPartida(),
                partidaActiva.getEstado().name(),
                partidaActiva.buscarJugadorPorNombre(nombreJugador).getEquipo().name(),
                partidaActiva.getNumeroTurno(),
                null
        ));
    }

    @Override
    public synchronized boolean existsNombreEnPartidaActiva(String nombreJugador) {
        if (partidaActiva == null || nombreJugador == null || nombreJugador.isBlank()) {
            return false;
        }

        EstadoPartida estado = partidaActiva.getEstado();
        boolean partidaBloqueante = estado == EstadoPartida.EN_JUEGO || estado == EstadoPartida.ESPERANDO_RIVAL;
        if (!partidaBloqueante) {
            return false;
        }

        return partidaActiva.buscarJugadorPorNombre(nombreJugador.trim()) != null;
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
