package com.skysea.server.persistencia.jpa.entity;

import com.skysea.server.logica.model.Equipo;
import com.skysea.server.logica.model.Jugador;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "jugador")
public class JugadorEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @ManyToOne
    @JoinColumn(name = "partida_id", nullable = false)
    private PartidaEntity partida;

    @Column(name = "slot", nullable = false)
    private int slot;

    @Column(name = "nombre", length = 60)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "equipo", nullable = false, length = 20)
    private Equipo equipo;

    @Column(name = "token_conexion", length = 80)
    private String tokenConexion;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "plantilla", length = 50)
    private String plantilla;

    @Column(name = "dron_seleccionado_id", length = 36)
    private String dronSeleccionadoId;

    @Enumerated(EnumType.STRING)
    @Column(name = "accion_turno", length = 20)
    private Jugador.AccionTurno accionTurno;

    @OneToOne(mappedBy = "jugador", cascade = CascadeType.ALL, orphanRemoval = true)
    private PortaEntity porta;

    @OneToMany(mappedBy = "jugador", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DronEntity> drones = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public PartidaEntity getPartida() {
        return partida;
    }

    public void setPartida(PartidaEntity partida) {
        this.partida = partida;
    }

    public int getSlot() {
        return slot;
    }

    public void setSlot(int slot) {
        this.slot = slot;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    public void setEquipo(Equipo equipo) {
        this.equipo = equipo;
    }

    public String getTokenConexion() {
        return tokenConexion;
    }

    public void setTokenConexion(String tokenConexion) {
        this.tokenConexion = tokenConexion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getPlantilla() {
        return plantilla;
    }

    public void setPlantilla(String plantilla) {
        this.plantilla = plantilla;
    }

    public String getDronSeleccionadoId() {
        return dronSeleccionadoId;
    }

    public void setDronSeleccionadoId(String dronSeleccionadoId) {
        this.dronSeleccionadoId = dronSeleccionadoId;
    }

    public Jugador.AccionTurno getAccionTurno() {
        return accionTurno;
    }

    public void setAccionTurno(Jugador.AccionTurno accionTurno) {
        this.accionTurno = accionTurno;
    }

    public PortaEntity getPorta() {
        return porta;
    }

    public void setPorta(PortaEntity porta) {
        this.porta = porta;
    }

    public List<DronEntity> getDrones() {
        return drones;
    }

    public void setDrones(List<DronEntity> drones) {
        this.drones = drones;
    }
}
