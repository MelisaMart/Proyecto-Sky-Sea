package com.skysea.server.presentacion.dto;

public class GameStateDTO {
    public int dronX;
    public int dronY;
    public int municion;
    public String equipo; // "NAVAL" o "AEREO"
    public ShotDTO ultimoDisparo; // puede ser null
    public int numeroTurno;
    public String turnoDe;

    public GameStateDTO() {}
}
