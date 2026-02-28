package com.skysea.server.logica.Servicio;

import com.skysea.server.logica.model.*;
import com.skysea.server.persistencia.dao.IPartidaDAO;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ServicioPartida {

    private final IPartidaDAO dao;

    public ServicioPartida(IPartidaDAO dao) {
        this.dao = dao;
    }

    public synchronized Partida getPartidaActiva() {
        return dao.loadActiva();
    }

    public synchronized void reset() {
        dao.reset();
    }


    public synchronized JoinResponse join(String nombre) {
        return join(nombre, null);
    }

    public synchronized JoinResponse join(String nombre, String equipoDeseado) {
        Partida partida = dao.loadActiva();
        EstadoPartida estadoAnterior = partida.getEstado();

        String nombreLimpio = nombre == null ? "" : nombre.trim();
        if (nombreLimpio.isEmpty()) {
            return new JoinResponse(null, null, "", "NOMBRE_INVALIDO", null, 0);
        }

        Jugador existente = partida.buscarJugadorPorNombre(nombreLimpio);
        if (existente != null) {
            String equipoNormalizadoExistente = equipoDeseado == null ? "" : equipoDeseado.trim().toUpperCase();
            boolean esPrimerJugador = existente.getId().equals(partida.getPrimerJugadorId());
            boolean aunNoEntroSegundoJugador = partida.getEstado() == EstadoPartida.ESPERANDO_RIVAL;

            if (esPrimerJugador && aunNoEntroSegundoJugador && !equipoNormalizadoExistente.isEmpty()) {
                if (!"NAVAL".equals(equipoNormalizadoExistente) && !"AEREO".equals(equipoNormalizadoExistente)) {
                    return new JoinResponse(partida.getIdPartida(), existente.getId(), existente.getNombre(),
                            "EQUIPO_INVALIDO", existente.getEquipo() != null ? existente.getEquipo().name() : null,
                            partida.numeroJugadorPorId(existente.getId()));
                }

                if ("NAVAL".equals(equipoNormalizadoExistente)) {
                    existente.asignarEquipo(Equipo.NAVAL);
                    partida.clearJugador2();
                    partida.setJugador1(existente);
                } else {
                    existente.asignarEquipo(Equipo.AEREO);
                    partida.clearJugador1();
                    partida.setJugador2(existente);
                }

                partida.setEstado(EstadoPartida.ESPERANDO_RIVAL);
                dao.save(partida);
            }

            int numeroJugador = partida.numeroJugadorPorId(existente.getId());
            return new JoinResponse(partida.getIdPartida(), existente.getId(), existente.getNombre(),
                partida.getEstado().name(), existente.getEquipo() != null ? existente.getEquipo().name() : null, numeroJugador);
        }

        // Si ya hay 2 jugadores, no deja entrar
        if (partida.getJugador1() != null && partida.getJugador2() != null) {
            return new JoinResponse(partida.getIdPartida(), null, nombreLimpio, "OCUPADO", null, 0);
        }

        // Crear jugador nuevo
        Jugador nuevo = new Jugador(nombreLimpio);

        String equipoNormalizado = equipoDeseado == null ? "" : equipoDeseado.trim().toUpperCase();

        // Primer jugador: debe elegir equipo explícitamente
        if (partida.getJugador1() == null && partida.getJugador2() == null) {
            if (equipoNormalizado.isEmpty()) {
                return new JoinResponse(partida.getIdPartida(), null, nombreLimpio, "NECESITA_EQUIPO", null, 0);
            }

            if (!"NAVAL".equals(equipoNormalizado) && !"AEREO".equals(equipoNormalizado)) {
                return new JoinResponse(partida.getIdPartida(), null, nombreLimpio, "EQUIPO_INVALIDO", null, 0);
            }

            if ("NAVAL".equals(equipoNormalizado)) {
                nuevo.asignarEquipo(Equipo.NAVAL);
                partida.setJugador1(nuevo);
            } else {
                nuevo.asignarEquipo(Equipo.AEREO);
                partida.setJugador2(nuevo);
            }

            partida.setPrimerJugadorId(nuevo.getId());
        } else {
            // Segundo jugador: se asigna automáticamente al equipo opuesto
            if (partida.getJugador1() == null) {
                nuevo.asignarEquipo(Equipo.NAVAL);
                partida.setJugador1(nuevo);
            } else {
                nuevo.asignarEquipo(Equipo.AEREO);
                partida.setJugador2(nuevo);
            }
        }

        partida.setEstado(partida.getJugador1() != null && partida.getJugador2() != null
                ? EstadoPartida.EN_JUEGO
                : EstadoPartida.ESPERANDO_RIVAL);

        if (estadoAnterior != EstadoPartida.EN_JUEGO && partida.getEstado() == EstadoPartida.EN_JUEGO) {
            partida.setNumeroTurno(1);
            partida.setTurnoDe(Equipo.NAVAL);
            partida.reiniciarTemporizadorTurno();
        }

        dao.save(partida);

        int numeroJugador = partida.numeroJugadorPorId(nuevo.getId());

        return new JoinResponse(partida.getIdPartida(), nuevo.getId(), nuevo.getNombre(),
            partida.getEstado().name(), nuevo.getEquipo().name(), numeroJugador);
    }

    public synchronized TurnoEstadoResponse obtenerEstadoTurno(String playerId) {
        Partida partida = dao.loadActiva();
        aplicarTimeoutTurnoSiCorresponde(partida);

        Jugador jugador = partida.buscarJugadorPorId(playerId);
        if (jugador == null) {
            throw new IllegalArgumentException("PLAYER_NO_ENCONTRADO");
        }

        boolean enJuego = partida.getEstado() == EstadoPartida.EN_JUEGO;
        boolean esMiTurno = enJuego && partida.esTurnoDe(playerId);
        int segundosRestantes = enJuego
                ? partida.segundosRestantesTurno(System.currentTimeMillis())
                : 0;

        return new TurnoEstadoResponse(
                partida.getIdPartida(),
                partida.getEstado().name(),
                partida.getTurnoDe().name(),
                partida.getNumeroTurno(),
                jugador.getEquipo().name(),
                partida.numeroJugadorPorId(playerId),
                esMiTurno,
                segundosRestantes,
                Partida.DURACION_TURNO_SEGUNDOS
        );
    }

    // Clase interna simple para que el Service no dependa del DTO de presentación
    public static class JoinResponse {
        public final String idPartida;
        public final String playerId;
        public final String nombre;
        public final String estadoPartida;
        public final String equipo;
        public final int numeroJugador;

        public JoinResponse(String idPartida, String playerId, String nombre, String estadoPartida, String equipo, int numeroJugador) {
            this.idPartida = idPartida;
            this.playerId = playerId;
            this.nombre = nombre;
            this.estadoPartida = estadoPartida;
            this.equipo = equipo;
            this.numeroJugador = numeroJugador;
        }
    }

    public synchronized TemplateResponse seleccionarPlantilla(String playerId, String plantilla) {
        Partida partida = dao.loadActiva();
        Jugador jugador = partida.buscarJugadorPorId(playerId);
        if (jugador == null) {
            return new TemplateResponse(false, "PLAYER_NO_ENCONTRADO", null, null);
        }

        if (plantilla == null || plantilla.trim().isEmpty()) {
            return new TemplateResponse(false, "PLANTILLA_REQUERIDA", null, null);
        }

        TipoPlantillaDespliegue tipo;
        try {
            tipo = TipoPlantillaDespliegue.fromNombre(plantilla);
        } catch (Exception e) {
            return new TemplateResponse(false, "PLANTILLA_INVALIDA", null, null);
        }

        if (tipo.getEquipo() != jugador.getEquipo()) {
            return new TemplateResponse(false, "PLANTILLA_NO_CORRESPONDE_A_EQUIPO", null, null);
        }

        if (jugador.getPorta() != null && !jugador.getDrones().isEmpty()) {
            String yaElegida = jugador.getPlantillaSeleccionada();
            if (yaElegida != null && yaElegida.equalsIgnoreCase(plantilla.trim())) {
                return new TemplateResponse(true, "OK", jugador.getEquipo().name(), yaElegida);
            }
            return new TemplateResponse(false, "PLANTILLA_YA_BLOQUEADA", jugador.getEquipo().name(), yaElegida);
        }

        FabricaDespliegue.desplegarJugador(jugador, tipo);
        dao.save(partida);

        return new TemplateResponse(true, "OK", jugador.getEquipo().name(), jugador.getPlantillaSeleccionada());
    }

    /**
     * Selecciona un dron para el jugador identificado por playerId.
     * Retorna OK únicamente si se cumplen todas las reglas de negocio:
     *   - la partida está EN_JUEGO
     *   - es el turno del jugador
     *   - el dron pertenece al jugador
     *   - el jugador no había escogido ya otro dron
     * En caso contrario se devuelve ERROR. El "equipo" se rellena cuando hay ok,
     * y la columna de plantilla se usa para propagar el id del dron seleccionado.
     */
    public synchronized TemplateResponse selectDrone(String playerId, String dronId) {
        Partida partida = dao.loadActiva();
        aplicarTimeoutTurnoSiCorresponde(partida);

        // regla 1: estado EN_JUEGO
        if (partida.getEstado() != EstadoPartida.EN_JUEGO) {
            return new TemplateResponse(false, "PARTIDA_NO_EN_JUEGO", null, null);
        }

        // regla 2: es el turno del jugador
        if (!partida.esTurnoDe(playerId)) {
            return new TemplateResponse(false, "NO_ES_TU_TURNO", null, null);
        }

        // obtengo jugador y valido existencia
        Jugador jugador = partida.buscarJugadorPorId(playerId);
        if (jugador == null) {
            return new TemplateResponse(false, "PLAYER_NO_ENCONTRADO", null, null);
        }

        // regla 3: el dron debe pertenecerle
        Dron dron = jugador.buscarDronPorId(dronId);
        if (dron == null) {
            return new TemplateResponse(false, "DRON_NO_PERTENECE_AL_JUGADOR", null, null);
        }

        String dronActual = jugador.getDronSeleccionado();
        if (dronActual != null && dronActual.equals(dronId)) {
            return new TemplateResponse(true, "OK", jugador.getEquipo().name(), dronId);
        }

        // todas las reglas cumplidas: guardar selección y persistir
        jugador.setDronSeleccionado(dronId);
        dao.save(partida);
        return new TemplateResponse(true, "OK", jugador.getEquipo().name(), dronId);
    }

    /**
     * Mueve el dron seleccionado del jugador a una casilla de destino.
     * Valida que exista selección previa y que el desplazamiento sea
     * como máximo ±2 en cada eje (cuadrado 5x5). Devuelve ERROR en caso de
     * fallo, OK en caso de éxito. Usa TemplateResponse para mantener el
     * formato del servicio.
     */
    public synchronized TemplateResponse moverDron(String playerId, int filaDestino, int colDestino) {
        Partida partida = dao.loadActiva();
        aplicarTimeoutTurnoSiCorresponde(partida);

        if (partida.getEstado() != EstadoPartida.EN_JUEGO) {
            return new TemplateResponse(false, "PARTIDA_NO_EN_JUEGO", null, null);
        }

        Jugador jugador = partida.buscarJugadorPorId(playerId);
        if (jugador == null) {
            return new TemplateResponse(false, "PLAYER_NO_ENCONTRADO", null, null);
        }

        if (!partida.esTurnoDe(playerId)) {
            return new TemplateResponse(false, "NO_ES_TU_TURNO", null, null);
        }

        if (jugador.yaMovioEsteTurno()) {
            return new TemplateResponse(false, "ACCION_YA_REALIZADA", null, null);
        }

        String seleccion = jugador.getDronSeleccionado();
        if (seleccion == null) {
            return new TemplateResponse(false, "SIN_DRON_SELECCIONADO", null, null);
        }

        Dron dron = jugador.buscarDronPorId(seleccion);
        if (dron == null || !dron.estaVivo()) {
            return new TemplateResponse(false, "DRON_INVALIDO_O_DESTRUIDO", null, null);
        }

        int actualX = dron.getPosicion().getX();
        int actualY = dron.getPosicion().getY();
        int dx = colDestino - actualX;
        int dy = filaDestino - actualY;

        if (Math.abs(dx) > dron.getRangoMovimiento() || Math.abs(dy) > dron.getRangoMovimiento()) {
            return new TemplateResponse(false, "FUERA_DE_RANGO", null, null);
        }

        if (!estaEnTablero(colDestino, filaDestino)) {
            return new TemplateResponse(false, "FUERA_DE_TABLERO", null, null);
        }

        // nueva validación: celda de destino libre (solo drones vivos)
        if (celdaOcupadaPorDron(partida, colDestino, filaDestino)) {
            return new TemplateResponse(false, "CELDA_OCUPADA", null, null);
        }

        // no se permite mover un dron sobre ninguna celda ocupada por porta
        if (celdaOcupadaPorPorta(partida, colDestino, filaDestino)) {
            return new TemplateResponse(false, "CELDA_OCUPADA", null, null);
        }

        // puede existir validación adicional en Dron.mover, pero el rango ya
        // se comprobó; usamos el método para mantener banderas internas.
        dron.mover(dx, dy);
        // ya consumió su acción de turno
        jugador.marcarMovimientoRealizado();
        avanzarTurnoSiAccionCompleta(partida, jugador);
        dao.save(partida);

        return new TemplateResponse(true, "OK", jugador.getEquipo().name(), seleccion);
    }

    public synchronized BoardResponse obtenerTablero(String playerId) {
        Partida partida = dao.loadActiva();
        aplicarTimeoutTurnoSiCorresponde(partida);
        Jugador jugador = partida.buscarJugadorPorId(playerId);
        if (jugador == null) {
            throw new IllegalArgumentException("PLAYER_NO_ENCONTRADO");
        }

        Jugador enemigo = obtenerJugadorEnemigo(partida, jugador.getId());
        Set<String> visibles = calcularCeldasVisibles(jugador);

        List<PortaView> portas = new ArrayList<>();
        List<DronView> drones = new ArrayList<>();

        agregarUnidadesPropias(jugador, portas, drones);
        agregarUnidadesEnemigasVisibles(enemigo, visibles, portas, drones);

        EquipoResumenView resumenPropio = construirResumenEquipo(jugador);
        EquipoResumenView resumenEnemigo = construirResumenEquipo(enemigo);

        List<CeldaView> celdasVisibles = visibles.stream().map(this::keyToCelda).toList();

        return new BoardResponse(
                jugador.getEquipo().name(),
                partida.getEstado().name(),
                partida.getTurnoDe().name(),
                partida.getNumeroTurno(),
                portas,
                drones,
                resumenPropio,
                resumenEnemigo,
                celdasVisibles
        );
    }

    public synchronized AvailableMovesResponse obtenerMovimientosDisponibles(String playerId) {
        Partida partida = dao.loadActiva();
        aplicarTimeoutTurnoSiCorresponde(partida);
        if (partida.getEstado() != EstadoPartida.EN_JUEGO) {
            return new AvailableMovesResponse(false, "PARTIDA_NO_EN_JUEGO", List.of());
        }

        Jugador jugador = partida.buscarJugadorPorId(playerId);
        if (jugador == null) {
            return new AvailableMovesResponse(false, "PLAYER_NO_ENCONTRADO", List.of());
        }

        if (!partida.esTurnoDe(playerId)) {
            return new AvailableMovesResponse(false, "NO_ES_TU_TURNO", List.of());
        }

        if (jugador.yaMovioEsteTurno()) {
            return new AvailableMovesResponse(false, "ACCION_YA_REALIZADA", List.of());
        }

        String dronSeleccionadoId = jugador.getDronSeleccionado();
        if (dronSeleccionadoId == null) {
            return new AvailableMovesResponse(false, "SIN_DRON_SELECCIONADO", List.of());
        }

        Dron dron = jugador.buscarDronPorId(dronSeleccionadoId);
        if (dron == null || !dron.estaVivo()) {
            return new AvailableMovesResponse(false, "DRON_INVALIDO_O_DESTRUIDO", List.of());
        }

        List<CeldaView> celdas = new ArrayList<>();
        int origenX = dron.getPosicion().getX();
        int origenY = dron.getPosicion().getY();
        int rango = dron.getRangoMovimiento();

        for (int dy = -rango; dy <= rango; dy++) {
            for (int dx = -rango; dx <= rango; dx++) {
                if (dx == 0 && dy == 0) continue;
                int x = origenX + dx;
                int y = origenY + dy;
                if (!estaEnTablero(x, y)) continue;
                if (celdaOcupadaPorDron(partida, x, y)) continue;
                if (celdaOcupadaPorPorta(partida, x, y)) continue;
                celdas.add(new CeldaView(x, y));
            }
        }

        return new AvailableMovesResponse(true, "OK", celdas);
    }

    private void agregarUnidadesPropias(Jugador jugador, List<PortaView> portas, List<DronView> drones) {
        if (jugador == null) {
            return;
        }

        if (jugador.getPorta() != null) {
            List<CeldaView> celdas = jugador.getPorta().getCeldasOcupadas().stream()
                    .map(p -> new CeldaView(p.getX(), p.getY()))
                    .toList();
            portas.add(new PortaView(
                    jugador.getPorta().getId(),
                    jugador.getEquipo().name(),
                    celdas,
                    jugador.getPorta().estaDestruido(),
                    jugador.getPorta().getImpactosRestantes()
            ));
        }

        for (Dron d : jugador.getDrones()) {
            if (!d.estaVivo()) {
                continue;
            }
            drones.add(new DronView(
                    d.getId(),
                    jugador.getEquipo().name(),
                    d.getTipoProyectil() == TipoProyectil.BOMBA ? "DRON_AEREO" : "DRON_NAVAL",
                    d.getPosicion().getX(),
                    d.getPosicion().getY(),
                    d.getVida(),
                    d.getMunicion(),
                    d.getMunicion(),
                    d.getTipoProyectil().name(),
                    d.getVida(),
                    !d.estaVivo(),
                    d.getTipoProyectil().name(),
                    d.estaVivo()
            ));
        }
    }

    private void agregarUnidadesEnemigasVisibles(Jugador enemigo, Set<String> celdasVisibles,
                                                 List<PortaView> portas, List<DronView> drones) {
        if (enemigo == null) {
            return;
        }

        if (enemigo.getPorta() != null) {
            List<CeldaView> celdasPorta = enemigo.getPorta().getCeldasOcupadas().stream()
                    .map(p -> new CeldaView(p.getX(), p.getY()))
                    .filter(c -> celdasVisibles.contains(celdaKey(c.x, c.y)))
                    .toList();

            if (!celdasPorta.isEmpty()) {
                portas.add(new PortaView(
                        enemigo.getPorta().getId(),
                        enemigo.getEquipo().name(),
                        celdasPorta,
                        enemigo.getPorta().estaDestruido(),
                        enemigo.getPorta().getImpactosRestantes()
                ));
            }
        }

        for (Dron d : enemigo.getDrones()) {
            if (!d.estaVivo()) {
                continue;
            }
            if (!celdasVisibles.contains(celdaKey(d.getPosicion().getX(), d.getPosicion().getY()))) {
                continue;
            }

            drones.add(new DronView(
                    d.getId(),
                    enemigo.getEquipo().name(),
                    d.getTipoProyectil() == TipoProyectil.BOMBA ? "DRON_AEREO" : "DRON_NAVAL",
                    d.getPosicion().getX(),
                    d.getPosicion().getY(),
                    d.getVida(),
                    d.getMunicion(),
                    d.getMunicion(),
                    d.getTipoProyectil().name(),
                    d.getVida(),
                    !d.estaVivo(),
                    d.getTipoProyectil().name(),
                    d.estaVivo()
            ));
        }
    }

    private Set<String> calcularCeldasVisibles(Jugador jugador) {
        Set<String> visibles = new HashSet<>();
        if (jugador == null) {
            return visibles;
        }

        agregarUnidadesPropiasAVisibles(jugador, visibles);

        String dronSeleccionadoId = jugador.getDronSeleccionado();
        if (dronSeleccionadoId == null) {
            return visibles;
        }

        Dron dronSeleccionado = jugador.buscarDronPorId(dronSeleccionadoId);
        if (dronSeleccionado == null || !dronSeleccionado.estaVivo()) {
            return visibles;
        }

        int x0 = dronSeleccionado.getPosicion().getX();
        int y0 = dronSeleccionado.getPosicion().getY();
        int rangoVisibilidad = dronSeleccionado.getTipoProyectil() == TipoProyectil.MISIL
                ? 2
                : dronSeleccionado.getRangoMovimiento();

        for (int dy = -rangoVisibilidad; dy <= rangoVisibilidad; dy++) {
            for (int dx = -rangoVisibilidad; dx <= rangoVisibilidad; dx++) {
                int x = x0 + dx;
                int y = y0 + dy;
                if (!estaEnTablero(x, y)) continue;
                visibles.add(celdaKey(x, y));
            }
        }

        return visibles;
    }

    private void agregarUnidadesPropiasAVisibles(Jugador jugador, Set<String> visibles) {
        if (jugador.getPorta() != null) {
            for (Posicion p : jugador.getPorta().getCeldasOcupadas()) {
                visibles.add(celdaKey(p.getX(), p.getY()));
            }
        }

        for (Dron d : jugador.getDrones()) {
            if (!d.estaVivo()) {
                continue;
            }
            visibles.add(celdaKey(d.getPosicion().getX(), d.getPosicion().getY()));
        }
    }

    private Jugador obtenerJugadorEnemigo(Partida partida, String playerId) {
        if (partida.getJugador1() != null && partida.getJugador1().getId().equals(playerId)) {
            return partida.getJugador2();
        }
        return partida.getJugador1();
    }

    private boolean celdaOcupadaPorDron(Partida partida, int x, int y) {
        for (Jugador j : List.of(partida.getJugador1(), partida.getJugador2())) {
            if (j == null) continue;
            for (Dron d : j.getDrones()) {
                if (d.estaVivo() && d.getPosicion().getX() == x && d.getPosicion().getY() == y) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean celdaOcupadaPorPorta(Partida partida, int x, int y) {
        Posicion posicion = new Posicion(x, y);
        for (Jugador j : List.of(partida.getJugador1(), partida.getJugador2())) {
            if (j == null || j.getPorta() == null) continue;
            if (j.getPorta().ocupa(posicion)) {
                return true;
            }
        }
        return false;
    }

    private EquipoResumenView construirResumenEquipo(Jugador jugador) {
        if (jugador == null) {
            return new EquipoResumenView(null, 0, 0, 0, 0);
        }

        int dronesActivos = 0;
        int municionTotal = 0;
        for (Dron d : jugador.getDrones()) {
            if (d.estaVivo()) {
                dronesActivos++;
            }
            municionTotal += d.getMunicion();
        }

        int impactosRestantes = jugador.getPorta() != null ? jugador.getPorta().getImpactosRestantes() : 0;
        int impactosMaximos = jugador.getEquipo() == Equipo.AEREO
                ? Reglas.IMPACTOS_PORTA_AEREO
                : Reglas.IMPACTOS_PORTA_NAVAL;

        return new EquipoResumenView(
                jugador.getEquipo().name(),
                dronesActivos,
                municionTotal,
                impactosRestantes,
                impactosMaximos
        );
    }

    private boolean estaEnTablero(int x, int y) {
        return x >= Reglas.TABLERO_X_MIN && x <= Reglas.TABLERO_X_MAX
                && y >= Reglas.TABLERO_Y_MIN && y <= Reglas.TABLERO_Y_MAX;
    }

    private String celdaKey(int x, int y) {
        return x + ":" + y;
    }

    private CeldaView keyToCelda(String key) {
        String[] parts = key.split(":");
        return new CeldaView(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
    }

    public static class AvailableMovesResponse {
        public final boolean ok;
        public final String estado;
        public final List<CeldaView> celdas;

        public AvailableMovesResponse(boolean ok, String estado, List<CeldaView> celdas) {
            this.ok = ok;
            this.estado = estado;
            this.celdas = celdas;
        }
    }

    public static class AvailableShotsResponse {
        public final boolean ok;
        public final String estado;
        public final List<CeldaView> celdas;

        public AvailableShotsResponse(boolean ok, String estado, List<CeldaView> celdas) {
            this.ok = ok;
            this.estado = estado;
            this.celdas = celdas;
        }
    }

    public static class EquipoResumenView {
        public final String equipo;
        public final int dronesActivos;
        public final int municionTotal;
        public final int impactosRestantesPorta;
        public final int impactosMaximosPorta;

        public EquipoResumenView(String equipo, int dronesActivos, int municionTotal,
                                 int impactosRestantesPorta, int impactosMaximosPorta) {
            this.equipo = equipo;
            this.dronesActivos = dronesActivos;
            this.municionTotal = municionTotal;
            this.impactosRestantesPorta = impactosRestantesPorta;
            this.impactosMaximosPorta = impactosMaximosPorta;
        }
    }

    public static class TemplateResponse {
        public final boolean ok;
        public final String estado;
        public final String equipo;
        public final String plantilla;

        public TemplateResponse(boolean ok, String estado, String equipo, String plantilla) {
            this.ok = ok;
            this.estado = estado;
            this.equipo = equipo;
            this.plantilla = plantilla;
        }
    }

    public static class BoardResponse {
        public final String miEquipo;
        public final String estadoPartida;
        public final String turnoDe;
        public final int numeroTurno;
        public final List<PortaView> portas;
        public final List<DronView> drones;
        public final EquipoResumenView resumenMiEquipo;
        public final EquipoResumenView resumenEnemigo;
        public final List<CeldaView> celdasVisibles;

        public BoardResponse(String miEquipo,
                             String estadoPartida,
                             String turnoDe,
                             int numeroTurno,
                             List<PortaView> portas,
                             List<DronView> drones,
                             EquipoResumenView resumenMiEquipo,
                             EquipoResumenView resumenEnemigo,
                             List<CeldaView> celdasVisibles) {
            this.miEquipo = miEquipo;
            this.estadoPartida = estadoPartida;
            this.turnoDe = turnoDe;
            this.numeroTurno = numeroTurno;
            this.portas = portas;
            this.drones = drones;
            this.resumenMiEquipo = resumenMiEquipo;
            this.resumenEnemigo = resumenEnemigo;
            this.celdasVisibles = celdasVisibles;
        }
    }

    public static class PortaView {
        public final String id;
        public final String equipo;
        public final List<CeldaView> celdas;
        public final boolean destruido;
        public final int impactosRestantes;

        public PortaView(String id, String equipo, List<CeldaView> celdas, boolean destruido, int impactosRestantes) {
            this.id = id;
            this.equipo = equipo;
            this.celdas = celdas;
            this.destruido = destruido;
            this.impactosRestantes = impactosRestantes;
        }
    }

    public static class DronView {
        public final String id;
        public final String equipo;
        public final String tipo;
        public final String type;
        public final int x;
        public final int y;
        public final int vida;
        public final int municion;
        public final int ammoRemaining;
        public final String proyectil;
        public final int hitsRemaining;
        public final boolean alive;
        public final boolean destruido;

        public DronView(String id, String equipo, String tipo, int x, int y, int vida, int municion,
                        int ammoRemaining, String proyectil, int hitsRemaining, boolean destruido,
                        String type, boolean alive) {
            this.id = id;
            this.equipo = equipo;
            this.tipo = tipo;
            this.type = type;
            this.x = x;
            this.y = y;
            this.vida = vida;
            this.municion = municion;
            this.ammoRemaining = ammoRemaining;
            this.proyectil = proyectil;
            this.hitsRemaining = hitsRemaining;
            this.alive = alive;
            this.destruido = destruido;
        }
    }

    public static class CeldaView {
        public final int x;
        public final int y;

        public CeldaView(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * Respuesta para operaciones de disparo (shoot2).
     * Contiene información sobre el resultado del impacto y el estado de las unidades después.
     */
    public static class ShootResponse {
        public final boolean ok;
        public final String estado;       // "OK" o "ERROR"
        public final String resultado;    // "HIT" o "MISS" (null si error)
        public final String objetivo;     // "DRON", "PORTA", "NADA" (null si error)
        public final Integer municionRestante;  // munición del dron disparador después
        public final Integer vidaObjetivo;      // vida del dron objetivo (si aplica)
        public final Integer impactosRestantesPorta; // impactos restantes de porta (si aplica)

        public ShootResponse(boolean ok, String estado, String resultado, String objetivo,
                             Integer municionRestante, Integer vidaObjetivo, Integer impactosRestantesPorta) {
            this.ok = ok;
            this.estado = estado;
            this.resultado = resultado;
            this.objetivo = objetivo;
            this.municionRestante = municionRestante;
            this.vidaObjetivo = vidaObjetivo;
            this.impactosRestantesPorta = impactosRestantesPorta;
        }
    }

    /**
     * Implementa el caso de uso DISPARAR.
     * Valida condiciones previas, descuenta munición, aplica daño según tipo de proyectil y objetivo,
     * y marca la acción de turno como "DISPARO".
     *
    * Reglas:
    * - Puede disparar como máximo una vez por turno.
     * - BOMBA (dron AEREO): rango adyacente (1), daña drones NAVAL y porta NAVAL.
     * - MISIL (dron NAVAL): por ahora rango 1 fijo.
     * - MISIL vs DRON AEREO: destrucción inmediata.
     * - BOMBA vs DRON NAVAL: resta 1 vida (muere en 2 impactos).
     * - MISIL vs PORTA AEREO: resta 1 impacto (destruida en 6).
     * - BOMBA vs PORTA NAVAL: resta 1 impacto (destruida en 3).
    * - Arma no compatible contra porta: MISS (sin daño).
    * - No se permite disparar a celda vacía ni a unidad propia.
    * - No se permite disparar a una unidad ya destruida.
     */
    public synchronized ShootResponse shoot2(String playerId, int filaDestino, int colDestino) {
        Partida partida = dao.loadActiva();
        aplicarTimeoutTurnoSiCorresponde(partida);

        // Validación 1: estado EN_JUEGO
        if (partida.getEstado() != EstadoPartida.EN_JUEGO) {
            return new ShootResponse(false, "PARTIDA_NO_EN_JUEGO", null, null, null, null, null);
        }

        // Validación 2: jugador existe
        Jugador jugador = partida.buscarJugadorPorId(playerId);
        if (jugador == null) {
            return new ShootResponse(false, "PLAYER_NO_ENCONTRADO", null, null, null, null, null);
        }

        // Validación 3: es su turno
        if (!partida.esTurnoDe(playerId)) {
            return new ShootResponse(false, "NO_ES_TU_TURNO", null, null, null, null, null);
        }

        if (jugador.yaDisparoEsteTurno()) {
            return new ShootResponse(false, "YA_DISPARO_EN_ESTE_TURNO", null, null, null, null, null);
        }

        // Validación 5: tiene dron seleccionado
        String dronSeleccionadoId = jugador.getDronSeleccionado();
        if (dronSeleccionadoId == null) {
            return new ShootResponse(false, "SIN_DRON_SELECCIONADO", null, null, null, null, null);
        }

        Dron dronDispara = jugador.buscarDronPorId(dronSeleccionadoId);
        if (dronDispara == null || !dronDispara.estaVivo()) {
            return new ShootResponse(false, "DRON_INVALIDO_O_DESTRUIDO", null, null, null, null, null);
        }

        // Validación 6: tiene munición
        if (dronDispara.getMunicion() <= 0) {
            return new ShootResponse(false, "SIN_MUNICION", null, null, null, null, null);
        }

        // Validación 7: está en rango
        int xOrigen = dronDispara.getPosicion().getX();
        int yOrigen = dronDispara.getPosicion().getY();
        int dx = colDestino - xOrigen;
        int dy = filaDestino - yOrigen;

        boolean enRango = Math.abs(dx) <= dronDispara.getRangoAtaque()
                && Math.abs(dy) <= dronDispara.getRangoAtaque()
                && !(dx == 0 && dy == 0);
        if (!enRango) {
            return new ShootResponse(false, "FUERA_DE_RANGO", null, null, null, null, null);
        }

        if (!estaEnTablero(colDestino, filaDestino)) {
            return new ShootResponse(false, "FUERA_DE_TABLERO", null, null, null, null, null);
        }

        Jugador jugadorEnemigo = partida.getJugador1().getId().equals(jugador.getId()) ? partida.getJugador2() : partida.getJugador1();

        if (jugador.getPorta() != null && jugador.getPorta().ocupa(new Posicion(colDestino, filaDestino))) {
            return new ShootResponse(false, "OBJETIVO_PROPIO", null, null, null, null, null);
        }
        for (Dron dronPropio : jugador.getDrones()) {
            if (dronPropio.getPosicion().getX() == colDestino && dronPropio.getPosicion().getY() == filaDestino && dronPropio.estaVivo()) {
                return new ShootResponse(false, "OBJETIVO_PROPIO", null, null, null, null, null);
            }
        }

        Dron dronObjetivoVivo = null;
        boolean objetivoDestruidoEnCelda = false;
        for (Dron dronEnemigo : jugadorEnemigo.getDrones()) {
            if (dronEnemigo.getPosicion().getX() == colDestino && dronEnemigo.getPosicion().getY() == filaDestino) {
                if (dronEnemigo.estaVivo()) {
                    dronObjetivoVivo = dronEnemigo;
                    break;
                }
                objetivoDestruidoEnCelda = true;
            }
        }

        Porta portaObjetivo = null;
        if (jugadorEnemigo.getPorta() != null && jugadorEnemigo.getPorta().ocupa(new Posicion(colDestino, filaDestino))) {
            portaObjetivo = jugadorEnemigo.getPorta();
        }

        if (dronObjetivoVivo == null && portaObjetivo == null) {
            if (objetivoDestruidoEnCelda) {
                return new ShootResponse(false, "OBJETIVO_DESTRUIDO", null, null, null, null, null);
            }
            return new ShootResponse(false, "NO_HAY_OBJETIVO_ENEMIGO", null, null, null, null, null);
        }

        // Consumir munición una vez validadas todas las precondiciones de objetivo
        dronDispara.consumirMunicion();
        TipoProyectil proyectil = dronDispara.getTipoProyectil();

        if (dronObjetivoVivo != null) {
            dronObjetivoVivo.recibirImpacto(proyectil);
            String resultado = "HIT";
            Integer vidaRestante = dronObjetivoVivo.estaVivo() ? dronObjetivoVivo.getVida() : 0;
            jugador.marcarDisparoRealizado();
            avanzarTurnoSiAccionCompleta(partida, jugador);
            dao.save(partida);
            return new ShootResponse(true, "OK", resultado, "DRON", dronDispara.getMunicion(), vidaRestante, null);
        }

        if (portaObjetivo != null) {
            boolean impactoRecibido = portaObjetivo.recibirImpacto(proyectil);
            if (impactoRecibido) {
                String resultado = "HIT";
                Integer impactosRestantes = portaObjetivo.getImpactosRestantes();
                jugador.marcarDisparoRealizado();
                avanzarTurnoSiAccionCompleta(partida, jugador);
                dao.save(partida);
                return new ShootResponse(true, "OK", resultado, "PORTA", dronDispara.getMunicion(), null, impactosRestantes);
            } else {
                // Arma no compatible contra esta porta (MISS)
                String resultado = "MISS";
                jugador.marcarDisparoRealizado();
                avanzarTurnoSiAccionCompleta(partida, jugador);
                dao.save(partida);
                return new ShootResponse(true, "OK", resultado, "PORTA", dronDispara.getMunicion(), null, null);
            }
        }

        return new ShootResponse(false, "NO_HAY_OBJETIVO_ENEMIGO", null, null, null, null, null);
    }

    public synchronized AvailableShotsResponse obtenerDisparosDisponibles(String playerId) {
        Partida partida = dao.loadActiva();
        aplicarTimeoutTurnoSiCorresponde(partida);
        if (partida.getEstado() != EstadoPartida.EN_JUEGO) {
            return new AvailableShotsResponse(false, "PARTIDA_NO_EN_JUEGO", List.of());
        }

        Jugador jugador = partida.buscarJugadorPorId(playerId);
        if (jugador == null) {
            return new AvailableShotsResponse(false, "PLAYER_NO_ENCONTRADO", List.of());
        }

        if (!partida.esTurnoDe(playerId)) {
            return new AvailableShotsResponse(false, "NO_ES_TU_TURNO", List.of());
        }

        String dronSeleccionadoId = jugador.getDronSeleccionado();
        if (dronSeleccionadoId == null) {
            return new AvailableShotsResponse(false, "SIN_DRON_SELECCIONADO", List.of());
        }

        Dron dron = jugador.buscarDronPorId(dronSeleccionadoId);
        if (dron == null || !dron.estaVivo()) {
            return new AvailableShotsResponse(false, "DRON_INVALIDO_O_DESTRUIDO", List.of());
        }

        if (dron.getMunicion() <= 0) {
            return new AvailableShotsResponse(false, "SIN_MUNICION", List.of());
        }

        List<CeldaView> celdas = new ArrayList<>();
        int origenX = dron.getPosicion().getX();
        int origenY = dron.getPosicion().getY();
        int rango = dron.getRangoAtaque();
        Jugador jugadorEnemigo = partida.getJugador1().getId().equals(jugador.getId()) ? partida.getJugador2() : partida.getJugador1();

        for (int dy = -rango; dy <= rango; dy++) {
            for (int dx = -rango; dx <= rango; dx++) {
                if (dx == 0 && dy == 0) continue;
                int x = origenX + dx;
                int y = origenY + dy;
                if (!estaEnTablero(x, y)) continue;

                boolean tieneObjetivo = false;
                if (jugadorEnemigo != null) {
                    for (Dron dronEnemigo : jugadorEnemigo.getDrones()) {
                        if (dronEnemigo.estaVivo()
                                && dronEnemigo.getPosicion().getX() == x
                                && dronEnemigo.getPosicion().getY() == y) {
                            tieneObjetivo = true;
                            break;
                        }
                    }

                    if (!tieneObjetivo
                            && jugadorEnemigo.getPorta() != null
                            && !jugadorEnemigo.getPorta().estaDestruido()
                            && jugadorEnemigo.getPorta().ocupa(new Posicion(x, y))) {
                        tieneObjetivo = true;
                    }
                }

                if (!tieneObjetivo) continue;
                celdas.add(new CeldaView(x, y));
            }
        }

        return new AvailableShotsResponse(true, "OK", celdas);
    }

    public synchronized Partida terminarTurno(String playerId) {
    Partida partida = dao.loadActiva();
    aplicarTimeoutTurnoSiCorresponde(partida);

    // Validación: jugador existe
    Jugador j = partida.buscarJugadorPorId(playerId);
    if (j == null) {
        throw new IllegalArgumentException("PLAYER_NO_ENCONTRADO");
    }

    // Validación: es su turno
    if (!partida.esTurnoDe(playerId)) {
        throw new IllegalStateException("NO_ES_TU_TURNO");
    }
        
    avanzarTurno(partida);

    dao.save(partida);
    return partida;
}

    private void aplicarTimeoutTurnoSiCorresponde(Partida partida) {
        if (partida.getEstado() != EstadoPartida.EN_JUEGO) {
            return;
        }

        int restantes = partida.segundosRestantesTurno(System.currentTimeMillis());
        if (restantes > 0) {
            return;
        }

        avanzarTurno(partida);
        dao.save(partida);
    }

    private void avanzarTurnoSiAccionCompleta(Partida partida, Jugador jugador) {
        if (jugador != null && jugador.getAccionTurno() == Jugador.AccionTurno.MOVIO_Y_DISPARO) {
            avanzarTurno(partida);
        }
    }

    private void avanzarTurno(Partida partida) {
        Jugador jugadorTurnoActual = jugadorPorEquipo(partida, partida.getTurnoDe());
        if (jugadorTurnoActual != null) {
            jugadorTurnoActual.setDronSeleccionado(null);
            jugadorTurnoActual.setAccionTurno(Jugador.AccionTurno.NINGUNA);
            jugadorTurnoActual.resetAccionesTurno();
            for (Dron dron : jugadorTurnoActual.getDrones()) {
                dron.resetAccionesTurno();
            }
        }

        if (partida.getTurnoDe() == Equipo.NAVAL) {
            partida.setTurnoDe(Equipo.AEREO);
        } else {
            partida.setTurnoDe(Equipo.NAVAL);
        }

        partida.setNumeroTurno(partida.getNumeroTurno() + 1);
        partida.reiniciarTemporizadorTurno();
    }

    private Jugador jugadorPorEquipo(Partida partida, Equipo equipo) {
        if (partida.getJugador1() != null && partida.getJugador1().getEquipo() == equipo) {
            return partida.getJugador1();
        }
        if (partida.getJugador2() != null && partida.getJugador2().getEquipo() == equipo) {
            return partida.getJugador2();
        }
        return null;
    }

    public static class TurnoEstadoResponse {
        public final String idPartida;
        public final String estadoPartida;
        public final String turnoDe;
        public final int numeroTurno;
        public final String equipo;
        public final int numeroJugador;
        public final boolean esMiTurno;
        public final int segundosRestantesTurno;
        public final int duracionTurnoSegundos;

        public TurnoEstadoResponse(String idPartida,
                                   String estadoPartida,
                                   String turnoDe,
                                   int numeroTurno,
                                   String equipo,
                                   int numeroJugador,
                                   boolean esMiTurno,
                                   int segundosRestantesTurno,
                                   int duracionTurnoSegundos) {
            this.idPartida = idPartida;
            this.estadoPartida = estadoPartida;
            this.turnoDe = turnoDe;
            this.numeroTurno = numeroTurno;
            this.equipo = equipo;
            this.numeroJugador = numeroJugador;
            this.esMiTurno = esMiTurno;
            this.segundosRestantesTurno = segundosRestantesTurno;
            this.duracionTurnoSegundos = duracionTurnoSegundos;
        }
    }


}