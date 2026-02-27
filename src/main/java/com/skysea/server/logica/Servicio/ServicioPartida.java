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

    public void reset() {
        dao.reset();
    }


    public JoinResponse join(String nombre) {
        return join(nombre, null);
    }

    public JoinResponse join(String nombre, String equipoDeseado) {
        Partida partida = dao.loadActiva();

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

        dao.save(partida);

        int numeroJugador = partida.numeroJugadorPorId(nuevo.getId());

        return new JoinResponse(partida.getIdPartida(), nuevo.getId(), nuevo.getNombre(),
            partida.getEstado().name(), nuevo.getEquipo().name(), numeroJugador);
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

    public TemplateResponse seleccionarPlantilla(String playerId, String plantilla) {
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
    public TemplateResponse selectDrone(String playerId, String dronId) {
        Partida partida = dao.loadActiva();

        // regla 1: estado EN_JUEGO
        if (partida.getEstado() != EstadoPartida.EN_JUEGO) {
            return new TemplateResponse(false, "ERROR", null, null);
        }

        // regla 2: es el turno del jugador
        if (!partida.esTurnoDe(playerId)) {
            return new TemplateResponse(false, "ERROR", null, null);
        }

        // obtengo jugador y valido existencia
        Jugador jugador = partida.buscarJugadorPorId(playerId);
        if (jugador == null) {
            return new TemplateResponse(false, "ERROR", null, null);
        }

        // regla 3: el dron debe pertenecerle
        Dron dron = jugador.buscarDronPorId(dronId);
        if (dron == null) {
            return new TemplateResponse(false, "ERROR", null, null);
        }

        // regla 4: no había seleccionado otro dron previamente
        if (jugador.getDronSeleccionado() != null) {
            return new TemplateResponse(false, "ERROR", null, null);
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
    public TemplateResponse moverDron(String playerId, int filaDestino, int colDestino) {
        Partida partida = dao.loadActiva();
        Jugador jugador = partida.buscarJugadorPorId(playerId);
        if (jugador == null) {
            return new TemplateResponse(false, "ERROR", null, null);
        }

        if (jugador.yaMovioEsteTurno()) {
            return new TemplateResponse(false, "YA_MOVIO_EN_ESTE_TURNO", null, null);
        }

        String seleccion = jugador.getDronSeleccionado();
        if (seleccion == null) {
            return new TemplateResponse(false, "ERROR", null, null);
        }

        Dron dron = jugador.buscarDronPorId(seleccion);
        if (dron == null) {
            return new TemplateResponse(false, "ERROR", null, null);
        }

        int actualX = dron.getPosicion().getX();
        int actualY = dron.getPosicion().getY();
        int dx = colDestino - actualX;
        int dy = filaDestino - actualY;

        if (Math.abs(dx) > 2 || Math.abs(dy) > 2) {
            return new TemplateResponse(false, "ERROR", null, null);
        }

        // nueva validación: celda de destino libre
        for (Jugador j : java.util.List.of(partida.getJugador1(), partida.getJugador2())) {
            if (j == null) continue;
            for (Dron d : j.getDrones()) {
                if (d.getPosicion().getX() == colDestino && d.getPosicion().getY() == filaDestino) {
                    // destino ya ocupado
                    return new TemplateResponse(false, "ERROR", null, null);
                }
            }
        }

        // puede existir validación adicional en Dron.mover, pero el rango ya
        // se comprobó; usamos el método para mantener banderas internas.
        dron.mover(dx, dy);
        // ya consumió su acción de turno
        jugador.marcarMovimientoRealizado();
        dao.save(partida);

        return new TemplateResponse(true, "OK", jugador.getEquipo().name(), seleccion);
    }

    public BoardResponse obtenerTablero(String playerId) {
        Partida partida = dao.loadActiva();
        Jugador jugador = partida.buscarJugadorPorId(playerId);
        if (jugador == null) {
            throw new IllegalArgumentException("PLAYER_NO_ENCONTRADO");
        }

        java.util.List<PortaView> portas = new java.util.ArrayList<>();
        java.util.List<DronView> drones = new java.util.ArrayList<>();

        agregarUnidadesJugador(partida.getJugador1(), portas, drones);
        agregarUnidadesJugador(partida.getJugador2(), portas, drones);

        return new BoardResponse(
                jugador.getEquipo().name(),
                partida.getEstado().name(),
                partida.getTurnoDe().name(),
                partida.getNumeroTurno(),
                portas,
                drones
        );
    }

    private void agregarUnidadesJugador(Jugador jugador, java.util.List<PortaView> portas, java.util.List<DronView> drones) {
        if (jugador == null) {
            return;
        }

        if (jugador.getPorta() != null) {
            java.util.List<CeldaView> celdas = jugador.getPorta().getCeldasOcupadas().stream()
                    .map(p -> new CeldaView(p.getX(), p.getY()))
                    .toList();
            portas.add(new PortaView(
                    jugador.getPorta().getId(),
                    jugador.getEquipo().name(),
                    celdas,
                    jugador.getPorta().estaDestruido()
            ));
        }

        for (Dron d : jugador.getDrones()) {
            drones.add(new DronView(
                    d.getId(),
                    jugador.getEquipo().name(),
                    d.getTipoProyectil() == TipoProyectil.BOMBA ? "DRON_AEREO" : "DRON_NAVAL",
                    d.getPosicion().getX(),
                    d.getPosicion().getY(),
                    d.getVida(),
                    d.getMunicion(),
                    !d.estaVivo()
            ));
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
        public final java.util.List<PortaView> portas;
        public final java.util.List<DronView> drones;

        public BoardResponse(String miEquipo,
                             String estadoPartida,
                             String turnoDe,
                             int numeroTurno,
                             java.util.List<PortaView> portas,
                             java.util.List<DronView> drones) {
            this.miEquipo = miEquipo;
            this.estadoPartida = estadoPartida;
            this.turnoDe = turnoDe;
            this.numeroTurno = numeroTurno;
            this.portas = portas;
            this.drones = drones;
        }
    }

    public static class PortaView {
        public final String id;
        public final String equipo;
        public final java.util.List<CeldaView> celdas;
        public final boolean destruido;

        public PortaView(String id, String equipo, java.util.List<CeldaView> celdas, boolean destruido) {
            this.id = id;
            this.equipo = equipo;
            this.celdas = celdas;
            this.destruido = destruido;
        }
    }

    public static class DronView {
        public final String id;
        public final String equipo;
        public final String tipo;
        public final int x;
        public final int y;
        public final int vida;
        public final int municion;
        public final boolean destruido;

        public DronView(String id, String equipo, String tipo, int x, int y, int vida, int municion, boolean destruido) {
            this.id = id;
            this.equipo = equipo;
            this.tipo = tipo;
            this.x = x;
            this.y = y;
            this.vida = vida;
            this.municion = municion;
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
     * - Una acción por turno (mover o disparar).
     * - BOMBA (dron AEREO): rango adyacente (1), daña drones NAVAL y porta NAVAL.
     * - MISIL (dron NAVAL): por ahora rango 1 fijo (TODO: visibilidad real).
     * - MISIL vs DRON AEREO: destrucción inmediata.
     * - BOMBA vs DRON NAVAL: resta 1 vida (muere en 2 impactos).
     * - MISIL vs PORTA AEREO: resta 1 impacto (destruida en 6).
     * - BOMBA vs PORTA NAVAL: resta 1 impacto (destruida en 3).
     * - Arma no compatible contra porta: MISS (sin daño).
     */
    public ShootResponse shoot2(String playerId, int filaDestino, int colDestino) {
        Partida partida = dao.loadActiva();

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

        // TODO: reemplazar rango fijo 1 por cálculo real de visibilidad del dron misil
        boolean enRango = Math.abs(dx) <= 1 && Math.abs(dy) <= 1 && !(dx == 0 && dy == 0);
        if (!enRango) {
            return new ShootResponse(false, "FUERA_DE_RANGO", null, null, null, null, null);
        }

        // Consumir munición (antes de evaluar impacto)
        dronDispara.consumirMunicion();
        TipoProyectil proyectil = dronDispara.getTipoProyectil();

        // Buscar objetivo en la celda de destino
        Jugador jugadorEnemigo = partida.getJugador1().getId().equals(jugador.getId()) ? partida.getJugador2() : partida.getJugador1();

        // Primero verificar si hay un dron en esa posición
        for (Dron dronEnemigo : jugadorEnemigo.getDrones()) {
            if (dronEnemigo.getPosicion().getX() == colDestino && dronEnemigo.getPosicion().getY() == filaDestino && dronEnemigo.estaVivo()) {
                // HIT en dron
                dronEnemigo.recibirImpacto(proyectil);
                String resultado = "HIT";
                Integer vidaRestante = dronEnemigo.estaVivo() ? dronEnemigo.getVida() : 0;
                jugador.marcarDisparoRealizado();                       
                dao.save(partida);
                return new ShootResponse(true, "OK", resultado, "DRON", dronDispara.getMunicion(), vidaRestante, null);
            }
        }

        // Luego verificar si hay una porta en esa posición
        if (jugadorEnemigo.getPorta() != null && jugadorEnemigo.getPorta().ocupa(new Posicion(colDestino, filaDestino))) {
            Porta porta = jugadorEnemigo.getPorta();
            boolean impactoRecibido = porta.recibirImpacto(proyectil);
            if (impactoRecibido) {
                String resultado = "HIT";
                Integer impactosRestantes = porta.getImpactosRestantes();
                jugador.marcarDisparoRealizado();
                dao.save(partida);
                return new ShootResponse(true, "OK", resultado, "PORTA", dronDispara.getMunicion(), null, impactosRestantes);
            } else {
                // Arma no compatible contra esta porta (MISS)
                String resultado = "MISS";
                jugador.marcarDisparoRealizado();
                dao.save(partida);
                return new ShootResponse(true, "OK", resultado, "PORTA", dronDispara.getMunicion(), null, null);
            }
        }

        // MISS: no hay nada en la celda de destino
        String resultado = "MISS";
        jugador.marcarDisparoRealizado();
        dao.save(partida);
        return new ShootResponse(true, "OK", resultado, "NADA", dronDispara.getMunicion(), null, null);
    }

    public Partida terminarTurno(String playerId) {
    Partida partida = dao.loadActiva();

    // Validación: jugador existe
    Jugador j = partida.buscarJugadorPorId(playerId);
    if (j == null) {
        throw new IllegalArgumentException("PLAYER_NO_ENCONTRADO");
    }

    if (j.getAccionTurno() == Jugador.AccionTurno.NINGUNA) {
        throw new IllegalStateException("DEBE_REALIZAR_ACCION");
    }

    // Validación: es su turno
    if (!partida.esTurnoDe(playerId)) {
        throw new IllegalStateException("NO_ES_TU_TURNO");
    }
        
    j.setDronSeleccionado(null);
    j.setAccionTurno(Jugador.AccionTurno.NINGUNA);
    j.resetAccionesTurno();

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