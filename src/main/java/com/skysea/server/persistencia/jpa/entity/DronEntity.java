package com.skysea.server.persistencia.jpa.entity;

import com.skysea.server.logica.model.Equipo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "dron")
public class DronEntity {

    @Id
    @Column(name = "id", nullable = false, length = 36)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private Equipo tipo;

    @Column(name = "x", nullable = false)
    private int x;

    @Column(name = "y", nullable = false)
    private int y;

    @Column(name = "altura", nullable = false)
    private int altura;

    @Column(name = "vivo", nullable = false)
    private boolean vivo;

    @Column(name = "vida", nullable = false)
    private int vida;

    @Column(name = "municion", nullable = false)
    private int municion;

    @Column(name = "destruido", nullable = false)
    private boolean destruido;

    @Column(name = "movio_este_turno", nullable = false)
    private boolean movioEsteTurno;

    @Column(name = "disparo_este_turno", nullable = false)
    private boolean disparoEsteTurno;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "jugador_id")
    private JugadorEntity jugador;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Equipo getEquipo() {
        return tipo;
    }

    public void setEquipo(Equipo equipo) {
        this.tipo = equipo;
    }

    public int getPosX() {
        return x;
    }

    public void setPosX(int posX) {
        this.x = posX;
    }

    public int getPosY() {
        return y;
    }

    public void setPosY(int posY) {
        this.y = posY;
    }

    public int getAltura() {
        return altura;
    }

    public void setAltura(int altura) {
        this.altura = altura;
    }

    public boolean isVivo() {
        return vivo;
    }

    public void setVivo(boolean vivo) {
        this.vivo = vivo;
    }

    public int getVida() {
        return vida;
    }

    public void setVida(int vida) {
        this.vida = vida;
    }

    public int getMunicion() {
        return municion;
    }

    public void setMunicion(int municion) {
        this.municion = municion;
    }

    public boolean isDestruido() {
        return destruido;
    }

    public void setDestruido(boolean destruido) {
        this.destruido = destruido;
    }

    public boolean isMovioEsteTurno() {
        return movioEsteTurno;
    }

    public void setMovioEsteTurno(boolean movioEsteTurno) {
        this.movioEsteTurno = movioEsteTurno;
    }

    public boolean isDisparoEsteTurno() {
        return disparoEsteTurno;
    }

    public void setDisparoEsteTurno(boolean disparoEsteTurno) {
        this.disparoEsteTurno = disparoEsteTurno;
    }

    public JugadorEntity getJugador() {
        return jugador;
    }

    public void setJugador(JugadorEntity jugador) {
        this.jugador = jugador;
    }
}
