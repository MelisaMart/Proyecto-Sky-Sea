package com.skysea.server.persistencia.jpa.entity;

import com.skysea.server.logica.model.Equipo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "porta")
public class PortaEntity {

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

    @Column(name = "impactos_restantes", nullable = false)
    private int impactosRestantes;

    @Column(name = "vivo", nullable = false)
    private boolean vivo;

    @OneToOne
    @JoinColumn(name = "jugador_id", nullable = false)
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

    public int getImpactosRestantes() {
        return impactosRestantes;
    }

    public void setImpactosRestantes(int impactosRestantes) {
        this.impactosRestantes = impactosRestantes;
    }

    public boolean isVivo() {
        return vivo;
    }

    public void setVivo(boolean vivo) {
        this.vivo = vivo;
    }

    public JugadorEntity getJugador() {
        return jugador;
    }

    public void setJugador(JugadorEntity jugador) {
        this.jugador = jugador;
    }
}
