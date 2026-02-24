package com.skysea.server.logica.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PlantillaDespliegue {

    private final String nombre;
    private final Equipo equipo;
    private final List<Posicion> portaCells;
    private final List<Posicion> drones;

    public PlantillaDespliegue(String nombre, Equipo equipo, List<Posicion> portaCells, List<Posicion> drones) {
        this.nombre = Objects.requireNonNull(nombre).trim();
        this.equipo = Objects.requireNonNull(equipo);
        this.portaCells = copiarPosiciones(portaCells);
        this.drones = copiarPosiciones(drones);
        validar();
    }

    public String getNombre() {
        return nombre;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    public List<Posicion> getPortaCells() {
        return Collections.unmodifiableList(portaCells);
    }

    public List<Posicion> getDrones() {
        return Collections.unmodifiableList(drones);
    }

    private static List<Posicion> copiarPosiciones(List<Posicion> posiciones) {
        Objects.requireNonNull(posiciones);
        List<Posicion> copia = new ArrayList<>(posiciones.size());
        for (Posicion p : posiciones) {
            Objects.requireNonNull(p);
            copia.add(new Posicion(p.getX(), p.getY()));
        }
        return copia;
    }

    private void validar() {
        if (nombre.isBlank()) {
            throw new IllegalArgumentException("PLANTILLA_SIN_NOMBRE");
        }
        if (portaCells.isEmpty()) {
            throw new IllegalArgumentException("PLANTILLA_SIN_PORTA");
        }
        if (drones.isEmpty()) {
            throw new IllegalArgumentException("PLANTILLA_SIN_DRONES");
        }

        int portaEsperado = (equipo == Equipo.AEREO) ? Reglas.PORTA_CELDAS_AEREO : Reglas.PORTA_CELDAS_NAVAL;
        int dronesEsperados = (equipo == Equipo.AEREO) ? Reglas.DRONES_AEREO : Reglas.DRONES_NAVAL;

        if (portaCells.size() != portaEsperado) {
            throw new IllegalArgumentException("PORTA_CELDAS_INVALIDAS_" + nombre);
        }
        if (drones.size() != dronesEsperados) {
            throw new IllegalArgumentException("DRONES_CANTIDAD_INVALIDA_" + nombre);
        }

        Set<Posicion> usadas = new HashSet<>();

        for (Posicion p : portaCells) {
            validarCeldaTablero(p);
            validarMitadEquipo(p);
            if (!usadas.add(p)) {
                throw new IllegalArgumentException("CELDA_REPETIDA_" + nombre);
            }
        }

        for (Posicion p : drones) {
            validarCeldaTablero(p);
            validarMitadEquipo(p);
            if (!usadas.add(p)) {
                throw new IllegalArgumentException("SOLAPAMIENTO_DRON_PORTA_" + nombre);
            }
        }
    }

    private void validarCeldaTablero(Posicion p) {
        boolean dentro = p.getX() >= Reglas.TABLERO_X_MIN && p.getX() <= Reglas.TABLERO_X_MAX
                && p.getY() >= Reglas.TABLERO_Y_MIN && p.getY() <= Reglas.TABLERO_Y_MAX;
        if (!dentro) {
            throw new IllegalArgumentException("POSICION_FUERA_DE_TABLERO_" + nombre);
        }
    }

    private void validarMitadEquipo(Posicion p) {
        if (equipo == Equipo.AEREO) {
            if (p.getX() < Reglas.AEREO_X_MIN || p.getX() > Reglas.AEREO_X_MAX) {
                throw new IllegalArgumentException("PLANTILLA_AEREO_FUERA_MITAD_" + nombre);
            }
            return;
        }

        if (p.getX() < Reglas.NAVAL_X_MIN || p.getX() > Reglas.NAVAL_X_MAX) {
            throw new IllegalArgumentException("PLANTILLA_NAVAL_FUERA_MITAD_" + nombre);
        }
    }
}
