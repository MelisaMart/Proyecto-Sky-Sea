package com.skysea.server.presentacion.dto;

public class JoinResponseDTO {
    public String idPartida;
    public String playerId;
    public String nombre;
    public String estadoPartida; // "ESPERANDO_RIVAL", "EN_JUEGO", "OCUPADO"
    public String equipo;        // "NAVAL" o "AEREO" (puede ser null si ocupado)
    public int numeroJugador;    // 1 = primer ingreso, 2 = segundo ingreso

    public JoinResponseDTO() {}

    public JoinResponseDTO(String idPartida, String playerId, String nombre, String estadoPartida, String equipo, int numeroJugador) {
        this.idPartida = idPartida;
        this.playerId = playerId;
        this.nombre = nombre;
        this.estadoPartida = estadoPartida;
        this.equipo = equipo;
        this.numeroJugador = numeroJugador;
    }
}

