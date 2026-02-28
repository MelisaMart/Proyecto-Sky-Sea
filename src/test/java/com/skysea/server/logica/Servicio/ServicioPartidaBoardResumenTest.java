package com.skysea.server.logica.Servicio;

import com.skysea.server.logica.model.Dron;
import com.skysea.server.logica.model.Equipo;
import com.skysea.server.logica.model.Jugador;
import com.skysea.server.logica.model.Partida;
import com.skysea.server.logica.model.Porta;
import com.skysea.server.logica.model.Posicion;
import com.skysea.server.logica.model.Reglas;
import com.skysea.server.persistencia.memory.InMemoryPartidaDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ServicioPartidaBoardResumenTest {

    private ServicioPartida servicio;

    @BeforeEach
    void setUp() {
        InMemoryPartidaDAO dao = new InMemoryPartidaDAO();
        servicio = new ServicioPartida(dao);
    }

    @Test
    void boardDeJugador1IncluyeResumenEnemigoCorrecto() {
        ServicioPartida.JoinResponse j1 = servicio.join("Jugador1", "NAVAL");
        ServicioPartida.JoinResponse j2 = servicio.join("Jugador2", "AEREO");

        servicio.seleccionarPlantilla(j1.playerId, "Naval1");
        servicio.seleccionarPlantilla(j2.playerId, "Aereo1");

        ServicioPartida.BoardResponse boardJ1 = servicio.obtenerTablero(j1.playerId);

        assertEquals("NAVAL", boardJ1.miEquipo);
        assertNotNull(boardJ1.resumenMiEquipo);
        assertNotNull(boardJ1.resumenEnemigo);

        assertEquals("NAVAL", boardJ1.resumenMiEquipo.equipo);
        assertEquals("AEREO", boardJ1.resumenEnemigo.equipo);

        assertEquals(Reglas.IMPACTOS_PORTA_NAVAL, boardJ1.resumenMiEquipo.impactosRestantesPorta);
        assertEquals(Reglas.IMPACTOS_PORTA_AEREO, boardJ1.resumenEnemigo.impactosRestantesPorta);
        assertTrue(boardJ1.resumenEnemigo.impactosRestantesPorta > 0);
    }

    @Test
    void boardDeJugador2IncluyeResumenEnemigoCorrecto() {
        ServicioPartida.JoinResponse j1 = servicio.join("Jugador1", "NAVAL");
        ServicioPartida.JoinResponse j2 = servicio.join("Jugador2", "AEREO");

        servicio.seleccionarPlantilla(j1.playerId, "Naval1");
        servicio.seleccionarPlantilla(j2.playerId, "Aereo1");

        ServicioPartida.BoardResponse boardJ2 = servicio.obtenerTablero(j2.playerId);

        assertEquals("AEREO", boardJ2.miEquipo);
        assertNotNull(boardJ2.resumenMiEquipo);
        assertNotNull(boardJ2.resumenEnemigo);

        assertEquals("AEREO", boardJ2.resumenMiEquipo.equipo);
        assertEquals("NAVAL", boardJ2.resumenEnemigo.equipo);

        assertEquals(Reglas.IMPACTOS_PORTA_AEREO, boardJ2.resumenMiEquipo.impactosRestantesPorta);
        assertEquals(Reglas.IMPACTOS_PORTA_NAVAL, boardJ2.resumenEnemigo.impactosRestantesPorta);
        assertTrue(boardJ2.resumenEnemigo.impactosRestantesPorta > 0);
    }

    @Test
    void boardReflejaDisparoAlPortaEnResumenEnemigo() {
        ServicioPartida.JoinResponse j1 = servicio.join("Jugador1", "NAVAL");
        ServicioPartida.JoinResponse j2 = servicio.join("Jugador2", "AEREO");

        servicio.seleccionarPlantilla(j1.playerId, "Naval1");
        servicio.seleccionarPlantilla(j2.playerId, "Aereo1");

        Partida partida = servicio.getPartidaActiva();
        Jugador jug1 = partida.getJugador1();
        Jugador jug2 = partida.getJugador2();
        assertNotNull(jug1);
        assertNotNull(jug2);

        partida.setTurnoDe(Equipo.NAVAL);

        Dron dronMisil = jug1.getDrones().get(0);
        Porta portaEnemiga = jug2.getPorta();
        Posicion celdaObjetivo = portaEnemiga.getCeldasOcupadas().get(0);

        dronMisil.getPosicion().setX(celdaObjetivo.getX() + 1);
        dronMisil.getPosicion().setY(celdaObjetivo.getY());

        int impactosAntes = portaEnemiga.getImpactosRestantes();

        ServicioPartida.TemplateResponse seleccion = servicio.selectDrone(j1.playerId, dronMisil.getId());
        assertTrue(seleccion.ok);

        ServicioPartida.ShootResponse disparo = servicio.shoot2(
                j1.playerId,
                celdaObjetivo.getY(),
                celdaObjetivo.getX()
        );

        assertTrue(disparo.ok);
        assertEquals("HIT", disparo.resultado);
        assertEquals("PORTA", disparo.objetivo);
        assertEquals(impactosAntes - 1, disparo.impactosRestantesPorta);

        ServicioPartida.BoardResponse boardJ1 = servicio.obtenerTablero(j1.playerId);

        assertNotNull(boardJ1.resumenEnemigo);
        assertEquals("AEREO", boardJ1.resumenEnemigo.equipo);
        assertEquals(impactosAntes - 1, boardJ1.resumenEnemigo.impactosRestantesPorta);
    }
}
