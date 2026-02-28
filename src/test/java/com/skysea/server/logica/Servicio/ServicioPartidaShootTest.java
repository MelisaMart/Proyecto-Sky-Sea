package com.skysea.server.logica.Servicio;

import com.skysea.server.logica.model.*;
import com.skysea.server.persistencia.memory.InMemoryPartidaDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests unitarios para ServicioPartida.shoot2()
 * Validar reglas: disparo por turno, municion, rango, danio, persistencia
 */


public class ServicioPartidaShootTest {

    private ServicioPartida servicio;
    private InMemoryPartidaDAO dao;

    @BeforeEach
    void setUp() {
        dao = new InMemoryPartidaDAO();
        servicio = new ServicioPartida(dao);

        // Crear una partida con 2 jugadores
        ServicioPartida.JoinResponse j1 = servicio.join("Jugador1", "NAVAL");
        ServicioPartida.JoinResponse j2 = servicio.join("Jugador2", "AEREO");

        // Obtener partida actual para acceder a los jugadores
        Partida partida = dao.loadActiva();
        assertNotNull(partida);
        assertEquals(EstadoPartida.EN_JUEGO, partida.getEstado());

        // Aplicar plantillas de despliegue
        servicio.seleccionarPlantilla(j1.playerId, "Naval1");
        servicio.seleccionarPlantilla(j2.playerId, "Aereo1");

        // Verificar que los drones están creados
        Jugador jug1 = partida.buscarJugadorPorId(j1.playerId);
        Jugador jug2 = partida.buscarJugadorPorId(j2.playerId);
        assertNotNull(jug1);
        assertNotNull(jug2);
        assertTrue(jug1.getDrones().size() > 0);
        assertTrue(jug2.getDrones().size() > 0);
    }

    /**
     * Test 1: Fuera de turno → ERROR
     * J1 intenta disparar pero no es su turno (comienza el turno de J2 en turnos iniciales)
     */
    @Test
    void testShootFueraDeturno() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1();

        // Cambiar al turno de J2
        partida.setTurnoDe(Equipo.AEREO);

        // Seleccionar un dron de J1 para intentar disparar
        assertFalse(jug1.getDrones().isEmpty());
        Dron dron1 = jug1.getDrones().get(0);
        jug1.setDronSeleccionado(dron1.getId());

        // Intentar disparar cuando no es su turno
        ServicioPartida.ShootResponse resp = servicio.shoot2(jug1.getId(), 5, 5);

        assertFalse(resp.ok);
        assertEquals("NO_ES_TU_TURNO", resp.estado);
        assertNull(resp.resultado);
    }

    /**
     * Test 2: Sin dron seleccionado → ERROR
     */
    @Test
    void testShootSinDronSeleccionado() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1();

        // Cambiar turno para que sea el de J1
        partida.setTurnoDe(Equipo.NAVAL);

        // NO seleccionar dron
        jug1.setDronSeleccionado(null);

        ServicioPartida.ShootResponse resp = servicio.shoot2(jug1.getId(), 5, 5);

        assertFalse(resp.ok);
        assertEquals("SIN_DRON_SELECCIONADO", resp.estado);
        assertNull(resp.resultado);
    }

    /**
     * Test 3: Fuera de rango → ERROR
     * Rango máximo es 1 (adyacente), disparo a distancia mayor
     */
    @Test
    void testShootFueraDeRango() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1();


        // Cambiar turno para que sea el de J1
        partida.setTurnoDe(Equipo.NAVAL);

        Dron dron1 = jug1.getDrones().get(0);
        jug1.setDronSeleccionado(dron1.getId());

        int xOrigen = dron1.getPosicion().getX();
        int yOrigen = dron1.getPosicion().getY();
        int deltaFueraRango = dron1.getRangoAtaque() + 1;

        // Intentar disparar fuera del rango, pero dentro del tablero
        int filaDestino = yOrigen;
        int colDestino = xOrigen + deltaFueraRango;
        if (colDestino > Reglas.TABLERO_X_MAX) {
            colDestino = xOrigen - deltaFueraRango;
        }
        if (colDestino < Reglas.TABLERO_X_MIN) {
            colDestino = xOrigen;
            filaDestino = yOrigen + deltaFueraRango;
            if (filaDestino > Reglas.TABLERO_Y_MAX) {
                filaDestino = yOrigen - deltaFueraRango;
            }
        }

        assertTrue(colDestino >= Reglas.TABLERO_X_MIN && colDestino <= Reglas.TABLERO_X_MAX);
        assertTrue(filaDestino >= Reglas.TABLERO_Y_MIN && filaDestino <= Reglas.TABLERO_Y_MAX);

        ServicioPartida.ShootResponse resp = servicio.shoot2(jug1.getId(), filaDestino, colDestino);

        assertFalse(resp.ok);
        assertEquals("FUERA_DE_RANGO", resp.estado);
        assertNull(resp.resultado);
    }

    /**
     * Test 4: Sin munición → ERROR
     */
    @Test
    void testShootSinMunicion() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1();

        partida.setTurnoDe(Equipo.NAVAL);

        Dron dron1 = jug1.getDrones().get(0);
        // Consumir toda la munición
        while (dron1.getMunicion() > 0) {
            dron1.consumirMunicion();
        }
        jug1.setDronSeleccionado(dron1.getId());

        int xOrigen = dron1.getPosicion().getX();
        int yOrigen = dron1.getPosicion().getY();

        ServicioPartida.ShootResponse resp = servicio.shoot2(jug1.getId(), yOrigen + 1, xOrigen + 1);

        assertFalse(resp.ok);
        assertEquals("SIN_MUNICION", resp.estado);
    }

    /**
     * Test 5: No se permite disparar a celda vacía.
     */
    @Test
    void testShootACeldaVaciaDebeFallarSinConsumirMunicion() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1();

        partida.setTurnoDe(Equipo.NAVAL);

        Dron dron1 = jug1.getDrones().get(0);
        jug1.setDronSeleccionado(dron1.getId());

        int municionAntes = dron1.getMunicion();
        int xOrigen = dron1.getPosicion().getX();
        int yOrigen = dron1.getPosicion().getY();

        // Disparar a una celda vacía (no adyacente a ningún objetivo)
        ServicioPartida.ShootResponse resp = servicio.shoot2(jug1.getId(), yOrigen + 1, xOrigen + 1);

        assertFalse(resp.ok);
        assertEquals("NO_HAY_OBJETIVO_ENEMIGO", resp.estado);
        assertNull(resp.resultado);
        assertEquals(municionAntes, dron1.getMunicion());
    }

    /**
     * Test 6: Bomba a Dron Naval: vida baja en 1, muere en segundo impacto
     */
    @Test
    void testShootBombaADronNaval() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1(); // NAVAL (Misil)
        Jugador jug2 = partida.getJugador2(); // AEREO (Bomba)

        partida.setTurnoDe(Equipo.AEREO);

        Dron dronBomba = jug2.getDrones().get(0); // Bomba de J2
        Dron dronMisil = jug1.getDrones().get(0); // Misil de J1

        // Ubicar el dron misil de J1 adyacente al dron bomba de J2
        int xBomba = dronBomba.getPosicion().getX();
        int yBomba = dronBomba.getPosicion().getY();
        dronMisil.getPosicion().setX(xBomba);
        dronMisil.getPosicion().setY(yBomba + 1);

        jug2.setDronSeleccionado(dronBomba.getId());

        int vidaAntes = dronMisil.getVida();

        // Primer impacto (Bomba a Misil)
        ServicioPartida.ShootResponse resp1 = servicio.shoot2(jug2.getId(), yBomba + 1, xBomba);

        assertTrue(resp1.ok);
        assertEquals("HIT", resp1.resultado);
        assertEquals("DRON", resp1.objetivo);
        assertEquals(vidaAntes - 1, resp1.vidaObjetivo);
        assertTrue(dronMisil.estaVivo()); // No muere aún (vida=1, vida inicial=2)
        // Nota: para probar el segundo impacto se necesaría resetear munición del dron bombardero
        // pero la prueba verifica que el daño se aplica correctamente en el primer impacto
    }

    /**
     * Test 7: Misil a Dron Bomba: destrucción inmediata (HIT en 1 impacto)
     */
    @Test
    void testShootMisilADronBomba() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1(); // NAVAL (Misil)
        Jugador jug2 = partida.getJugador2(); // AEREO (Bomba)

        partida.setTurnoDe(Equipo.NAVAL);

        Dron dronMisil = jug1.getDrones().get(0);
        Dron dronBomba = jug2.getDrones().get(0);

        // Ubicar el misil junto al dron bomba para asegurar objetivo enemigo válido
        int xBomba = dronBomba.getPosicion().getX();
        int yBomba = dronBomba.getPosicion().getY();
        int xMisil = xBomba + 1;
        if (xMisil > Reglas.TABLERO_X_MAX) {
            xMisil = xBomba - 1;
        }
        dronMisil.getPosicion().setX(xMisil);
        dronMisil.getPosicion().setY(yBomba);

        jug1.setDronSeleccionado(dronMisil.getId());

        // Misil a Bomba = destrucción inmediata
        ServicioPartida.ShootResponse resp = servicio.shoot2(jug1.getId(), yBomba, xBomba);

        assertTrue(resp.ok);
        assertEquals("HIT", resp.resultado);
        assertEquals("DRON", resp.objetivo);
        assertEquals(0, resp.vidaObjetivo); // Muerto instantáneamente
        assertFalse(dronBomba.estaVivo());

        ServicioPartida.BoardResponse boardJugador2 = servicio.obtenerTablero(jug2.getId());
        boolean dronAunVisible = boardJugador2.drones.stream().anyMatch(d -> d.id.equals(dronBomba.getId()));
        assertFalse(dronAunVisible);
    }

    @Test
    void testShootAObjetivoDestruidoDebeFallar() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1(); // NAVAL
        Jugador jug2 = partida.getJugador2(); // AEREO

        partida.setTurnoDe(Equipo.NAVAL);

        Dron dronMisil = jug1.getDrones().get(0);
        Dron dronBomba = jug2.getDrones().get(0);

        int xBomba = dronBomba.getPosicion().getX();
        int yBomba = dronBomba.getPosicion().getY();
        int xMisil = xBomba + 1;
        if (xMisil > Reglas.TABLERO_X_MAX) {
            xMisil = xBomba - 1;
        }
        dronMisil.getPosicion().setX(xMisil);
        dronMisil.getPosicion().setY(yBomba);

        jug1.setDronSeleccionado(dronMisil.getId());

        ServicioPartida.ShootResponse resp1 = servicio.shoot2(jug1.getId(), yBomba, xBomba);
        assertTrue(resp1.ok);
        assertFalse(dronBomba.estaVivo());

        jug1.setAccionTurno(Jugador.AccionTurno.NINGUNA);
        ServicioPartida.ShootResponse resp2 = servicio.shoot2(jug1.getId(), yBomba, xBomba);
        assertFalse(resp2.ok);
        assertEquals("OBJETIVO_DESTRUIDO", resp2.estado);
    }

    /**
     * Test 8: Impacto a Porta correcta: decrementa impactos restantes
     */
    @Test
    void testShootAPortaCorrecta() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1(); // NAVAL (Misil)
        Jugador jug2 = partida.getJugador2(); // AEREO (Bomba)

        partida.setTurnoDe(Equipo.NAVAL);

        Dron dronMisil = jug1.getDrones().get(0);
        Porta portaAerea = jug2.getPorta();

        // Ubicar el dron misil cerca de la porta aérea
        java.util.List<Posicion> celdas = portaAerea.getCeldasOcupadas();
        if (!celdas.isEmpty()) {
            Posicion celdaPorta = celdas.get(0);
            dronMisil.getPosicion().setX(celdaPorta.getX() + 1);
            dronMisil.getPosicion().setY(celdaPorta.getY());
        }

        jug1.setDronSeleccionado(dronMisil.getId());

        int impactosAntes = portaAerea.getImpactosRestantes();

        // Disparo de Misil a Porta AEREO (correcto)
        ServicioPartida.ShootResponse resp = servicio.shoot2(
                jug1.getId(),
                dronMisil.getPosicion().getY(),
                dronMisil.getPosicion().getX() + 1
        );

        assertTrue(resp.ok);
        assertEquals("HIT", resp.resultado);
        assertEquals("PORTA", resp.objetivo);
        assertEquals(impactosAntes - 1, resp.impactosRestantesPorta);
    }

    /**
     * Test 9: Se puede disparar y luego mover en el mismo turno,
     * y al completar ambas acciones se pasa automáticamente al turno rival.
     */
    @Test
    void testDispararLuegoMoverYNoDispararDosVeces() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1();
        Jugador jug2 = partida.getJugador2();

        partida.setTurnoDe(Equipo.NAVAL);

        Dron dron1 = jug1.getDrones().get(0);
        jug1.setDronSeleccionado(dron1.getId());

        Dron dronEnemigo = jug2.getDrones().get(0);
        int xObjetivo = dronEnemigo.getPosicion().getX();
        int yObjetivo = dronEnemigo.getPosicion().getY();
        int xShooter = xObjetivo + 1;
        if (xShooter > Reglas.TABLERO_X_MAX) {
            xShooter = xObjetivo - 1;
        }
        dron1.getPosicion().setX(xShooter);
        dron1.getPosicion().setY(yObjetivo);

        ServicioPartida.ShootResponse resp1 = servicio.shoot2(jug1.getId(), yObjetivo, xObjetivo);
        assertTrue(resp1.ok);

        // Verificar que accionTurno fue marcado como DISPARO
        assertEquals(Jugador.AccionTurno.DISPARO, jug1.getAccionTurno());

        // Intentar mover después de disparar (debe permitirse)
        ServicioPartida.AvailableMovesResponse moves = servicio.obtenerMovimientosDisponibles(jug1.getId());
        assertTrue(moves.ok);
        assertFalse(moves.celdas.isEmpty());
        int filaMove = moves.celdas.get(0).y;
        int colMove = moves.celdas.get(0).x;
        ServicioPartida.TemplateResponse respMove = servicio.moverDron(jug1.getId(), filaMove, colMove);
        assertTrue(respMove.ok);
        assertEquals("OK", respMove.estado);

        // Intentar disparar nuevamente (debe fallar por cambio automático de turno)
        ServicioPartida.ShootResponse resp2 = servicio.shoot2(jug1.getId(), filaMove, colMove);
        assertFalse(resp2.ok);
        assertEquals("NO_ES_TU_TURNO", resp2.estado);

        Partida partidaActualizada = dao.loadActiva();
        assertEquals(Equipo.AEREO, partidaActualizada.getTurnoDe());
    }

    @Test
    void testMismoDronPuedeMoverseDeNuevoTrasCambioDeTurno() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1(); // NAVAL
        Jugador jug2 = partida.getJugador2(); // AEREO

        partida.setTurnoDe(Equipo.NAVAL);

        Dron dron = jug1.getDrones().get(0);
        jug1.setDronSeleccionado(dron.getId());

        ServicioPartida.AvailableMovesResponse moves1 = servicio.obtenerMovimientosDisponibles(jug1.getId());
        assertTrue(moves1.ok);
        assertFalse(moves1.celdas.isEmpty());

        int filaMove1 = moves1.celdas.get(0).y;
        int colMove1 = moves1.celdas.get(0).x;
        ServicioPartida.TemplateResponse respMove1 = servicio.moverDron(jug1.getId(), filaMove1, colMove1);
        assertTrue(respMove1.ok);
        assertEquals(colMove1, dron.getPosicion().getX());
        assertEquals(filaMove1, dron.getPosicion().getY());

        servicio.terminarTurno(jug1.getId());
        servicio.terminarTurno(jug2.getId());

        jug1.setDronSeleccionado(dron.getId());
        ServicioPartida.AvailableMovesResponse moves2 = servicio.obtenerMovimientosDisponibles(jug1.getId());
        assertTrue(moves2.ok);
        assertFalse(moves2.celdas.isEmpty());

        int xAntesSegundoMovimiento = dron.getPosicion().getX();
        int yAntesSegundoMovimiento = dron.getPosicion().getY();
        int filaMove2 = moves2.celdas.get(0).y;
        int colMove2 = moves2.celdas.get(0).x;
        ServicioPartida.TemplateResponse respMove2 = servicio.moverDron(jug1.getId(), filaMove2, colMove2);

        assertTrue(respMove2.ok);
        assertEquals(colMove2, dron.getPosicion().getX());
        assertEquals(filaMove2, dron.getPosicion().getY());
        assertFalse(dron.getPosicion().getX() == xAntesSegundoMovimiento
                && dron.getPosicion().getY() == yAntesSegundoMovimiento);
    }

    @Test
    void testNoMoverSobrePortaPeroSiDispararAlPorta() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1(); // NAVAL
        Jugador jug2 = partida.getJugador2(); // AEREO

        partida.setTurnoDe(Equipo.NAVAL);

        Dron dron = jug1.getDrones().get(0);
        jug1.setDronSeleccionado(dron.getId());

        Posicion celdaPortaEnemiga = jug2.getPorta().getCeldasOcupadas().get(0);
        int xPorta = celdaPortaEnemiga.getX();
        int yPorta = celdaPortaEnemiga.getY();

        int xOrigen = xPorta - 1;
        if (xOrigen < Reglas.TABLERO_X_MIN) {
            xOrigen = xPorta + 1;
        }
        dron.getPosicion().setX(xOrigen);
        dron.getPosicion().setY(yPorta);

        ServicioPartida.AvailableMovesResponse moves = servicio.obtenerMovimientosDisponibles(jug1.getId());
        assertTrue(moves.ok);
        boolean portaApareceComoMovible = moves.celdas.stream()
                .anyMatch(c -> c.x == xPorta && c.y == yPorta);
        assertFalse(portaApareceComoMovible);

        ServicioPartida.TemplateResponse moverSobrePorta = servicio.moverDron(jug1.getId(), yPorta, xPorta);
        assertFalse(moverSobrePorta.ok);
        assertEquals("CELDA_OCUPADA", moverSobrePorta.estado);

        ServicioPartida.AvailableShotsResponse shots = servicio.obtenerDisparosDisponibles(jug1.getId());
        assertTrue(shots.ok);
        boolean portaApareceComoDisparable = shots.celdas.stream()
                .anyMatch(c -> c.x == xPorta && c.y == yPorta);
        assertTrue(portaApareceComoDisparable);
    }

    @Test
    void testMoverLuegoDispararPasaTurnoAutomaticamente() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1();
        Jugador jug2 = partida.getJugador2();

        partida.setTurnoDe(Equipo.NAVAL);
        int turnoAntes = partida.getNumeroTurno();

        Dron dron = jug1.getDrones().get(0);
        jug1.setDronSeleccionado(dron.getId());

        ServicioPartida.AvailableMovesResponse moves = servicio.obtenerMovimientosDisponibles(jug1.getId());
        assertTrue(moves.ok);
        assertFalse(moves.celdas.isEmpty());
        int filaMove = moves.celdas.get(0).y;
        int colMove = moves.celdas.get(0).x;

        ServicioPartida.TemplateResponse moveResp = servicio.moverDron(jug1.getId(), filaMove, colMove);
        assertTrue(moveResp.ok);

        Dron enemigo = jug2.getDrones().get(0);
        int xObjetivo = enemigo.getPosicion().getX();
        int yObjetivo = enemigo.getPosicion().getY();
        int xShooter = xObjetivo + 1;
        if (xShooter > Reglas.TABLERO_X_MAX) {
            xShooter = xObjetivo - 1;
        }
        dron.getPosicion().setX(xShooter);
        dron.getPosicion().setY(yObjetivo);

        ServicioPartida.ShootResponse shootResp = servicio.shoot2(jug1.getId(), yObjetivo, xObjetivo);
        assertTrue(shootResp.ok);

        Partida actualizada = dao.loadActiva();
        assertEquals(Equipo.AEREO, actualizada.getTurnoDe());
        assertEquals(turnoAntes + 1, actualizada.getNumeroTurno());
    }

    @Test
    void testDispararLuegoMoverPasaTurnoAutomaticamente() {
        Partida partida = dao.loadActiva();
        Jugador jug1 = partida.getJugador1();
        Jugador jug2 = partida.getJugador2();

        partida.setTurnoDe(Equipo.NAVAL);
        int turnoAntes = partida.getNumeroTurno();

        Dron dron = jug1.getDrones().get(0);
        jug1.setDronSeleccionado(dron.getId());

        Dron enemigo = jug2.getDrones().get(0);
        int xObjetivo = enemigo.getPosicion().getX();
        int yObjetivo = enemigo.getPosicion().getY();
        int xShooter = xObjetivo + 1;
        if (xShooter > Reglas.TABLERO_X_MAX) {
            xShooter = xObjetivo - 1;
        }
        dron.getPosicion().setX(xShooter);
        dron.getPosicion().setY(yObjetivo);

        ServicioPartida.ShootResponse shootResp = servicio.shoot2(jug1.getId(), yObjetivo, xObjetivo);
        assertTrue(shootResp.ok);

        ServicioPartida.AvailableMovesResponse moves = servicio.obtenerMovimientosDisponibles(jug1.getId());
        assertTrue(moves.ok);
        assertFalse(moves.celdas.isEmpty());
        int filaMove = moves.celdas.get(0).y;
        int colMove = moves.celdas.get(0).x;

        ServicioPartida.TemplateResponse moveResp = servicio.moverDron(jug1.getId(), filaMove, colMove);
        assertTrue(moveResp.ok);

        Partida actualizada = dao.loadActiva();
        assertEquals(Equipo.AEREO, actualizada.getTurnoDe());
        assertEquals(turnoAntes + 1, actualizada.getNumeroTurno());
    }
}
