package com.skysea.server.logica.model;

public class EstadoJuego {

    private Dron dron;
    private TipoProyectil ultimoDisparo;
    private int numeroTurno;
    private Equipo turnoDe; 

    public EstadoJuego(Dron dron) {
        this.dron = dron;
    }
    
    public int getNumeroTurno() {
    return numeroTurno;
    }

    public void setNumeroTurno(int numeroTurno) {
        this.numeroTurno = numeroTurno;
    }

    public Equipo getTurnoDe() {
        return turnoDe;
    }

    public void setTurnoDe(Equipo turnoDe) {
        this.turnoDe = turnoDe;
    }


    public Dron getDron() {
        return dron;
    }

    public TipoProyectil getUltimoDisparo() {
        return ultimoDisparo;
    }

    public void setUltimoDisparo(TipoProyectil ultimoDisparo) {
        this.ultimoDisparo = ultimoDisparo;
    }
}
