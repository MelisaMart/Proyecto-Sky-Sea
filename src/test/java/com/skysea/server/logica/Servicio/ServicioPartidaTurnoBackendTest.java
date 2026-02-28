package com.skysea.server.logica.Servicio;

import com.skysea.server.logica.model.Equipo;
import com.skysea.server.logica.model.EstadoPartida;
import com.skysea.server.logica.model.Partida;
import com.skysea.server.persistencia.memory.InMemoryPartidaDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ServicioPartidaTurnoBackendTest {

    private ServicioPartida servicio;
    private InMemoryPartidaDAO dao;
    private ServicioPartida.JoinResponse j1;
    private ServicioPartida.JoinResponse j2;

    @BeforeEach
    void setUp() {
        dao = new InMemoryPartidaDAO();
        servicio = new ServicioPartida(dao);

        j1 = servicio.join("TurnoJ1", "NAVAL");
        j2 = servicio.join("TurnoJ2", "AEREO");

        Partida partida = dao.loadActiva();
        assertNotNull(partida);
        assertEquals(EstadoPartida.EN_JUEGO, partida.getEstado());
    }

    @Test
    void stateTurnoIncluyeDatosCanonicosParaCadaJugador() {
        ServicioPartida.TurnoEstadoResponse estadoJ1 = servicio.obtenerEstadoTurno(j1.playerId);
        ServicioPartida.TurnoEstadoResponse estadoJ2 = servicio.obtenerEstadoTurno(j2.playerId);

        assertEquals("EN_JUEGO", estadoJ1.estadoPartida);
        assertEquals("NAVAL", estadoJ1.turnoDe);
        assertEquals(1, estadoJ1.numeroTurno);
        assertEquals(1, estadoJ1.numeroJugador);
        assertTrue(estadoJ1.esMiTurno);
        assertTrue(estadoJ1.segundosRestantesTurno > 0);
        assertEquals(Partida.DURACION_TURNO_SEGUNDOS, estadoJ1.duracionTurnoSegundos);

        assertEquals(2, estadoJ2.numeroJugador);
        assertFalse(estadoJ2.esMiTurno);
        assertEquals(Partida.DURACION_TURNO_SEGUNDOS, estadoJ2.duracionTurnoSegundos);
    }

    @Test
    void timeoutDeTurnoSeResuelveEnBackendSinIntervencionDelFront() {
        Partida partida = dao.loadActiva();
        partida.setTurnoDe(Equipo.NAVAL);
        partida.setNumeroTurno(7);
        partida.setTurnoInicioEpochMs(System.currentTimeMillis() - (Partida.DURACION_TURNO_SEGUNDOS * 1000L + 2000L));
        dao.save(partida);

        ServicioPartida.TurnoEstadoResponse estadoTrasTimeout = servicio.obtenerEstadoTurno(j1.playerId);

        assertEquals("AEREO", estadoTrasTimeout.turnoDe);
        assertEquals(8, estadoTrasTimeout.numeroTurno);
        assertFalse(estadoTrasTimeout.esMiTurno);

        ServicioPartida.TurnoEstadoResponse estadoJ2 = servicio.obtenerEstadoTurno(j2.playerId);
        assertTrue(estadoJ2.esMiTurno);
        assertTrue(estadoJ2.segundosRestantesTurno > 0);
    }
}
