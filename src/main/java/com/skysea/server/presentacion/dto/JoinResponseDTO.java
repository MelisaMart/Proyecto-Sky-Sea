package com.skysea.server.presentacion.dto;

public class JoinResponseDTO {
    public String idPartida;
    public String playerId;
    public String nombre;
    public String estadoPartida; // "ESPERANDO_RIVAL", "EN_JUEGO", "OCUPADO"
    public String equipo;        // "NAVAL" o "AEREO" (puede ser null si ocupado)

    public JoinResponseDTO() {}

    public JoinResponseDTO(String idPartida, String playerId, String nombre, String estadoPartida, String equipo) {
        this.idPartida = idPartida;
        this.playerId = playerId;
        this.nombre = nombre;
        this.estadoPartida = estadoPartida;
        this.equipo = equipo;
    }
}

