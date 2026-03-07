package com.skysea.server.persistencia.jpa.mapper;

import com.skysea.server.logica.model.CatalogoPlantillasDespliegue;
import com.skysea.server.logica.model.Dron;
import com.skysea.server.logica.model.Equipo;
import com.skysea.server.logica.model.EstadoPartida;
import com.skysea.server.logica.model.Jugador;
import com.skysea.server.logica.model.Partida;
import com.skysea.server.logica.model.Porta;
import com.skysea.server.logica.model.Posicion;
import com.skysea.server.logica.model.TipoPlantillaDespliegue;
import com.skysea.server.persistencia.jpa.entity.DronEntity;
import com.skysea.server.persistencia.jpa.entity.JugadorEntity;
import com.skysea.server.persistencia.jpa.entity.PartidaEntity;
import com.skysea.server.persistencia.jpa.entity.PortaEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class PartidaJpaMapper {

    public Partida toDomain(PartidaEntity entity) {
        Partida partida = new Partida(entity.getId());
        partida.setEstado(entity.getEstado() != null ? entity.getEstado() : EstadoPartida.ESPERANDO_RIVAL);
        partida.setNumeroTurno(entity.getTurnoNumero());
        partida.setTurnoDe(entity.getTurnoEquipo() != null ? entity.getTurnoEquipo() : Equipo.NAVAL);
        partida.setTurnoInicioEpochMs(entity.getTurnoInicioEpochMs());

        if (entity.getJugadores() != null) {
            for (JugadorEntity jugadorEntity : entity.getJugadores()) {
                Jugador jugador = toDomainJugador(jugadorEntity);
                if (jugadorEntity.getSlot() == 1) {
                    partida.setJugador1(jugador);
                } else if (jugadorEntity.getSlot() == 2) {
                    partida.setJugador2(jugador);
                }
            }
        }

        if (entity.getPrimerJugadorId() != null && !entity.getPrimerJugadorId().isBlank()) {
            partida.setPrimerJugadorId(entity.getPrimerJugadorId());
        }

        partida.setGanador(entity.getGanador() != null ? entity.getGanador() : entity.getGanadorEquipo());
        partida.setMotivoFin(entity.getMotivoFin());
        partida.setReanudable(entity.getIsReanudable() == null || entity.getIsReanudable());
        return partida;
    }

    public PartidaEntity toEntity(Partida domain) {
        PartidaEntity entity = new PartidaEntity();
        entity.setId(domain.getIdPartida());
        entity.setEstado(domain.getEstado());
        entity.setTurnoNumero(domain.getNumeroTurno());
        entity.setTurnoEquipo(domain.getTurnoDe());
        entity.setTurnoInicioEpochMs(domain.getTurnoInicioEpochMs());
        entity.setPrimerJugadorId(domain.getPrimerJugadorId());
        entity.setGanador(domain.getGanador());
        entity.setGanadorEquipo(domain.getGanador());
        entity.setMotivoFin(domain.getMotivoFin());
        entity.setIsReanudable(domain.isReanudable());
        // Persistimos siempre como activa; la rotacion de partida la manejan reset/join.
        entity.setActiva(true);

        List<JugadorEntity> jugadores = new ArrayList<>();
        JugadorEntity j1 = toEntityJugador(domain.getJugador1(), entity, 1);
        if (j1 != null) {
            jugadores.add(j1);
        }
        JugadorEntity j2 = toEntityJugador(domain.getJugador2(), entity, 2);
        if (j2 != null) {
            jugadores.add(j2);
        }
        entity.setJugadores(jugadores);
        return entity;
    }

    private Jugador toDomainJugador(JugadorEntity entity) {
        boolean conectado = entity.getEstado() == null || !"DISCONNECTED".equalsIgnoreCase(entity.getEstado());
        Jugador jugador = new Jugador(entity.getId(), entity.getNombre(), entity.getEquipo(), conectado);
        jugador.setPlantillaSeleccionada(entity.getPlantilla());
        jugador.setDronSeleccionado(entity.getDronSeleccionadoId());
        jugador.setAccionTurno(entity.getAccionTurno() != null ? entity.getAccionTurno() : Jugador.AccionTurno.NINGUNA);

        Jugador.AccionTurno accion = jugador.getAccionTurno();
        boolean movio = accion == Jugador.AccionTurno.MOVIO || accion == Jugador.AccionTurno.MOVIO_Y_DISPARO;
        boolean disparo = accion == Jugador.AccionTurno.DISPARO || accion == Jugador.AccionTurno.MOVIO_Y_DISPARO;
        jugador.setMovioEsteTurno(movio);
        jugador.setDisparoEsteTurno(disparo);

        if (entity.getPorta() != null) {
            jugador.asignarPorta(toDomainPorta(entity.getPorta(), entity.getPlantilla()));
        }

        List<Dron> drones = new ArrayList<>();
        if (entity.getDrones() != null) {
            for (DronEntity dronEntity : entity.getDrones()) {
                drones.add(toDomainDron(dronEntity));
            }
        }
        jugador.setDrones(drones);
        return jugador;
    }

    private JugadorEntity toEntityJugador(Jugador domain, PartidaEntity partida, int slot) {
        if (domain == null) {
            return null;
        }
        JugadorEntity entity = new JugadorEntity();
        entity.setId(domain.getId());
        entity.setPartida(partida);
        entity.setSlot(slot);
        entity.setNombre(domain.getNombre());
        entity.setEquipo(domain.getEquipo());
        entity.setEstado(domain.isConectado() ? "CONNECTED" : "DISCONNECTED");
        entity.setPlantilla(domain.getPlantillaSeleccionada());
        entity.setDronSeleccionadoId(domain.getDronSeleccionado());
        entity.setAccionTurno(domain.getAccionTurno());
        entity.setPorta(toEntityPorta(domain.getPorta(), entity));

        List<DronEntity> drones = new ArrayList<>();
        for (Dron dron : domain.getDrones()) {
            DronEntity dronEntity = toEntityDron(dron);
            dronEntity.setJugador(entity);
            drones.add(dronEntity);
        }
        entity.setDrones(drones);

        return entity;
    }

    private Porta toDomainPorta(PortaEntity entity, String plantilla) {
        List<Posicion> celdas = new ArrayList<>();

        if (plantilla != null && !plantilla.isBlank()) {
            try {
                TipoPlantillaDespliegue tipo = TipoPlantillaDespliegue.fromNombre(plantilla);
                celdas.addAll(CatalogoPlantillasDespliegue.obtener(tipo).getPortaCells());
            } catch (Exception ignored) {
                // Si la plantilla no existe en catalogo, se usa fallback simple.
            }
        }

        if (celdas.isEmpty()) {
            celdas.add(new Posicion(entity.getPosX(), entity.getPosY()));
        }

        Porta porta = new Porta(entity.getId(), entity.getEquipo(), celdas, entity.getImpactosRestantes());
        porta.setPosicion(new Posicion(entity.getPosX(), entity.getPosY()));
        return porta;
    }

    private PortaEntity toEntityPorta(Porta domain, JugadorEntity jugador) {
        if (domain == null) {
            return null;
        }
        PortaEntity entity = new PortaEntity();
        entity.setId(domain.getId());
        entity.setEquipo(domain.getEquipo());
        entity.setPosX(domain.getPosicion().getX());
        entity.setPosY(domain.getPosicion().getY());
        entity.setImpactosRestantes(domain.getImpactosRestantes());
        entity.setVivo(!domain.estaDestruido());
        entity.setJugador(jugador);
        return entity;
    }

    private Dron toDomainDron(DronEntity entity) {
        return new Dron(
                entity.getId(),
                entity.getEquipo(),
                new Posicion(entity.getPosX(), entity.getPosY()),
                entity.getVida(),
                entity.getMunicion(),
                entity.isMovioEsteTurno(),
                entity.isDisparoEsteTurno()
        );
    }

    private DronEntity toEntityDron(Dron domain) {
        DronEntity entity = new DronEntity();
        entity.setId(domain.getId());
        entity.setEquipo(domain.getEquipo());
        entity.setPosX(domain.getPosicion().getX());
        entity.setPosY(domain.getPosicion().getY());
        entity.setAltura(0);
        entity.setVivo(domain.estaVivo());
        entity.setVida(domain.getVida());
        entity.setMunicion(domain.getMunicion());
        entity.setDestruido(!domain.estaVivo());
        entity.setMovioEsteTurno(domain.isMovioEsteTurno());
        entity.setDisparoEsteTurno(domain.isDisparoEsteTurno());
        return entity;
    }
}
