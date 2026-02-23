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
            return new JoinResponse(null, null, "", "NOMBRE_INVALIDO", null);
        }

        Jugador existente = partida.buscarJugadorPorNombre(nombreLimpio);
        if (existente != null) {
            return new JoinResponse(partida.getIdPartida(), existente.getId(), existente.getNombre(),
                    partida.getEstado().name(), existente.getEquipo() != null ? existente.getEquipo().name() : null);
        }

        // Si ya hay 2 jugadores, no deja entrar
        if (partida.getJugador1() != null && partida.getJugador2() != null) {
            return new JoinResponse(partida.getIdPartida(), null, nombreLimpio, "OCUPADO", null);
        }

        // Crear jugador nuevo
        Jugador nuevo = new Jugador(nombreLimpio);

        String equipoNormalizado = equipoDeseado == null ? "" : equipoDeseado.trim().toUpperCase();

        // Primer jugador: debe elegir equipo explícitamente
        if (partida.getJugador1() == null && partida.getJugador2() == null) {
            if (equipoNormalizado.isEmpty()) {
                return new JoinResponse(partida.getIdPartida(), null, nombreLimpio, "NECESITA_EQUIPO", null);
            }

            if (!"NAVAL".equals(equipoNormalizado) && !"AEREO".equals(equipoNormalizado)) {
                return new JoinResponse(partida.getIdPartida(), null, nombreLimpio, "EQUIPO_INVALIDO", null);
            }

            if ("NAVAL".equals(equipoNormalizado)) {
                nuevo.asignarEquipo(Equipo.NAVAL);
                partida.setJugador1(nuevo);
            } else {
                nuevo.asignarEquipo(Equipo.AEREO);
                partida.setJugador2(nuevo);
            }
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
