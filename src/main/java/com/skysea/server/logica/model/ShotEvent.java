package com.skysea.server.logica.model;

public class ShotEvent {
    private long seq;

    private String weapon;        // "MISIL" o "BOMBA" (o lo que uses: proyectil.name())
    private String attackerEquipo; // "NAVAL" o "AEREO"
    private String target;        // "DRON" o "PORTA"
    private String targetEquipo;  // "NAVAL" o "AEREO"
    private String result;        // "HIT" o "MISS"
    private String hitKind;       // "damage" o "destroy" (para tu animación)
    private Integer remainingHealth; // vida restante dron / impactos restantes porta (si aplica)

    public ShotEvent() {}

    public ShotEvent(long seq, String weapon, String attackerEquipo, String target, String targetEquipo,
                     String result, String hitKind, Integer remainingHealth) {
        this.seq = seq;
        this.weapon = weapon;
        this.attackerEquipo = attackerEquipo;
        this.target = target;
        this.targetEquipo = targetEquipo;
        this.result = result;
        this.hitKind = hitKind;
        this.remainingHealth = remainingHealth;
    }

    public long getSeq() { return seq; }
    public void setSeq(long seq) { this.seq = seq; }

    public String getWeapon() { return weapon; }
    public void setWeapon(String weapon) { this.weapon = weapon; }

    public String getAttackerEquipo() { return attackerEquipo; }
    public void setAttackerEquipo(String attackerEquipo) { this.attackerEquipo = attackerEquipo; }

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getTargetEquipo() { return targetEquipo; }
    public void setTargetEquipo(String targetEquipo) { this.targetEquipo = targetEquipo; }

    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }

    public String getHitKind() { return hitKind; }
    public void setHitKind(String hitKind) { this.hitKind = hitKind; }

    public Integer getRemainingHealth() { return remainingHealth; }
    public void setRemainingHealth(Integer remainingHealth) { this.remainingHealth = remainingHealth; }
}