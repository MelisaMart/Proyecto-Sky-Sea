package com.skysea.server.presentacion.dto;

public class ShotEventDTO {
    public long id;                 // consecutivo para no repetir
    public String weapon;           // "MISIL" o "BOMBA" (o "missile"/"bomb" si preferís)
    public String attacker;         // "NAVAL" o "AEREO" (equipo atacante)
    public String target;           // "DRON" o "PORTA"
    public String result;           // "HIT" o "MISS"
    public Integer vidaObjetivo;    // para DRON (0 si muere)
    public Integer impactosRestantesPorta; // para PORTA (puede ser null)
}