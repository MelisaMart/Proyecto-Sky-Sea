package com.skysea.server.presentacion.dto;

public class ShotDTO {
    public String tipo; // "BOMBA" o "MISIL"
    public int origenX;
    public int origenY;

    public ShotDTO() {}
    public ShotDTO(String tipo, int origenX, int origenY) {
        this.tipo = tipo;
        this.origenX = origenX;
        this.origenY = origenY;
    }
}
