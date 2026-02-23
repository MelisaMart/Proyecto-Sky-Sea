package com.skysea.server.logica.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FabricaDespliegue {

    private FabricaDespliegue() {
    }

    public static Porta crearPorta(Equipo equipo, TipoPlantillaDespliegue tipoPlantilla) {
        PlantillaDespliegue plantilla = obtenerPlantillaDelEquipo(equipo, tipoPlantilla);
        return new Porta(equipo, plantilla.getPortaCells());
    }

    public static List<Dron> crearDrones(Equipo equipo, TipoPlantillaDespliegue tipoPlantilla) {
        PlantillaDespliegue plantilla = obtenerPlantillaDelEquipo(equipo, tipoPlantilla);
        List<Dron> drones = new ArrayList<>();
        for (Posicion p : plantilla.getDrones()) {
            drones.add(new Dron(equipo, new Posicion(p.getX(), p.getY())));
        }
        return drones;
    }

    public static void desplegarJugador(Jugador jugador, TipoPlantillaDespliegue tipoPlantilla) {
        Objects.requireNonNull(jugador);
        if (jugador.getEquipo() == null) {
            throw new IllegalStateException("JUGADOR_SIN_EQUIPO");
        }
        PlantillaDespliegue plantilla = obtenerPlantillaDelEquipo(jugador.getEquipo(), tipoPlantilla);
        jugador.aplicarPlantilla(plantilla);
    }

    private static PlantillaDespliegue obtenerPlantillaDelEquipo(Equipo equipo, TipoPlantillaDespliegue tipoPlantilla) {
        Objects.requireNonNull(equipo);
        Objects.requireNonNull(tipoPlantilla);

        PlantillaDespliegue plantilla = CatalogoPlantillasDespliegue.obtener(tipoPlantilla);
        if (plantilla.getEquipo() != equipo) {
            throw new IllegalArgumentException("PLANTILLA_NO_CORRESPONDE_A_EQUIPO");
        }
        return plantilla;
    }
}
