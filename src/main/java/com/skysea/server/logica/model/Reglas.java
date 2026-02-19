package com.skysea.server.logica.model;

public final class Reglas {
    private Reglas() {}

    // Tablero (si ya lo definieron distinto, cambialo acá)
    public static final int TABLERO_ANCHO = 25;
    public static final int TABLERO_ALTO = 50;

    // Cantidades obligatorias
    public static final int DRONES_AEREO = 12; // bomba
    public static final int DRONES_NAVAL = 6;  // misil

    // Munición obligatoria por dron
    public static final int MUNICION_BOMBA = 1;
    public static final int MUNICION_MISIL = 2;

    // Porta: impactos necesarios
    public static final int IMPACTOS_PORTA_AEREO = 6; // misiles
    public static final int IMPACTOS_PORTA_NAVAL = 3; // bombas

    // Movimiento (misma cantidad de casillas para ambos)
    public static final int RANGO_MOVIMIENTO_DRON = 3; // A VERIFICAR

    // Visión: bomba ve el doble que misil
    public static final int VISION_MISIL = 3;
    public static final int VISION_BOMBA = VISION_MISIL * 2;

    // Vida base de dron (la destrucción especial del misil a dron bomba está aparte)
    public static final int VIDA_DRON = 1;
}
