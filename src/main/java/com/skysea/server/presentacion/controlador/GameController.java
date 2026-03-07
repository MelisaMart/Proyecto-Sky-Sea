package com.skysea.server.presentacion.controlador;

import com.skysea.server.logica.model.EstadoJuego;
import com.skysea.server.logica.Servicio.ServicioJuego;
import com.skysea.server.presentacion.dto.GameStateDTO;
import com.skysea.server.presentacion.dto.ShotDTO;
import org.springframework.web.bind.annotation.*;
import com.skysea.server.logica.Servicio.ServicioPartida;
import com.skysea.server.presentacion.dto.JoinResponseDTO;


@RestController
@RequestMapping("/api/game")
@CrossOrigin // para que el front web pueda consumirlo
public class GameController {

    private final ServicioJuego servicio;
    private final ServicioPartida servicioPartida;

    public GameController(ServicioJuego servicio, ServicioPartida servicioPartida) {
        this.servicio = servicio;
        this.servicioPartida = servicioPartida;
    }

    @GetMapping("/state")
    public GameStateDTO state() {
        return toDTO(servicio.getEstado());
    }

    @PostMapping("/fire")
    public GameStateDTO fire() {
        return toDTO(servicio.disparar());
    }

    @PostMapping("/reset")
    public void reset() {
        servicio.reset();
    }

    @GetMapping("/reset")
    public Object resetDesdeNavegador() {
        servicio.reset();
        return java.util.Map.of("ok", true, "mensaje", "Partida reiniciada");
    }

    @PostMapping("/resetPartida")
    public Object resetPartida() {
        servicioPartida.reset();
        return java.util.Map.of("ok", true, "mensaje", "Partida de jugadores reiniciada");
    }

    @GetMapping("/resetPartida")
    public Object resetPartidaDesdeNavegador() {
        servicioPartida.reset();
        return java.util.Map.of("ok", true, "mensaje", "Partida de jugadores reiniciada");
    }

    @PostMapping("/move")
    public GameStateDTO move(@RequestParam int dx, @RequestParam int dy) {
        return toDTO(servicio.mover(dx, dy));
    }

    @PostMapping("/endTurn")
    public GameStateDTO endTurn() {
        return toDTO(servicio.terminarTurno());
    }

    @PostMapping("/join")
    public JoinResponseDTO join(@RequestParam String nombre,
                                @RequestParam(required = false) String equipo) {
        ServicioPartida.JoinResponse r = servicioPartida.join(nombre, equipo);
        return new JoinResponseDTO(r.idPartida, r.playerId, r.nombre, r.estadoPartida, r.equipo, r.numeroJugador);
    }

    @GetMapping("/resumableMatches")
    public Object listarPartidasReanudables(@RequestParam String nombre) {
        try {
            var partidas = servicioPartida.listarPartidasReanudables(nombre);
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("ok", true);
            out.put("partidas", partidas);
            return out;
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage());
        }
    }

    @PostMapping("/resume")
    public Object reanudarPartida(@RequestParam String nombre, @RequestParam String idPartida) {
        try {
            var r = servicioPartida.reanudarPartida(nombre, idPartida);
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("ok", r.ok);
            out.put("estado", r.estado);
            out.put("idPartida", r.idPartida);
            out.put("playerId", r.playerId);
            out.put("nombre", r.nombre);
            out.put("estadoPartida", r.estadoPartida);
            out.put("equipo", r.equipo);
            out.put("numeroJugador", r.numeroJugador);
            out.put("esperandoRival", r.esperandoRival);
            return out;
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/state2")
    public Object state2(@RequestParam String playerId) {
        try {
            var estado = servicioPartida.obtenerEstadoTurno(playerId);
            java.util.Map<String,Object> out = new java.util.HashMap<>();
            out.put("idPartida", estado.idPartida);
            out.put("estadoPartida", estado.estadoPartida);
            out.put("turnoDe", estado.turnoDe);
            out.put("numeroTurno", estado.numeroTurno);
            out.put("equipo", estado.equipo);
            out.put("numeroJugador", estado.numeroJugador);
            out.put("esMiTurno", estado.esMiTurno);
            out.put("segundosRestantesTurno", estado.segundosRestantesTurno);
            out.put("duracionTurnoSegundos", estado.duracionTurnoSegundos);
            // campos de fin de partida
            out.put("partidaFinalizada", estado.partidaFinalizada);
            out.put("ganador", estado.ganador);
            out.put("motivoFin", estado.motivoFin);
            out.put("isReanudable", estado.isReanudable);
            return out;
        } catch (Exception e) {
            return java.util.Map.of(
                    "error", e.getMessage()
            );
        }
    }

    @PostMapping("/abandon")
    public Object abandonar(@RequestParam String playerId) {
        try {
            ServicioPartida.AbandonResponse r = servicioPartida.abandonarPartida(playerId);
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("ok", r.ok);
            out.put("estado", r.estado);
            out.put("ganador", r.ganador);
            out.put("motivoFin", r.motivoFin);
            out.put("isReanudable", r.isReanudable);
            return out;
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage());
        }
    }

    @PostMapping("/disconnect")
    public Object desconectar(@RequestParam String playerId) {
        try {
            ServicioPartida.DisconnectResponse r = servicioPartida.desconectarJugador(playerId);
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("ok", r.ok);
            out.put("estado", r.estado);
            out.put("estadoPartida", r.estadoPartida);
            out.put("isReanudable", r.isReanudable);
            return out;
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage());
        }
    }

    @PostMapping("/template")
    public Object seleccionarPlantilla(@RequestParam String playerId,
                                       @RequestParam String plantilla) {
        ServicioPartida.TemplateResponse r = servicioPartida.seleccionarPlantilla(playerId, plantilla);
        java.util.Map<String, Object> out = new java.util.HashMap<>();
        out.put("ok", r.ok);
        out.put("estado", r.estado);
        out.put("equipo", r.equipo);
        out.put("plantilla", r.plantilla);
        return out;
    }

    @GetMapping("/board")
    public Object board(@RequestParam String playerId) {
        try {
            return servicioPartida.obtenerTablero(playerId);
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage());
        }
    }

    @PostMapping("/endTurn2")
    public Object endTurn2(@RequestParam String playerId) {
        try {
            servicioPartida.terminarTurno(playerId);
            var estado = servicioPartida.obtenerEstadoTurno(playerId);
            return java.util.Map.of(
                    "idPartida", estado.idPartida,
                    "estadoPartida", estado.estadoPartida,
                    "turnoDe", estado.turnoDe,
                    "numeroTurno", estado.numeroTurno,
                    "esMiTurno", estado.esMiTurno,
                    "segundosRestantesTurno", estado.segundosRestantesTurno,
                    "duracionTurnoSegundos", estado.duracionTurnoSegundos
            );
        } catch (Exception e) {
            return java.util.Map.of(
                    "error", e.getMessage()
            );
        }
    }

    @PostMapping("/move2")
    public Object move2(@RequestParam String playerId,
                        @RequestParam int fila,
                        @RequestParam int col) {
        try {
            ServicioPartida.TemplateResponse r = servicioPartida.moverDron(playerId, fila, col);
            java.util.Map<String,Object> out = new java.util.HashMap<>();
            out.put("ok", r.ok);
            out.put("estado", r.estado);
            out.put("fila", fila);
            out.put("col", col);
            out.put("dronId", r.plantilla); // id del dron para debug
            return out;
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/availableMoves2")
    public Object availableMoves2(@RequestParam String playerId) {
        try {
            ServicioPartida.AvailableMovesResponse r = servicioPartida.obtenerMovimientosDisponibles(playerId);
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("ok", r.ok);
            out.put("estado", r.estado);
            out.put("celdas", r.celdas);
            return out;
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage());
        }
    }

    @GetMapping("/availableShots2")
    public Object availableShots2(@RequestParam String playerId) {
        try {
            ServicioPartida.AvailableShotsResponse r = servicioPartida.obtenerDisparosDisponibles(playerId);
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("ok", r.ok);
            out.put("estado", r.estado);
            out.put("celdas", r.celdas);
            return out;
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage());
        }
    }

    @PostMapping("/selectDrone2")
    public Object selectDrone2(@RequestParam String playerId, @RequestParam String dronId) {
        try {
            ServicioPartida.TemplateResponse r = servicioPartida.selectDrone(playerId, dronId);
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("ok", r.ok);
            out.put("estado", r.estado);
            out.put("dronId", dronId); // información de debug
            return out;
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage());
        }
    }

    @PostMapping("/shoot2")
    public Object shoot2(@RequestParam String playerId,
                         @RequestParam int fila,
                         @RequestParam int col) {
        try {
            ServicioPartida.ShootResponse r = servicioPartida.shoot2(playerId, fila, col);
            java.util.Map<String, Object> out = new java.util.HashMap<>();
            out.put("ok", r.ok);
            out.put("estado", r.estado);
            out.put("resultado", r.resultado);
            out.put("objetivo", r.objetivo);
            out.put("municionRestante", r.municionRestante);
            if (r.tipoProyectilDisparo != null) {
                out.put("tipoProyectilDisparo", r.tipoProyectilDisparo);
            }
            if (r.vidaObjetivo != null) {
                out.put("vidaObjetivo", r.vidaObjetivo);
            }
            if (r.impactosRestantesPorta != null) {
                out.put("impactosRestantesPorta", r.impactosRestantesPorta);
            }
            return out;
        } catch (Exception e) {
            return java.util.Map.of("error", e.getMessage());
        }
    }

    private GameStateDTO toDTO(EstadoJuego estado) {
        GameStateDTO dto = new GameStateDTO();
        dto.dronX = estado.getDron().getPosicion().getX();
        dto.dronY = estado.getDron().getPosicion().getY();
        dto.municion = estado.getDron().getMunicion();
        dto.equipo = estado.getDron().getEquipo().name();

        if (estado.getUltimoDisparo() != null) {
            dto.ultimoDisparo = new ShotDTO(
                    estado.getUltimoDisparo().name(),
                    dto.dronX,
                    dto.dronY
            );
        }
        dto.numeroTurno = estado.getNumeroTurno();
        dto.turnoDe = estado.getTurnoDe().name();

        return dto;
    }
}
