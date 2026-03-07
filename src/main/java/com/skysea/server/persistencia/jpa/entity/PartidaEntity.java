package com.skysea.server.persistencia.jpa.entity;

import com.skysea.server.logica.model.Equipo;
import com.skysea.server.logica.model.EstadoPartida;
import com.skysea.server.logica.model.MotivoFinPartida;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "partida")
public class PartidaEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false, length = 30)
    private EstadoPartida estado;

    @Enumerated(EnumType.STRING)
    @Column(name = "turno_equipo", nullable = false, length = 20)
    private Equipo turnoEquipo;

    @Column(name = "turno_numero", nullable = false)
    private int turnoNumero;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "ganador_equipo", length = 20)
    private Equipo ganadorEquipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_fin", length = 30)
    private MotivoFinPartida motivoFin;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "primer_jugador_id", length = 64)
    private String primerJugadorId;

    @Column(name = "turno_inicio_epoch_ms", nullable = false)
    private long turnoInicioEpochMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "ganador", length = 20)
    private Equipo ganador;

    @Column(name = "activa", nullable = false)
    private boolean activa;

    @Column(name = "isReanudable")
    private Boolean isReanudable;

    @OneToMany(mappedBy = "partida", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<JugadorEntity> jugadores = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public EstadoPartida getEstado() {
        return estado;
    }

    public void setEstado(EstadoPartida estado) {
        this.estado = estado;
    }

    public Equipo getTurnoEquipo() {
        return turnoEquipo;
    }

    public void setTurnoEquipo(Equipo turnoEquipo) {
        this.turnoEquipo = turnoEquipo;
    }

    public int getTurnoNumero() {
        return turnoNumero;
    }

    public void setTurnoNumero(int turnoNumero) {
        this.turnoNumero = turnoNumero;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public Equipo getGanadorEquipo() {
        return ganadorEquipo;
    }

    public void setGanadorEquipo(Equipo ganadorEquipo) {
        this.ganadorEquipo = ganadorEquipo;
    }

    public MotivoFinPartida getMotivoFin() {
        return motivoFin;
    }

    public void setMotivoFin(MotivoFinPartida motivoFin) {
        this.motivoFin = motivoFin;
    }

    public String getPrimerJugadorId() {
        return primerJugadorId;
    }

    public void setPrimerJugadorId(String primerJugadorId) {
        this.primerJugadorId = primerJugadorId;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public long getTurnoInicioEpochMs() {
        return turnoInicioEpochMs;
    }

    public void setTurnoInicioEpochMs(long turnoInicioEpochMs) {
        this.turnoInicioEpochMs = turnoInicioEpochMs;
    }

    public Equipo getGanador() {
        return ganador;
    }

    public void setGanador(Equipo ganador) {
        this.ganador = ganador;
    }

    public boolean isActiva() {
        return activa;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    public List<JugadorEntity> getJugadores() {
        return jugadores;
    }

    public void setJugadores(List<JugadorEntity> jugadores) {
        this.jugadores = jugadores;
    }

    public Boolean getIsReanudable() {
        return isReanudable;
    }

    public void setIsReanudable(Boolean isReanudable) {
        this.isReanudable = isReanudable;
    }
}
