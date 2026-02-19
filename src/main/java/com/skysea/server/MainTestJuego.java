package com.skysea.server;

import com.skysea.server.logica.model.EstadoJuego;
import com.skysea.server.logica.Servicio.ServicioJuego;
import com.skysea.server.persistencia.memory.InMemoryGameStateDAO;

public class MainTestJuego {

    public static void main(String[] args) {

        ServicioJuego servicio = new ServicioJuego(new InMemoryGameStateDAO());

        System.out.println("=== ESTADO INICIAL ===");
        imprimirEstado(servicio.getEstado());

        System.out.println("\n=== DISPARANDO ===");
        servicio.disparar();
        imprimirEstado(servicio.getEstado());

        System.out.println("\n=== MOVIENDO ===");
        servicio.mover(1, 0);
        imprimirEstado(servicio.getEstado());

        System.out.println("\n=== RESET ===");
        servicio.reset();
        imprimirEstado(servicio.getEstado());
    }

    private static void imprimirEstado(EstadoJuego estado) {
        System.out.println("Posición: (" +
                estado.getDron().getPosicion().getX() + ", " +
                estado.getDron().getPosicion().getY() + ")");

        System.out.println("Munición: " + estado.getDron().getMunicion());

        if (estado.getUltimoDisparo() != null) {
            System.out.println("Último disparo: " + estado.getUltimoDisparo());
        } else {
            System.out.println("Último disparo: ninguno");
        }
    }
}
