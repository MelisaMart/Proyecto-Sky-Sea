const equipoEl = document.getElementById("equipo");
const municionEl = document.getElementById("municion");
const ultimoDisparoEl = document.getElementById("ultimoDisparo");
const dronEl = document.getElementById("dron");
const tableroEl = document.getElementById("tablero");

const estadoPartidaEl = document.getElementById("estadoPartida");
const turnoDeEl = document.getElementById("turnoDe");

const btnJoin = document.getElementById("btnJoin");
const btnSalir = document.getElementById("btnSalir");
const nombreInput = document.getElementById("nombreInput");
const msgEl = document.getElementById("msg");

const panelJuego = document.getElementById("panelJuego");

const btnState = document.getElementById("btnState");
const btnFire = document.getElementById("btnFire");
const btnReset = document.getElementById("btnReset");
const btnEndTurn = document.getElementById("btnEndTurn");

// Ajustá si tu tablero no es 15x15. Esto es solo para mapear a pixeles.
const GRID = 15;

let pollTimer = null;

function setMsg(text) {
  msgEl.textContent = text || "";
}

function getPlayerId() {
  return localStorage.getItem("playerId");
}

function setPlayerId(id) {
  localStorage.setItem("playerId", id);
}

function clearSession() {
  localStorage.removeItem("playerId");
  localStorage.removeItem("idPartida");
  setMsg("Sesión borrada. Volvé a entrar con tu nombre.");
  panelJuego.style.display = "none";
  stopPolling();
}

function renderState2(state) {
  if (state.error) {
    setMsg("Error: " + state.error);
    return;
  }

  estadoPartidaEl.textContent = state.estadoPartida;
  turnoDeEl.textContent = state.turnoDe;

  // por ahora munición y dron no vienen de state2 
  equipoEl.textContent = state.equipo;
  municionEl.textContent = "-";
  ultimoDisparoEl.textContent = "-";
}

function startPolling() {
  if (pollTimer) return;
  pollTimer = setInterval(getState2, 1000); // cada 1s
}

function stopPolling() {
  if (pollTimer) clearInterval(pollTimer);
  pollTimer = null;
}


async function join(nombre) {
  const res = await fetch(`/api/game/join?nombre=${encodeURIComponent(nombre)}`, { method: "POST" });
  const data = await res.json();

  if (data.estadoPartida === "OCUPADO") {
    setMsg("Partida ocupada. Ya hay 2 jugadores.");
    panelJuego.style.display = "none";
    return;
  }

  setPlayerId(data.playerId);
  localStorage.setItem("idPartida", data.idPartida);

  setMsg(`Entraste como ${data.nombre} (${data.equipo}). Estado: ${data.estadoPartida}`);
  panelJuego.style.display = "block";
  startPolling();

  await getState2(); // refresco inicial
}

async function getState2() {
  const playerId = getPlayerId();

  if (!playerId) {
    stopPolling();
    setMsg("No hay sesión. Ingresá tu nombre y presioná Entrar.");
    panelJuego.style.display = "none";
    return;
  }

  const res = await fetch(`/api/game/state2?playerId=${encodeURIComponent(playerId)}`);
  const data = await res.json();
  renderState2(data);
}


btnJoin.addEventListener("click", async () => {
  const nombre = (nombreInput.value || "").trim();
  if (!nombre) {
    setMsg("Ingresá un nombre.");
    return;
  }
  await join(nombre);
});

btnSalir.addEventListener("click", clearSession);

btnState.addEventListener("click", getState2);

// Estos botones por ahora no hacen nada multiusuario (los dejamos bloqueados para no confundir)
btnFire.addEventListener("click", () => setMsg("Disparar: todavía no conectado al modo multiusuario."));
btnReset.addEventListener("click", () => setMsg("Reset: todavía no conectado al modo multiusuario."));
btnEndTurn.addEventListener("click", async () => {
  const playerId = getPlayerId();
  if (!playerId) {
    setMsg("No hay sesión. Entrá con tu nombre.");
    return;
  }

  const res = await fetch(`/api/game/endTurn2?playerId=${encodeURIComponent(playerId)}`, {
    method: "POST"
  });

  const data = await res.json();

  if (data.error) {
    setMsg("No se pudo terminar el turno: " + data.error);
    return;
  }

  setMsg("Turno cambiado. Ahora es turno de: " + data.turnoDe);
  await getState2(); // refresca la vista
});

// Al abrir la página, si ya hay sesión guardada, muestra panel y pide state2
if (getPlayerId()) {
  startPolling();  
  getState2();
}

