package com.skysea.server.logica.model;

public final class Reglas {
    private Reglas() {}

    // Tablero
    public static final int TABLERO_ANCHO = 20;
    public static final int TABLERO_ALTO = 10;
    public static final int TABLERO_X_MIN = 0;
    public static final int TABLERO_Y_MIN = 0;
    public static final int TABLERO_X_MAX = TABLERO_ANCHO - 1;
    public static final int TABLERO_Y_MAX = TABLERO_ALTO - 1;
    public static final int AEREO_X_MIN = 0;
    public static final int AEREO_X_MAX = 9;
    public static final int NAVAL_X_MIN = 10;
    public static final int NAVAL_X_MAX = 19;

    // Cantidades obligatorias
    public static final int DRONES_AEREO = 12; // bomba
    public static final int DRONES_NAVAL = 6;  // misil
    public static final int PORTA_CELDAS_AEREO = 10;
    public static final int PORTA_CELDAS_NAVAL = 7;

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
    public static final int RANGO_ATAQUE_BOMBA = 1;
    public static final int RANGO_ATAQUE_MISIL = VISION_MISIL;

    // Vida por tipo de dron
    public static final int VIDA_DRON_AEREO = 1;
    public static final int VIDA_DRON_NAVAL = 2;
}
