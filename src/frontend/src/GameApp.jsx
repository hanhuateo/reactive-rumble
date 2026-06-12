import { useState, useEffect, useRef, useCallback } from "react";

// ─── Constants ────────────────────────────────────────────────────────────────
const CELL = 10;
const CANVAS_SIZE = 400;

// ─── Leaderboard Panel ────────────────────────────────────────────────────────
function Leaderboard({ entries }) {
  return (
    <div className="bg-black/60 border border-yellow-400/30 rounded-xl p-4 w-44 shrink-0">
      <h3 className="text-yellow-400 font-bold text-sm tracking-widest uppercase mb-3 flex items-center gap-1">
        <span>🏆</span> Top Snakes
      </h3>
      {entries.length === 0 ? (
        <p className="text-gray-500 text-xs italic">No scores yet</p>
      ) : (
        <ol className="space-y-1">
          {entries.map((entry, i) => (
            <li key={entry.playerId} className="flex justify-between items-center text-xs font-mono">
              <span className={`font-bold ${i === 0 ? "text-yellow-400" : i === 1 ? "text-gray-300" : i === 2 ? "text-amber-600" : "text-gray-500"}`}>
                #{i + 1} {entry.playerId.replace(/^User_/, "")}
              </span>
              <span className="text-green-400">{entry.score}</span>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

// ─── Status Badge ─────────────────────────────────────────────────────────────
function StatusBadge({ status }) {
  const isConnected = status.startsWith("Status: Connected");
  const isGameOver = status.includes("Game Over");
  return (
    <div className={`inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-mono border
      ${isGameOver ? "border-red-500/50 bg-red-900/30 text-red-400"
        : isConnected ? "border-green-500/50 bg-green-900/30 text-green-400"
        : "border-gray-600 bg-gray-800 text-gray-400"}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${isGameOver ? "bg-red-400" : isConnected ? "bg-green-400 animate-pulse" : "bg-gray-500"}`} />
      {status.replace("Status: ", "")}
    </div>
  );
}

// ─── Game Canvas ──────────────────────────────────────────────────────────────
function GameCanvas({ gameState }) {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas || !gameState) return;
    const ctx = canvas.getContext("2d");
    ctx.clearRect(0, 0, CANVAS_SIZE, CANVAS_SIZE);

    // Draw grid (subtle)
    ctx.strokeStyle = "rgba(255,255,255,0.03)";
    ctx.lineWidth = 0.5;
    for (let x = 0; x <= CANVAS_SIZE; x += CELL) {
      ctx.beginPath(); ctx.moveTo(x, 0); ctx.lineTo(x, CANVAS_SIZE); ctx.stroke();
    }
    for (let y = 0; y <= CANVAS_SIZE; y += CELL) {
      ctx.beginPath(); ctx.moveTo(0, y); ctx.lineTo(CANVAS_SIZE, y); ctx.stroke();
    }

    // Draw food
    if (gameState.food) {
      const fx = gameState.food.x * CELL;
      const fy = gameState.food.y * CELL;
      ctx.fillStyle = "#ff4444";
      ctx.shadowColor = "#ff4444";
      ctx.shadowBlur = 8;
      ctx.beginPath();
      ctx.arc(fx + CELL / 2, fy + CELL / 2, CELL / 2 - 1, 0, Math.PI * 2);
      ctx.fill();
      ctx.shadowBlur = 0;
    }

    // Draw snakes
    Object.values(gameState.players).forEach((player) => {
      player.body.forEach((seg, i) => {
        const alpha = i === 0 ? 1 : Math.max(0.4, 1 - i * 0.04);
        ctx.fillStyle = player.color + (i === 0 ? "" : Math.floor(alpha * 255).toString(16).padStart(2, "0"));
        if (i === 0) {
          ctx.shadowColor = player.color;
          ctx.shadowBlur = 6;
        }
        ctx.fillRect(seg.x * CELL + 1, seg.y * CELL + 1, CELL - 2, CELL - 2);
        ctx.shadowBlur = 0;
      });
    });
  }, [gameState]);

  return (
    <canvas
      ref={canvasRef}
      width={CANVAS_SIZE}
      height={CANVAS_SIZE}
      className="border border-gray-700 rounded-lg bg-black block"
    />
  );
}

// ─── Controls hint ────────────────────────────────────────────────────────────
function ControlsHint() {
  const keys = [
    { label: "W", sub: "↑" }, { label: "A", sub: "←" },
    { label: "S", sub: "↓" }, { label: "D", sub: "→" },
  ];
  return (
    <div className="flex items-center gap-3 mt-2 justify-center">
      <span className="text-gray-500 text-xs">Move:</span>
      <div className="flex gap-1">
        {keys.map(k => (
          <kbd key={k.label} className="bg-gray-800 border border-gray-600 text-gray-300 text-xs px-2 py-0.5 rounded font-mono">
            {k.label}
          </kbd>
        ))}
      </div>
      <span className="text-gray-600 text-xs">or Arrow Keys</span>
    </div>
  );
}

// ─── Main App ─────────────────────────────────────────────────────────────────
export default function App() {
  const [username, setUsername] = useState("");
  const [color, setColor] = useState("#00ff00");
  const [statusText, setStatusText] = useState("Status: Disconnected");
  const [gameState, setGameState] = useState(null);
  const [leaderboard, setLeaderboard] = useState([]);
  const [joined, setJoined] = useState(false);
  const [joining, setJoining] = useState(false);
  const myPlayerIdRef = useRef(null);

  // SSE stream
  useEffect(() => {
    const es = new EventSource("/game/stream");
    es.onopen = () => setStatusText("Status: Connected (Watching)");
    es.onmessage = (e) => {
      const state = JSON.parse(e.data);
      if (myPlayerIdRef.current && !state.players[myPlayerIdRef.current]) {
        setStatusText("Status: Game Over! Click Join to try again.");
        myPlayerIdRef.current = null;
        setJoined(false);
      }
      setGameState(state);
    };
    es.onerror = () => setStatusText("Status: Disconnected");
    return () => es.close();
  }, []);

  // Leaderboard polling
  useEffect(() => {
    const poll = () =>
      fetch("/game/leaderboard")
        .then((r) => r.json())
        .then(setLeaderboard)
        .catch(() => {});
    poll();
    const id = setInterval(poll, 1000);
    return () => clearInterval(id);
  }, []);

  // Keyboard controls
  const handleKeyDown = useCallback((e) => {
    if (!myPlayerIdRef.current) return;
    const map = {
      arrowup: "UP", w: "UP",
      arrowdown: "DOWN", s: "DOWN",
      arrowleft: "LEFT", a: "LEFT",
      arrowright: "RIGHT", d: "RIGHT",
    };
    const dir = map[e.key.toLowerCase()];
    if (dir) {
      e.preventDefault();
      fetch(`/game/move?id=${myPlayerIdRef.current}&dir=${dir}`, { method: "POST" });
    }
  }, []);

  useEffect(() => {
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [handleKeyDown]);

  const handleJoin = async () => {
    if (joining) return;
    setJoining(true);
    const id = "User_" + Math.floor(Math.random() * 1000);
    try {
      await fetch(`/game/profile/${id}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username: username || id, color }),
      });
      await fetch(`/game/join/${id}`, { method: "POST" });
      myPlayerIdRef.current = id;
      setJoined(true);
      setStatusText("Status: Playing as " + (username || id));
    } catch {
      setStatusText("Status: Failed to join");
    }
    setJoining(false);
  };

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex flex-col items-center py-8 px-4 font-sans">
      {/* Header */}
      <div className="mb-6 text-center">
        <h1 className="text-4xl font-black tracking-tight text-white mb-1">
          Reactive <span className="text-green-400">Rumble</span>
        </h1>
        <p className="text-gray-500 text-sm">Distributed multiplayer snake · Powered by Spring WebFlux + Redis</p>
      </div>

      {/* Join form */}
      <div className="flex flex-wrap items-center justify-center gap-3 mb-4">
        <input
          type="text"
          value={username}
          onChange={(e) => setUsername(e.target.value)}
          placeholder="Enter name"
          disabled={joined}
          className="bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm text-white placeholder-gray-500 focus:outline-none focus:border-green-500 disabled:opacity-50 w-40"
        />
        <div className="flex items-center gap-2">
          <label className="text-xs text-gray-500">Color</label>
          <input
            type="color"
            value={color}
            onChange={(e) => setColor(e.target.value)}
            disabled={joined}
            className="w-9 h-9 rounded-lg border border-gray-700 bg-gray-800 cursor-pointer disabled:opacity-50 p-0.5"
          />
        </div>
        <button
          onClick={handleJoin}
          disabled={joined || joining}
          className="bg-green-600 hover:bg-green-500 disabled:bg-gray-700 disabled:text-gray-500 disabled:cursor-not-allowed text-white font-semibold px-5 py-2 rounded-lg text-sm transition-colors"
        >
          {joining ? "Joining…" : joined ? "In Game" : "Join Game"}
        </button>
      </div>

      <div className="mb-4">
        <StatusBadge status={statusText} />
      </div>

      {/* Game area */}
      <div className="flex gap-4 items-start">
        <GameCanvas gameState={gameState} />
        <Leaderboard entries={leaderboard} />
      </div>

      <ControlsHint />

      <p className="mt-6 text-gray-700 text-xs">Open multiple tabs to test distributed sync</p>
    </div>
  );
}
