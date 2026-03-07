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
        this(
                UUID.randomUUID().toString(),
                equipo,
                posicion,
                (equipo == Equipo.AEREO) ? Reglas.VIDA_DRON_AEREO : Reglas.VIDA_DRON_NAVAL,
                (equipo == Equipo.AEREO) ? Reglas.MUNICION_BOMBA : Reglas.MUNICION_MISIL,
                false,
                false
        );
    }

    // Para reconstruccion desde persistencia
    public Dron(String id,
                Equipo equipo,
                Posicion posicion,
                int vida,
                int municion,
                boolean movioEsteTurno,
                boolean disparoEsteTurno) {
        this.id = Objects.requireNonNull(id);
        this.equipo = Objects.requireNonNull(equipo);
        this.posicion = Objects.requireNonNull(posicion);
        this.destruido = vida <= 0;

        this.rangoMovimiento = Reglas.RANGO_MOVIMIENTO_DRON;

        if (equipo == Equipo.AEREO) {
            this.rangoVision = Reglas.VISION_BOMBA;
            this.rangoAtaque = Reglas.RANGO_ATAQUE_BOMBA;
        } else {
            this.rangoVision = Reglas.VISION_MISIL;
            this.rangoAtaque = Reglas.RANGO_ATAQUE_MISIL;
        }

        this.vida = Math.max(0, vida);
        this.municion = Math.max(0, municion);
        this.movioEsteTurno = movioEsteTurno;
        this.disparoEsteTurno = disparoEsteTurno;
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
    public boolean isMovioEsteTurno() { return movioEsteTurno; }
    public boolean isDisparoEsteTurno() { return disparoEsteTurno; }

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
