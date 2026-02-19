package com.skysea.server.logica.model;

import java.util.Objects;
import java.util.UUID;

public class Porta {

    private final String id;
    private final Equipo equipo;

    private Posicion posicion;
    private int impactosRestantes;

    public Porta(Equipo equipo, Posicion posicion) {
        this.id = UUID.randomUUID().toString();
        this.equipo = Objects.requireNonNull(equipo);
        this.posicion = Objects.requireNonNull(posicion);
        this.impactosRestantes = (equipo == Equipo.AEREO)
                ? Reglas.IMPACTOS_PORTA_AEREO
                : Reglas.IMPACTOS_PORTA_NAVAL;
    }

    public String getId() { return id; }
    public Equipo getEquipo() { return equipo; }

    public Posicion getPosicion() { return posicion; }
    public void setPosicion(Posicion posicion) { this.posicion = Objects.requireNonNull(posicion); }

    public int getImpactosRestantes() { return impactosRestantes; }

    public boolean estaDestruido() { return impactosRestantes <= 0; }

    /**
     * Aplica impacto SOLO si el proyectil corresponde al tipo de porta:
     * - Porta AEREO: cuenta MISIL
     * - Porta NAVAL: cuenta BOMBA
     */
    public boolean recibirImpacto(TipoProyectil proyectil) {
        Objects.requireNonNull(proyectil);

        boolean corresponde =
                (equipo == Equipo.AEREO && proyectil == TipoProyectil.MISIL) ||
                (equipo == Equipo.NAVAL && proyectil == TipoProyectil.BOMBA);

        if (!corresponde || estaDestruido()) return false;

        impactosRestantes = Math.max(0, impactosRestantes - 1);
        return true;
    }
}
