package com.skysea.server.logica.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class Jugador {

    private final String id;
    private String nombre;
    private Equipo equipo;
    private boolean conectado;
    private String plantillaSeleccionada;
    private String dronSeleccionado;  // almacena el dron elegido por el jugador

    private Porta porta;
    private final List<Dron> drones;

    public Jugador(String nombre) {
        this.id = UUID.randomUUID().toString();
        this.nombre = normalizarNombre(nombre);
        this.conectado = true;
        this.plantillaSeleccionada = null;
        this.dronSeleccionado = null;
        this.drones = new ArrayList<>();
    }

    // Para reconstrucción desde persistencia
    public Jugador(String id, String nombre, Equipo equipo, boolean conectado) {
        this.id = Objects.requireNonNull(id);
        this.nombre = normalizarNombre(nombre);
        this.equipo = equipo;
        this.conectado = conectado;
        this.plantillaSeleccionada = null;
        this.dronSeleccionado = null;
        this.drones = new ArrayList<>();
    }

    // ----------------- Lógica útil (MVP) -----------------

    public boolean portaDestruido() {
        return porta != null && porta.estaDestruido();
    }

    public boolean sinDronesVivos() {
        return drones.stream().noneMatch(Dron::estaVivo);
    }

    public int bombasDisponibles() {
        return drones.stream()
                .filter(d -> d.getEquipo() == Equipo.AEREO)
                .mapToInt(Dron::getMunicion)
                .sum();
    }

    public int misilesDisponibles() {
        return drones.stream()
                .filter(d -> d.getEquipo() == Equipo.NAVAL)
                .mapToInt(Dron::getMunicion)
                .sum();
    }

    public boolean sinMunicion() {
        return bombasDisponibles() == 0 && misilesDisponibles() == 0;
    }

    /**
     * Derrota típica:
     * - Porta destruido, o
     * - sin drones vivos, o
     * - sin munición
     */
    public boolean estaDerrotado(boolean considerarSinMunicion) {
        if (portaDestruido()) return true;
        if (sinDronesVivos()) return true;
        return considerarSinMunicion && sinMunicion();
    }

    // ----------------- Gestión de unidades -----------------

    public void asignarEquipo(Equipo equipo) {
        this.equipo = Objects.requireNonNull(equipo);
    }

    public void asignarPorta(Porta porta) {
        this.porta = Objects.requireNonNull(porta);
    }

    public void agregarDron(Dron dron) {
        Objects.requireNonNull(dron);
        this.drones.add(dron);
    }

    public void setDrones(List<Dron> nuevos) {
        this.drones.clear();
        if (nuevos != null) this.drones.addAll(nuevos);
    }

    public Dron buscarDronPorId(String dronId) {
        if (dronId == null) return null;
        return drones.stream()
                .filter(d -> dronId.equals(d.getId()))
                .findFirst()
                .orElse(null);
    }

    public void aplicarPlantilla(PlantillaDespliegue plantilla) {
        Objects.requireNonNull(plantilla);
        if (equipo == null) {
            throw new IllegalStateException("EQUIPO_NO_ASIGNADO");
        }
        if (plantilla.getEquipo() != equipo) {
            throw new IllegalArgumentException("PLANTILLA_NO_CORRESPONDE_AL_EQUIPO");
        }

        this.porta = new Porta(equipo, plantilla.getPortaCells());
        this.drones.clear();
        for (Posicion posicionDron : plantilla.getDrones()) {
            this.drones.add(new Dron(equipo, new Posicion(posicionDron.getX(), posicionDron.getY())));
        }
        this.plantillaSeleccionada = plantilla.getNombre();
    }

    // ----------------- Getters/Setters -----------------

    public String getId() { return id; }

    public String getNombre() { return nombre; }

    public void setNombre(String nombre) {
        this.nombre = normalizarNombre(nombre);
    }

    public Equipo getEquipo() { return equipo; }

    public boolean isConectado() { return conectado; }

    public void setConectado(boolean conectado) {
        this.conectado = conectado;
    }

    public Porta getPorta() { return porta; }

    public List<Dron> getDrones() { return drones; }

    public String getPlantillaSeleccionada() { return plantillaSeleccionada; }

    public void setPlantillaSeleccionada(String plantillaSeleccionada) {
        this.plantillaSeleccionada = plantillaSeleccionada;
    }

    public String getDronSeleccionado() { return dronSeleccionado; }

    public void setDronSeleccionado(String dronSeleccionado) {
        this.dronSeleccionado = dronSeleccionado;
    }

    // ----------------- Helpers -----------------

    private String normalizarNombre(String nombre) {
        if (nombre == null) return "Jugador";
        String n = nombre.trim();
        if (n.isEmpty()) return "Jugador";
        if (n.length() > 20) n = n.substring(0, 20);
        return n;
    }

    @Override
    public String toString() {
        return "Jugador{" +
                "id='" + id + '\'' +
                ", nombre='" + nombre + '\'' +
                ", equipo=" + equipo +
                ", conectado=" + conectado +
                '}';
    }
}
