const ATAQUES = {
  DronNavalVsDronAereo: {
    attacker: { tex: "dronNaval", pos: "rightTop", scale: "dron" },
    target: { tex: "dronAereoSinBomba", pos: "leftBot", scale: "dron" },
    attack: { type: "missile", delay: 650, flightMs: 950, speed: 560, hitPoint: "target" },
    hit: { kind: "destroy", endR: 240 },
    next: "DronNavalVsPortaAereoImpacto"
  },

  DronNavalVsPortaAereoImpacto: {
    attacker: { tex: "dronNaval", pos: "rightTop", scale: "dron" },
    target: { tex: "portaAereo", pos: "ship", scale: "porta" },
    attack: { type: "missile", delay: 650, flightMs: 950, speed: 560, hitPoint: "ship" },
    hit: { kind: "damage", endR: 230 },
    next: "DronNavalVsPortaAereoDestruccion"
  },

  DronNavalVsPortaAereoDestruccion: {
    attacker: { tex: "dronNaval", pos: "rightTop", scale: "dron" },
    target: { tex: "portaAereo", pos: "ship", scale: "porta" },
    attack: { type: "missile", delay: 800, flightMs: 1000, speed: 560, hitPoint: "ship" },
    hit: { kind: "destroy", endR: 280 },
    next: "DronAereoVsDronNavalImpacto"
  },

  DronAereoVsDronNavalImpacto: {
    attacker: { tex: "dronAereo", pos: "rightTop", scale: "dron" },
    target: { tex: "dronNavalInvertido", pos: "leftBot", scale: "dron" },
    attack: { type: "bomb", delay: 650, flightMs: 650, hitOffset: { x: 0.02, y: 0.01 } },
    hit: { kind: "damage", endR: 220 },
    next: "DronAereoVsDronNavalDestruccion"
  },

  DronAereoVsDronNavalDestruccion: {
    attacker: { tex: "dronAereo", pos: "rightTop", scale: "dron" },
    target: { tex: "dronNavalInvertido", pos: "leftBot", scale: "dron" },
    attack: { type: "bomb", delay: 650, flightMs: 650, hitOffset: { x: 0.02, y: 0.01 } },
    hit: { kind: "destroy", endR: 250 },
    next: "DronAereoVsPortaNavalImpacto"
  },

  DronAereoVsPortaNavalImpacto: {
    attacker: { tex: "dronAereo", pos: "rightTop", scale: "dron" },
    target: { tex: "portaNaval", pos: "ship", scale: "porta" },
    attack: { type: "bomb", delay: 650, flightMs: 650, hitPoint: "ship" },
    hit: { kind: "damage", endR: 240 },
    next: "DronAereoVsPortaNavalDestruccion"
  },

  DronAereoVsPortaNavalDestruccion: {
    attacker: { tex: "dronAereo", pos: "rightTop", scale: "dron" },
    target: { tex: "portaNaval", pos: "ship", scale: "porta" },
    attack: { type: "bomb", delay: 650, flightMs: 650, hitPoint: "ship" },
    hit: { kind: "destroy", endR: 300 },
    next: null
  }
};

class AtaqueLateralBase extends Phaser.Scene {
  constructor(key) {
    super(key);
    this._damageSmoke = new Map();
  }

  preload() {
    this.load.image("fondo", "assets/FondoVistaLateral.png");

    this.load.image("dronNaval", "assets/DronMisil.png");
    this.load.image("dronNavalInvertido", "assets/DronMisilInvertido.png");

    this.load.image("dronAereo", "assets/DronAereo.png");
    this.load.image("dronAereoSinBomba", "assets/DronAereoSinBomba.png");

    this.load.image("portaAereo", "assets/PortaAereoVistaLateral.png");
    this.load.image("portaNaval", "assets/PortaNavalVistaLateral.png");

    this.load.image("misil", "assets/Misil.png");
    this.load.image("bomba", "assets/Bomba.png");
  }

  createBase() {
    const { width, height } = this.scale;

    this.add.image(width / 2, height / 2, "fondo").setDisplaySize(width, height).setDepth(0);

    this.pos = {
      rightTop: { x: width * 0.78, y: height * 0.22 },
      ship: { x: width * 0.22, y: height * 0.72 },
      leftBot: { x: width * 0.22, y: height * 0.72 },
      leftMid: { x: width * 0.22, y: height * 0.58 }
    };

    this.flashRect = this.add.rectangle(width / 2, height / 2, width, height, 0xffffff, 0).setDepth(999);

    this.physics.world.setBounds(0, 0, width, height);
    this._damageSmoke = new Map();
  }

  s(type) {
    const base = this.scale.width / 1280;
    switch (type) {
      case "dron": return 0.25 * base;
      case "porta": return 0.35 * base;
      case "misil": return 0.18 * base;
      case "bomba": return 0.18 * base;
      default: return 1 * base;
    }
  }

  shipHitPoint(ship) {
    return {
      hitX: ship.x + (this.scale.width * 0.08),
      hitY: ship.y - (this.scale.height * 0.05)
    };
  }

  hitStop(ms = 45) {
    this.physics.world.pause();
    this.tweens.pauseAll();
    this.time.timeScale = 0;

    setTimeout(() => {
      this.time.timeScale = 1;
      this.tweens.resumeAll();
      this.physics.world.resume();
    }, ms);
  }

  shake(ms = 300, intensity = 0.01) {
    this.cameras.main.shake(ms, intensity);
  }

  flash(ms = 80, alpha = 0.25) {
    this.flashRect.setAlpha(alpha);
    this.tweens.add({ targets: this.flashRect, alpha: 0, duration: ms, ease: "Quad.easeOut" });
  }

  debris(x, y, amount = 10, cfg = {}) {
    const lifeMin = cfg.lifeMin ?? 420;
    const lifeMax = cfg.lifeMax ?? 720;

    for (let i = 0; i < amount; i++) {
      const w = Phaser.Math.Between(6, 14);
      const h = Phaser.Math.Between(3, 10);
      const d = this.add.rectangle(x, y, w, h, 0x222222, 0.9).setDepth(62);

      const ang = Phaser.Math.FloatBetween(-Math.PI, Math.PI);
      const dist = Phaser.Math.Between(80, 220);

      this.tweens.add({
        targets: d,
        x: x + Math.cos(ang) * dist,
        y: y + Math.sin(ang) * dist - Phaser.Math.Between(20, 90),
        rotation: Phaser.Math.FloatBetween(-3, 3),
        alpha: 0,
        duration: Phaser.Math.Between(lifeMin, lifeMax),
        ease: "Quad.easeOut",
        onComplete: () => d.destroy()
      });
    }
  }

  startDamageSmoke(target, cfg = {}) {
    if (!target?.active) return;
    if (this._damageSmoke.has(target)) return;

    const rate = cfg.rate ?? 120;
    const baseAlpha = cfg.alpha ?? 0.35;

    const timer = this.time.addEvent({
      delay: rate,
      loop: true,
      callback: () => {
        if (!target.active) return;

        const x = target.x + Phaser.Math.Between(-20, 20);
        const y = target.y + Phaser.Math.Between(-15, 10);

        const p = this.add.circle(x, y, Phaser.Math.Between(10, 18), 0x333333, baseAlpha).setDepth(56);

        this.tweens.add({
          targets: p,
          x: p.x + Phaser.Math.Between(-50, 50),
          y: p.y - Phaser.Math.Between(60, 120),
          alpha: 0,
          duration: Phaser.Math.Between(900, 1400),
          ease: "Quad.easeOut",
          onComplete: () => p.destroy()
        });
      }
    });

    this._damageSmoke.set(target, timer);
  }

  stopDamageSmoke(target) {
    const t = this._damageSmoke.get(target);
    if (t) t.remove(false);
    this._damageSmoke.delete(target);
  }

  explosion(x, y, cfg = {}) {
    const color = cfg.color ?? 0xff5500;
    const endR = cfg.endR ?? 220;
    const duration = cfg.duration ?? 650;

    const white = this.add.circle(x, y, 10, 0xffffff, 0.85).setDepth(60);
    this.tweens.add({
      targets: white,
      radius: endR * 0.55,
      alpha: 0,
      duration: 130,
      ease: "Quad.easeOut",
      onComplete: () => white.destroy()
    });

    const boom = this.add.circle(x, y, 18, color, 1).setDepth(59);
    this.tweens.add({
      targets: boom,
      radius: endR,
      alpha: 0,
      duration,
      ease: "Cubic.easeOut",
      onComplete: () => boom.destroy()
    });

    const ring = this.add.circle(x, y, 22, 0xffffff, 0).setDepth(58);
    ring.setStrokeStyle(4, 0xffddaa, 0.9);
    this.tweens.add({
      targets: ring,
      radius: endR * 1.1,
      alpha: 0,
      duration: 420,
      ease: "Quad.easeOut",
      onComplete: () => ring.destroy()
    });

    for (let i = 0; i < 12; i++) {
      const s = this.add.circle(x, y, Phaser.Math.Between(2, 4), 0xffee66, 1).setDepth(61);
      const ang = Phaser.Math.FloatBetween(0, Math.PI * 2);
      const dist = Phaser.Math.Between(60, 160);

      this.tweens.add({
        targets: s,
        x: x + Math.cos(ang) * dist,
        y: y + Math.sin(ang) * dist,
        alpha: 0,
        duration: Phaser.Math.Between(260, 520),
        ease: "Quad.easeOut",
        onComplete: () => s.destroy()
      });
    }

    for (let i = 0; i < 10; i++) {
      const p = this.add.circle(
        x + Phaser.Math.Between(-15, 15),
        y + Phaser.Math.Between(-10, 10),
        Phaser.Math.Between(10, 20),
        0x333333,
        0.55
      ).setDepth(57);

      this.tweens.add({
        targets: p,
        x: p.x + Phaser.Math.Between(-90, 90),
        y: p.y - Phaser.Math.Between(80, 160),
        alpha: 0,
        duration: Phaser.Math.Between(900, 1300),
        ease: "Quad.easeOut",
        onComplete: () => p.destroy()
      });
    }
  }

  fadeOutDestroy(obj, ms = 520) {
    this.tweens.add({
      targets: obj,
      alpha: 0,
      duration: ms,
      ease: "Quad.easeOut",
      onComplete: () => obj.destroy()
    });
  }

  wobble(obj, deg = 5) {
    const ox = obj.x, oy = obj.y;
    this.tweens.add({ targets: obj, angle: deg, yoyo: true, repeat: 5, duration: 90 });
    this.tweens.add({ targets: obj, x: ox - 10, y: oy + 6, yoyo: true, duration: 150, ease: "Quad.easeOut" });
  }

  hitDamage(target, hitX, hitY, opts = {}) {
    this.hitStop(opts.hitStop ?? 45);
    this.flash(70, 0.18);
    this.shake(opts.shakeMs ?? 320, opts.shakeInt ?? 0.012);

    this.explosion(hitX, hitY, { endR: opts.endR ?? 210, color: opts.color ?? 0xff6600, duration: 600 });

    this.debris(hitX, hitY, opts.debris ?? 6);
    this.startDamageSmoke(target, { rate: 140, alpha: 0.28 });

    target.setTint(0xff6666);
    this.time.delayedCall(220, () => target.clearTint());

    this.wobble(target, 6);
    this.tweens.add({ targets: target, alpha: 0.5, yoyo: true, repeat: 3, duration: 90 });
  }

  hitDestroy(target, hitX, hitY, opts = {}) {
    this.hitStop(opts.hitStop ?? 55);
    this.flash(90, 0.25);
    this.shake(opts.shakeMs ?? 450, opts.shakeInt ?? 0.02);

    this.cameras.main.zoomTo(1.10, 180);
    this.time.delayedCall(320, () => this.cameras.main.zoomTo(1.0, 240));

    this.explosion(hitX, hitY, { endR: opts.endR ?? 260, color: opts.color ?? 0xff3300, duration: 720 });

    this.debris(hitX, hitY, opts.debris ?? 14);
    this.stopDamageSmoke(target);
    this.fadeOutDestroy(target, opts.fadeMs ?? 650);
  }

  shootMissile(fromSprite, toX, toY, speed = 560) {
    const m = this.physics.add.image(fromSprite.x - 50, fromSprite.y + 15, "misil")
      .setScale(this.s("misil"))
      .setDepth(10);

    const ang = Phaser.Math.Angle.Between(m.x, m.y, toX, toY);
    m.setRotation(ang + Math.PI);
    this.physics.moveTo(m, toX, toY, speed);

    const timer = this.time.addEvent({
      delay: 40,
      loop: true,
      callback: () => {
        if (!m.active) return;

        const smoke = this.add.circle(m.x + 10, m.y + 4, Phaser.Math.Between(6, 10), 0x444444, 0.35)
          .setDepth(9);

        this.tweens.add({
          targets: smoke,
          x: smoke.x + Phaser.Math.Between(15, 35),
          y: smoke.y + Phaser.Math.Between(-10, 10),
          alpha: 0,
          duration: 420,
          ease: "Quad.easeOut",
          onComplete: () => smoke.destroy()
        });
      }
    });

    m.on("destroy", () => timer.remove(false));
    this.tweens.add({ targets: m, angle: m.angle + 2, yoyo: true, repeat: -1, duration: 80 });

    return m;
  }

  dropBomb(fromSprite, hitY, duration = 650) {
    const startX = fromSprite.x - 10;
    const startY = fromSprite.y + 35;

    const b = this.add.image(startX, startY, "bomba")
      .setScale(this.s("bomba"))
      .setDepth(10);

    const shadow = this.add.ellipse(startX, hitY + 55, 26, 10, 0x000000, 0.22).setDepth(1);

    this.tweens.add({
      targets: b,
      y: hitY,
      rotation: Phaser.Math.DegToRad(360),
      duration,
      ease: "Quad.easeIn",
      onComplete: () => { b.destroy(); shadow.destroy(); }
    });

    this.tweens.add({
      targets: shadow,
      scaleX: 1.6,
      scaleY: 0.7,
      duration,
      ease: "Quad.easeIn"
    });

    return b;
  }
}

class AtaqueLateral extends AtaqueLateralBase {
  constructor() { super("AtaqueLateral"); }
  init(data) { this.attackKey = data?.key; }

  create() {
    this.createBase();

    const cfg = ATAQUES[this.attackKey];
    if (!cfg) return;

    const pos = (name) => this.pos[name] ?? this.pos.leftBot;

    this.atacante = this.add.image(pos(cfg.attacker.pos).x, pos(cfg.attacker.pos).y, cfg.attacker.tex)
      .setScale(this.s(cfg.attacker.scale))
      .setDepth(5);

    this.objetivo = this.add.image(pos(cfg.target.pos).x, pos(cfg.target.pos).y, cfg.target.tex)
      .setScale(this.s(cfg.target.scale))
      .setDepth(5);

    this.time.delayedCall(cfg.attack.delay ?? 650, () => this.doAttack(cfg));
  }

  resolveHitPoint(cfg) {
    if (cfg.attack.hitPoint === "ship") return this.shipHitPoint(this.objetivo);
    if (cfg.attack.hitOffset) {
      return {
        hitX: this.objetivo.x + this.scale.width * cfg.attack.hitOffset.x,
        hitY: this.objetivo.y + this.scale.height * cfg.attack.hitOffset.y
      };
    }
    return { hitX: this.objetivo.x, hitY: this.objetivo.y };
  }

  doAttack(cfg) {
    const { hitX, hitY } = this.resolveHitPoint(cfg);

    if (cfg.attack.type === "missile") {
      const m = this.shootMissile(this.atacante, hitX, hitY, cfg.attack.speed ?? 560);
      this.time.delayedCall(cfg.attack.flightMs ?? 950, () => {
        m.destroy();
        this.applyHit(cfg, hitX, hitY);
      });
      return;
    }

    if (cfg.attack.type === "bomb") {
      this.tweens.add({
        targets: this.atacante,
        x: hitX + 10,
        duration: 220,
        ease: "Sine.easeInOut",
        onComplete: () => this.dropBomb(this.atacante, hitY, cfg.attack.flightMs ?? 650)
      });

      this.time.delayedCall((cfg.attack.flightMs ?? 650) + 220, () => {
        this.applyHit(cfg, hitX, hitY);
      });
    }
  }

  applyHit(cfg, hitX, hitY) {
    if (cfg.hit.kind === "damage") this.hitDamage(this.objetivo, hitX, hitY, cfg.hit);
    else this.hitDestroy(this.objetivo, hitX, hitY, cfg.hit);

    if (cfg.next) this.time.delayedCall(1200, () => this.scene.start(cfg.next));
  }
}

class DronNavalVsDronAereo extends AtaqueLateralBase { constructor(){ super("DronNavalVsDronAereo"); } create(){ this.scene.start("AtaqueLateral",{ key:"DronNavalVsDronAereo" }); } }
class DronNavalVsPortaAereoImpacto extends AtaqueLateralBase { constructor(){ super("DronNavalVsPortaAereoImpacto"); } create(){ this.scene.start("AtaqueLateral",{ key:"DronNavalVsPortaAereoImpacto" }); } }
class DronNavalVsPortaAereoDestruccion extends AtaqueLateralBase { constructor(){ super("DronNavalVsPortaAereoDestruccion"); } create(){ this.scene.start("AtaqueLateral",{ key:"DronNavalVsPortaAereoDestruccion" }); } }
class DronAereoVsDronNavalImpacto extends AtaqueLateralBase { constructor(){ super("DronAereoVsDronNavalImpacto"); } create(){ this.scene.start("AtaqueLateral",{ key:"DronAereoVsDronNavalImpacto" }); } }
class DronAereoVsDronNavalDestruccion extends AtaqueLateralBase { constructor(){ super("DronAereoVsDronNavalDestruccion"); } create(){ this.scene.start("AtaqueLateral",{ key:"DronAereoVsDronNavalDestruccion" }); } }
class DronAereoVsPortaNavalImpacto extends AtaqueLateralBase { constructor(){ super("DronAereoVsPortaNavalImpacto"); } create(){ this.scene.start("AtaqueLateral",{ key:"DronAereoVsPortaNavalImpacto" }); } }
class DronAereoVsPortaNavalDestruccion extends AtaqueLateralBase { constructor(){ super("DronAereoVsPortaNavalDestruccion"); } create(){ this.scene.start("AtaqueLateral",{ key:"DronAereoVsPortaNavalDestruccion" }); } }

class DemoBoot extends Phaser.Scene {
  constructor() { super("DemoBoot"); }
  create() { this.scene.start("DronNavalVsDronAereo"); }
}

const config = {
  type: Phaser.AUTO,
  width: 1280,
  height: 720,
  parent: "game-container",
  physics: { default: "arcade" },
  scale: { mode: Phaser.Scale.FIT, autoCenter: Phaser.Scale.CENTER_BOTH },
  scene: [
    DemoBoot,
    AtaqueLateral,
    DronNavalVsDronAereo,
    DronNavalVsPortaAereoImpacto,
    DronNavalVsPortaAereoDestruccion,
    DronAereoVsDronNavalImpacto,
    DronAereoVsDronNavalDestruccion,
    DronAereoVsPortaNavalImpacto,
    DronAereoVsPortaNavalDestruccion
  ]
};

new Phaser.Game(config);