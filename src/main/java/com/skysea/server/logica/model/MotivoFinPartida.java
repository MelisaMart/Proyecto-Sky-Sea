package com.skysea.server.logica.model;

/**
 * Enum que representa los motivos por los cuales una partida puede finalizar.
 * Se utiliza para informar al frontend el contexto de la victoria/derrota.
 */
public enum MotivoFinPartida {
    PORTA_DESTRUIDO,    // El porta enemigo fue destruido
    SIN_DRONES,         // El equipo enemigo se quedó sin drones vivos
    SIN_MUNICION        // El equipo enemigo se quedó sin munición total
}
