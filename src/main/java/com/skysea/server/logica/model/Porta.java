package com.skysea.server.logica.model;

import java.util.Objects;
import java.util.UUID;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Porta {

    private final String id;
    private final Equipo equipo;

    private Posicion posicion;
    private final List<Posicion> celdasOcupadas;
    private int impactosRestantes;

    public Porta(Equipo equipo, Posicion posicion) {
        this(equipo, List.of(posicion));
    }

    public Porta(Equipo equipo, List<Posicion> celdasOcupadas) {
        this.id = UUID.randomUUID().toString();
        this.equipo = Objects.requireNonNull(equipo);
        Objects.requireNonNull(celdasOcupadas);
        if (celdasOcupadas.isEmpty()) {
            throw new IllegalArgumentException("PORTA_SIN_CELDAS");
        }

        this.celdasOcupadas = new ArrayList<>();
        for (Posicion p : celdasOcupadas) {
            this.celdasOcupadas.add(new Posicion(p.getX(), p.getY()));
        }

        this.posicion = this.celdasOcupadas.get(0);
        this.impactosRestantes = (equipo == Equipo.AEREO)
                ? Reglas.IMPACTOS_PORTA_AEREO
                : Reglas.IMPACTOS_PORTA_NAVAL;
    }

    public String getId() { return id; }
    public Equipo getEquipo() { return equipo; }

    public Posicion getPosicion() { return posicion; }
    public void setPosicion(Posicion posicion) { this.posicion = Objects.requireNonNull(posicion); }
    public List<Posicion> getCeldasOcupadas() { return Collections.unmodifiableList(celdasOcupadas); }
    public boolean ocupa(Posicion posicion) { return celdasOcupadas.contains(posicion); }
    public boolean esFijo() { return true; }

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
