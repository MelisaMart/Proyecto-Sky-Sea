package com.skysea.server.logica.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CatalogoPlantillasDespliegue {

    private static final Map<TipoPlantillaDespliegue, PlantillaDespliegue> CATALOGO = new EnumMap<>(TipoPlantillaDespliegue.class);

    static {
        CATALOGO.put(TipoPlantillaDespliegue.AEREO_1, new PlantillaDespliegue(
                "Aereo1",
                Equipo.AEREO,
                posiciones(new int[][]{
                        {0,0},{1,0},{2,0},{3,0},{4,0},{0,1},{1,1},{2,1},{3,1},{4,1}
                }),
                posiciones(new int[][]{
                        {1,3},{1,5},{1,7},{1,9},{2,4},{2,6},{2,8},{3,5},{3,7},{5,0},{5,1},{4,6}
                })
        ));

        CATALOGO.put(TipoPlantillaDespliegue.AEREO_2, new PlantillaDespliegue(
                "Aereo2",
                Equipo.AEREO,
                posiciones(new int[][]{
                        {1,8},{2,8},{3,8},{4,8},{5,8},{1,9},{2,9},{3,9},{4,9},{5,9}
                }),
                posiciones(new int[][]{
                        {1,0},{3,0},{5,0},{1,2},{3,2},{5,2},{1,4},{3,4},{5,4},{1,6},{3,6},{5,6}
                })
        ));

        CATALOGO.put(TipoPlantillaDespliegue.AEREO_3, new PlantillaDespliegue(
                "Aereo3",
                Equipo.AEREO,
                posiciones(new int[][]{
                        {0,2},{1,2},{0,3},{1,3},{0,4},{1,4},{0,5},{1,5},{0,6},{1,6}
                }),
                posiciones(new int[][]{
                        {3,0},{2,1},{5,1},{3,2},{2,3},{3,4},{2,5},{3,6},{2,7},{3,8},{5,4},{5,7}
                })
        ));

        CATALOGO.put(TipoPlantillaDespliegue.NAVAL_1, new PlantillaDespliegue(
                "Naval1",
                Equipo.NAVAL,
                posiciones(new int[][]{
                        {13,0},{14,0},{15,0},{16,0},{17,0},{18,0},{19,0}
                }),
                posiciones(new int[][]{
                        {11,2},{11,3},{11,5},{12,0},{14,2},{17,2}
                })
        ));

        CATALOGO.put(TipoPlantillaDespliegue.NAVAL_2, new PlantillaDespliegue(
                "Naval2",
                Equipo.NAVAL,
                posiciones(new int[][]{
                        {19,2},{19,3},{19,4},{19,5},{19,6},{19,7},{19,8}
                }),
                posiciones(new int[][]{
                        {10,2},{10,6},{11,4},{11,8},{17,4},{17,7}
                })
        ));

        CATALOGO.put(TipoPlantillaDespliegue.NAVAL_3, new PlantillaDespliegue(
                "Naval3",
                Equipo.NAVAL,
                posiciones(new int[][]{
                        {13,8},{14,8},{15,8},{16,8},{17,8},{18,8},{19,8}
                }),
                posiciones(new int[][]{
                        {11,0},{11,3},{11,6},{11,9},{13,3},{13,6}
                })
        ));
    }

    private CatalogoPlantillasDespliegue() {
    }

    public static PlantillaDespliegue obtener(TipoPlantillaDespliegue tipo) {
        PlantillaDespliegue plantilla = CATALOGO.get(tipo);
        if (plantilla == null) {
            throw new IllegalArgumentException("PLANTILLA_NO_EXISTE");
        }
        return plantilla;
    }

    public static List<PlantillaDespliegue> listarPorEquipo(Equipo equipo) {
        return CATALOGO.values().stream()
                .filter(p -> p.getEquipo() == equipo)
                .toList();
    }

    private static List<Posicion> posiciones(int[][] coords) {
        return java.util.Arrays.stream(coords)
                .map(c -> new Posicion(c[0], c[1]))
                .toList();
    }
}
