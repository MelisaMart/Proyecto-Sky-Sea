package com.skysea.server.logica.Servicio;

import com.skysea.server.logica.model.EstadoPartida;
import com.skysea.server.logica.model.Partida;
import com.skysea.server.persistencia.memory.InMemoryPartidaDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class ServicioPartidaConcurrencyTest {

    private ServicioPartida servicio;
    private InMemoryPartidaDAO dao;

    @BeforeEach
    void setUp() {
        dao = new InMemoryPartidaDAO();
        servicio = new ServicioPartida(dao);
    }

    @Test
    void elPrimerUsuarioQueSeRegistraEsJugador1AunqueElijaAereo() {
        ServicioPartida.JoinResponse primero = servicio.join("Lucia", "AEREO");
        ServicioPartida.JoinResponse segundo = servicio.join("Mateo", "NAVAL");

        assertNotNull(primero.playerId);
        assertNotNull(segundo.playerId);
        assertEquals(1, primero.numeroJugador);
        assertEquals(2, segundo.numeroJugador);

        Partida partida = dao.loadActiva();
        assertEquals(primero.playerId, partida.getPrimerJugadorId());
        assertEquals(EstadoPartida.EN_JUEGO, partida.getEstado());
    }

    @Test
    void alEntrarSegundoJugadorNoSePierdeElPrimero() {
        ServicioPartida.JoinResponse primero = servicio.join("JugadorUno", "NAVAL");
        ServicioPartida.JoinResponse segundo = servicio.join("JugadorDos", "AEREO");

        Partida partida = dao.loadActiva();

        assertNotNull(partida.buscarJugadorPorId(primero.playerId));
        assertNotNull(partida.buscarJugadorPorId(segundo.playerId));
        assertEquals(1, partida.numeroJugadorPorId(primero.playerId));
        assertEquals(2, partida.numeroJugadorPorId(segundo.playerId));
        assertEquals(EstadoPartida.EN_JUEGO, partida.getEstado());
    }

    @Test
    void joinConcurrenteMantieneDosJugadoresSinExpulsar() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        Callable<ServicioPartida.JoinResponse> tarea1 = () -> {
            ready.countDown();
            assertTrue(start.await(3, TimeUnit.SECONDS));
            return servicio.join("Alpha", "NAVAL");
        };

        Callable<ServicioPartida.JoinResponse> tarea2 = () -> {
            ready.countDown();
            assertTrue(start.await(3, TimeUnit.SECONDS));
            return servicio.join("Bravo", "AEREO");
        };

        Future<ServicioPartida.JoinResponse> f1 = pool.submit(tarea1);
        Future<ServicioPartida.JoinResponse> f2 = pool.submit(tarea2);

        assertTrue(ready.await(3, TimeUnit.SECONDS));
        start.countDown();

        ServicioPartida.JoinResponse j1 = f1.get(3, TimeUnit.SECONDS);
        ServicioPartida.JoinResponse j2 = f2.get(3, TimeUnit.SECONDS);

        pool.shutdownNow();

        assertNotNull(j1.playerId);
        assertNotNull(j2.playerId);
        assertNotEquals(j1.playerId, j2.playerId);

        Partida partida = dao.loadActiva();
        assertNotNull(partida.buscarJugadorPorId(j1.playerId));
        assertNotNull(partida.buscarJugadorPorId(j2.playerId));
        assertEquals(EstadoPartida.EN_JUEGO, partida.getEstado());
    }

    @Test
    void bloqueaNombreSiYaEstaEnPartidaEsperandoRival() {
        ServicioPartida.JoinResponse primero = servicio.join("NombreRepetido", "NAVAL");
        ServicioPartida.JoinResponse segundo = servicio.join("NombreRepetido", "AEREO");

        assertNotNull(primero.playerId);
        assertEquals("USUARIO_EN_PARTIDA_ACTIVA", segundo.estadoPartida);
        assertNull(segundo.playerId);
    }

    @Test
    void permiteReusarNombreSiPartidaYaNoEstaActivaEnJuego() {
        ServicioPartida.JoinResponse primero = servicio.join("NombreFinalizado", "NAVAL");
        assertNotNull(primero.playerId);

        Partida partida = dao.loadActiva();
        partida.setEstado(EstadoPartida.FINALIZADA);
        dao.save(partida);

        ServicioPartida.JoinResponse nuevoIngreso = servicio.join("NombreFinalizado", "AEREO");
        assertNotNull(nuevoIngreso.playerId);
        assertNotEquals("USUARIO_EN_PARTIDA_ACTIVA", nuevoIngreso.estadoPartida);
    }

    @Test
    void salirEnEsperaCancelaCupoDelJugador() {
        ServicioPartida.JoinResponse j1 = servicio.join("SoloEnEspera", "NAVAL");
        assertNotNull(j1.playerId);

        ServicioPartida.DisconnectResponse salida = servicio.salirAlMenuDesdeEspera(j1.playerId);
        assertTrue(salida.ok);

        ServicioPartida.JoinResponse nuevo = servicio.join("SoloEnEspera", "AEREO");
        assertNotNull(nuevo.playerId);
    }

    @Test
    void salirEnEsperaNoDejaPartidaReanudable() {
        ServicioPartida.JoinResponse j1 = servicio.join("NoReanudar", "NAVAL");
        assertNotNull(j1.playerId);

        ServicioPartida.DisconnectResponse salida = servicio.salirAlMenuDesdeEspera(j1.playerId);
        assertTrue(salida.ok);
        assertEquals("PARTIDA_FINALIZADA", salida.estado);
        assertFalse(salida.isReanudable);

        var reanudables = servicio.listarPartidasReanudables("NoReanudar");
        assertTrue(reanudables.isEmpty());
    }

    @Test
    void nombreActivoPuedeReanudarSuPartida() {
        ServicioPartida.JoinResponse j1 = servicio.join("ReanudaActivo", "NAVAL");
        assertNotNull(j1.playerId);

        var reanudables = servicio.listarPartidasReanudables("ReanudaActivo");
        assertFalse(reanudables.isEmpty());

        var resume = servicio.reanudarPartida("ReanudaActivo", reanudables.get(0).idPartida);
        assertTrue(resume.ok);
        assertEquals("ReanudaActivo", resume.nombre);
    }
}
