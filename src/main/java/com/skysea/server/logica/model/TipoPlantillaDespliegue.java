package com.skysea.server.logica.model;

public enum TipoPlantillaDespliegue {
    AEREO_1(Equipo.AEREO),
    AEREO_2(Equipo.AEREO),
    AEREO_3(Equipo.AEREO),
    NAVAL_1(Equipo.NAVAL),
    NAVAL_2(Equipo.NAVAL),
    NAVAL_3(Equipo.NAVAL);

    private final Equipo equipo;

    TipoPlantillaDespliegue(Equipo equipo) {
        this.equipo = equipo;
    }

    public Equipo getEquipo() {
        return equipo;
    }

    public static TipoPlantillaDespliegue fromNombre(String nombre) {
        if (nombre == null) {
            throw new IllegalArgumentException("PLANTILLA_INVALIDA");
        }

        String normalizado = nombre.trim().toUpperCase()
                .replace("Á", "A")
                .replace("É", "E")
                .replace("Í", "I")
                .replace("Ó", "O")
                .replace("Ú", "U")
                .replace(" ", "")
                .replace("_", "");

        return switch (normalizado) {
            case "AEREO1" -> AEREO_1;
            case "AEREO2" -> AEREO_2;
            case "AEREO3" -> AEREO_3;
            case "NAVAL1" -> NAVAL_1;
            case "NAVAL2" -> NAVAL_2;
            case "NAVAL3" -> NAVAL_3;
            default -> throw new IllegalArgumentException("PLANTILLA_INVALIDA");
        };
    }
}
