package com.skysea.server.persistencia.dao;

import com.skysea.server.logica.model.Partida;

import java.util.List;
import java.util.Optional;

public interface IPartidaDAO {
    Partida loadActiva();
    Optional<Partida> loadById(String idPartida);
    List<PartidaReanudableInfo> findReanudablesByNombre(String nombreJugador);
    boolean existsNombreEnPartidaActiva(String nombreJugador);
    void save(Partida partida);
    void reset();

    final class PartidaReanudableInfo {
        public final String idPartida;
        public final String estadoPartida;
        public final String equipoJugador;
        public final int turnoNumero;
        public final String fechaCreacion;

        public PartidaReanudableInfo(String idPartida,
                                     String estadoPartida,
                                     String equipoJugador,
                                     int turnoNumero,
                                     String fechaCreacion) {
            this.idPartida = idPartida;
            this.estadoPartida = estadoPartida;
            this.equipoJugador = equipoJugador;
            this.turnoNumero = turnoNumero;
            this.fechaCreacion = fechaCreacion;
        }
    }
}
