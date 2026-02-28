package com.skysea.server.logica.model;

import java.util.Objects;
import java.util.UUID;

public class Partida {

    public static final int DURACION_TURNO_SEGUNDOS = 30;

    private final String idPartida;
    private EstadoPartida estado;

    private Jugador jugador1;
    private Jugador jugador2;
    private String primerJugadorId;

    private int numeroTurno;
    private Equipo turnoDe;
    private long turnoInicioEpochMs;

    public Partida() {
        this.idPartida = UUID.randomUUID().toString();
        this.estado = EstadoPartida.ESPERANDO_RIVAL;
        this.numeroTurno = 1;
        this.turnoDe = Equipo.NAVAL; // regla del doc: naval empieza
        this.turnoInicioEpochMs = System.currentTimeMillis();
    }

    // ----------------- Helpers de cupos -----------------

    public boolean estaLlena() {
        return jugador1 != null && jugador2 != null;
    }

    public boolean tieneUnSoloJugador() {
        return (jugador1 != null) ^ (jugador2 != null);
    }

    public Jugador getJugadorLibreSlot() {
        if (jugador1 == null) return null;
        if (jugador2 == null) return null;
        return null;
    }

    // ----------------- Identidad / Búsqueda -----------------

    public boolean pertenece(String playerId) {
        return buscarJugadorPorId(playerId) != null;
    }

    public Jugador buscarJugadorPorId(String playerId) {
        if (playerId == null) return null;
        if (jugador1 != null && playerId.equals(jugador1.getId())) return jugador1;
        if (jugador2 != null && playerId.equals(jugador2.getId())) return jugador2;
        return null;
    }

    public Jugador buscarJugadorPorNombre(String nombre) {
        if (nombre == null) return null;
        String n = nombre.trim();
        if (jugador1 != null && n.equalsIgnoreCase(jugador1.getNombre())) return jugador1;
        if (jugador2 != null && n.equalsIgnoreCase(jugador2.getNombre())) return jugador2;
        return null;
    }

    public int numeroJugadorPorId(String playerId) {
        if (playerId == null) return 0;
        if (primerJugadorId == null) return 0;
        Jugador jugador = buscarJugadorPorId(playerId);
        if (jugador == null) return 0;
        return primerJugadorId.equals(playerId) ? 1 : 2;
    }

    public boolean esTurnoDe(String playerId) {
        Jugador j = buscarJugadorPorId(playerId);
        return j != null && j.getEquipo() == this.turnoDe;
    }

    // ----------------- Setters con validación mínima -----------------

    public void setJugador1(Jugador jugador1) {
        this.jugador1 = Objects.requireNonNull(jugador1);
    }

    public void setJugador2(Jugador jugador2) {
        this.jugador2 = Objects.requireNonNull(jugador2);
    }

    public void clearJugador1() {
        this.jugador1 = null;
    }

    public void clearJugador2() {
        this.jugador2 = null;
    }

    public void setPrimerJugadorId(String primerJugadorId) {
        this.primerJugadorId = Objects.requireNonNull(primerJugadorId);
    }

    public String getPrimerJugadorId() {
        return primerJugadorId;
    }

    // ----------------- Getters/Setters -----------------

    public String getIdPartida() {
        return idPartida;
    }

    public EstadoPartida getEstado() {
        return estado;
    }

    public void setEstado(EstadoPartida estado) {
        this.estado = Objects.requireNonNull(estado);
    }

    public Jugador getJugador1() {
        return jugador1;
    }

    public Jugador getJugador2() {
        return jugador2;
    }

    public int getNumeroTurno() {
        return numeroTurno;
    }

    public void setNumeroTurno(int numeroTurno) {
        this.numeroTurno = numeroTurno;
    }

    public Equipo getTurnoDe() {
        return turnoDe;
    }

    public void setTurnoDe(Equipo turnoDe) {
        this.turnoDe = Objects.requireNonNull(turnoDe);
    }

    public long getTurnoInicioEpochMs() {
        return turnoInicioEpochMs;
    }

    public void setTurnoInicioEpochMs(long turnoInicioEpochMs) {
        this.turnoInicioEpochMs = turnoInicioEpochMs;
    }

    public void reiniciarTemporizadorTurno() {
        this.turnoInicioEpochMs = System.currentTimeMillis();
    }

    public int segundosRestantesTurno(long ahoraEpochMs) {
        long transcurridosMs = Math.max(0L, ahoraEpochMs - this.turnoInicioEpochMs);
        long restantesMs = (long) DURACION_TURNO_SEGUNDOS * 1000L - transcurridosMs;
        if (restantesMs <= 0L) {
            return 0;
        }
        return (int) ((restantesMs + 999L) / 1000L);
    }
}
