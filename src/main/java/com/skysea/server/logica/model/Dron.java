package com.skysea.server.logica.model;

import java.util.Objects;
import java.util.UUID;

public class Dron {

    private final String id;
    private final Equipo equipo;

    private Posicion posicion;

    private int vida;
    private int municion;
    private boolean destruido;

    private final int rangoMovimiento;
    private final int rangoVision;
    private final int rangoAtaque;

    private boolean movioEsteTurno;
    private boolean disparoEsteTurno;

    public Dron(Equipo equipo, Posicion posicion) {
        this.id = UUID.randomUUID().toString();
        this.equipo = Objects.requireNonNull(equipo);
        this.posicion = Objects.requireNonNull(posicion);
        this.destruido = false;

        this.rangoMovimiento = Reglas.RANGO_MOVIMIENTO_DRON;

        if (equipo == Equipo.AEREO) {
            // Dron AEREO => BOMBA
            this.vida = Reglas.VIDA_DRON_AEREO;
            this.municion = Reglas.MUNICION_BOMBA;
            this.rangoVision = Reglas.VISION_BOMBA;
            this.rangoAtaque = Reglas.RANGO_ATAQUE_BOMBA;
        } else {
            // Dron NAVAL => MISIL
            this.vida = Reglas.VIDA_DRON_NAVAL;
            this.municion = Reglas.MUNICION_MISIL;
            this.rangoVision = Reglas.VISION_MISIL;
            this.rangoAtaque = Reglas.RANGO_ATAQUE_MISIL;
        }

        this.movioEsteTurno = false;
        this.disparoEsteTurno = false;
    }

    public String getId() { return id; }
    public Equipo getEquipo() { return equipo; }

    public Posicion getPosicion() { return posicion; }
    public void setPosicion(Posicion posicion) { this.posicion = Objects.requireNonNull(posicion); }

    public int getVida() { return vida; }
    public int getMunicion() { return municion; }

    public int getRangoMovimiento() { return rangoMovimiento; }
    public int getRangoVision() { return rangoVision; }
    public int getRangoAtaque() { return rangoAtaque; }

    public boolean estaVivo() { return !destruido && vida > 0; }

    public boolean puedeMover() { return estaVivo() && !movioEsteTurno; }
    public boolean puedeDisparar() { return estaVivo() && !disparoEsteTurno && municion > 0; }

    public void resetAccionesTurno() {
        movioEsteTurno = false;
        disparoEsteTurno = false;
    }

    public void marcarMovimiento() { movioEsteTurno = true; }
    public void marcarDisparo() { disparoEsteTurno = true; }

    public TipoProyectil getTipoProyectil() {
        return (equipo == Equipo.AEREO) ? TipoProyectil.BOMBA : TipoProyectil.MISIL;
    }

    public void consumirMunicion() {
        if (municion > 0) municion--;
    }

    /**
     * Regla especial:
     * Si un MISIL impacta a un dron del equipo AEREO (dron con bomba) => explota (destrucción inmediata).
     */
    public void recibirImpacto(TipoProyectil proyectil) {
        Objects.requireNonNull(proyectil);
        if (!estaVivo()) return;

        if (equipo == Equipo.AEREO && proyectil == TipoProyectil.MISIL) {
            destruir();
            return;
        }

        if (equipo == Equipo.NAVAL && proyectil != TipoProyectil.BOMBA) {
            return;
        }

        if (equipo == Equipo.AEREO && proyectil != TipoProyectil.MISIL) {
            return;
        }

        vida = Math.max(0, vida - 1);
        if (vida == 0) destruido = true;
    }

    public void destruir() {
        vida = 0;
        destruido = true;
    }


    public void disparar() {
        if (!puedeDisparar()) return;
        consumirMunicion();
        marcarDisparo();
    }

    public void mover(int dx, int dy) {
        if (!puedeMover()) return;
        int nx = this.posicion.getX() + dx;
        int ny = this.posicion.getY() + dy;
        this.posicion.setX(nx);
        this.posicion.setY(ny);
        marcarMovimiento();
    }
}
