package com.skysea.server.logica.Servicio;

import com.skysea.server.logica.model.*;
import com.skysea.server.persistencia.dao.IPartidaDAO;

public class ServicioPartida {

    private final IPartidaDAO dao;

    public ServicioPartida(IPartidaDAO dao) {
        this.dao = dao;
    }

    public Partida getPartidaActiva() {
        return dao.loadActiva();
    }


    public JoinResponse join(String nombre) {
        Partida partida = dao.loadActiva();

        // Si ya hay 2 jugadores, no deja entrar
        if (partida.getJugador1() != null && partida.getJugador2() != null) {
            return new JoinResponse(null, null, nombre, "OCUPADO", null);
        }

        // Restricción simple: nombre único dentro de esta partida activa
        Jugador existente = partida.buscarJugadorPorNombre(nombre);
        if (existente != null) {
            // Si el mismo nombre ya está dentro, devolvemos su playerId (sirve para re-entrar en la demo)
            return new JoinResponse(partida.getIdPartida(), existente.getId(), existente.getNombre(),
                    partida.getEstado().name(), existente.getEquipo() != null ? existente.getEquipo().name() : null);
        }

        // Crear jugador nuevo
        Jugador nuevo = new Jugador(nombre);

        // Asignar slot y equipo
        if (partida.getJugador1() == null) {
            nuevo.asignarEquipo(Equipo.NAVAL); // Naval arranca (según reglas)
            partida.setJugador1(nuevo);
            partida.setEstado(EstadoPartida.ESPERANDO_RIVAL);
        } else {
            nuevo.asignarEquipo(Equipo.AEREO);
            partida.setJugador2(nuevo);
            partida.setEstado(EstadoPartida.EN_JUEGO);
        }

        dao.save(partida);

        return new JoinResponse(partida.getIdPartida(), nuevo.getId(), nuevo.getNombre(),
                partida.getEstado().name(), nuevo.getEquipo().name());
    }

    // Clase interna simple para que el Service no dependa del DTO de presentación
    public static class JoinResponse {
        public final String idPartida;
        public final String playerId;
        public final String nombre;
        public final String estadoPartida;
        public final String equipo;

        public JoinResponse(String idPartida, String playerId, String nombre, String estadoPartida, String equipo) {
            this.idPartida = idPartida;
            this.playerId = playerId;
            this.nombre = nombre;
            this.estadoPartida = estadoPartida;
            this.equipo = equipo;
        }
    }

    public Partida terminarTurno(String playerId) {
    Partida partida = dao.loadActiva();

    // Validación: jugador existe
    Jugador j = partida.buscarJugadorPorId(playerId);
    if (j == null) {
        throw new IllegalArgumentException("PLAYER_NO_ENCONTRADO");
    }

    // Validación: es su turno
    if (!partida.esTurnoDe(playerId)) {
        throw new IllegalStateException("NO_ES_TU_TURNO");
    }

    // Cambiar turno
    if (partida.getTurnoDe() == Equipo.NAVAL) {
        partida.setTurnoDe(Equipo.AEREO);
    } else {
        partida.setTurnoDe(Equipo.NAVAL);
    }

    // Aumentar turno
    partida.setNumeroTurno(partida.getNumeroTurno() + 1);

    dao.save(partida);
    return partida;
}


}
