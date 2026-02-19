package com.skysea.server.presentacion.controlador;

import com.skysea.server.logica.model.EstadoJuego;
import com.skysea.server.logica.Servicio.ServicioJuego;
import com.skysea.server.persistencia.memory.InMemoryGameStateDAO;
import com.skysea.server.presentacion.dto.GameStateDTO;
import com.skysea.server.presentacion.dto.ShotDTO;
import org.springframework.web.bind.annotation.*;
import com.skysea.server.logica.Servicio.ServicioPartida;
import com.skysea.server.persistencia.memory.InMemoryPartidaDAO;
import com.skysea.server.presentacion.dto.JoinResponseDTO;


@RestController
@RequestMapping("/api/game")
@CrossOrigin // para que el front web pueda consumirlo
public class GameController {

    private final ServicioJuego servicio;
    private final ServicioPartida servicioPartida;

    public GameController() {
        // MVP: cableado manual en memoria (después lo pasamos a inyección con @Bean/@Service)
        this.servicio = new ServicioJuego(new InMemoryGameStateDAO());
        this.servicioPartida = new ServicioPartida(new InMemoryPartidaDAO());
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

    @PostMapping("/move")
    public GameStateDTO move(@RequestParam int dx, @RequestParam int dy) {
        return toDTO(servicio.mover(dx, dy));
    }

    @PostMapping("/endTurn")
    public GameStateDTO endTurn() {
        return toDTO(servicio.terminarTurno());
    }

    @PostMapping("/join")
    public JoinResponseDTO join(@RequestParam String nombre) {
        ServicioPartida.JoinResponse r = servicioPartida.join(nombre);
        return new JoinResponseDTO(r.idPartida, r.playerId, r.nombre, r.estadoPartida, r.equipo);
    }

    @GetMapping("/state2")
    public Object state2(@RequestParam String playerId) {
        var partida = servicioPartida.getPartidaActiva(); // lo vamos a crear ya
        var jugador = partida.buscarJugadorPorId(playerId);

        if (jugador == null) {
            return java.util.Map.of(
                    "error", "PLAYER_NO_ENCONTRADO"
            );
        }

        return java.util.Map.of(
                "idPartida", partida.getIdPartida(),
                "estadoPartida", partida.getEstado().name(),
                "turnoDe", partida.getTurnoDe().name(),
                "equipo", jugador.getEquipo().name()
        );
    }

    @PostMapping("/endTurn2")
    public Object endTurn2(@RequestParam String playerId) {
        try {
            var partida = servicioPartida.terminarTurno(playerId);
            return java.util.Map.of(
                    "idPartida", partida.getIdPartida(),
                    "estadoPartida", partida.getEstado().name(),
                    "turnoDe", partida.getTurnoDe().name(),
                    "numeroTurno", partida.getNumeroTurno()
            );
        } catch (Exception e) {
            return java.util.Map.of(
                    "error", e.getMessage()
            );
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
