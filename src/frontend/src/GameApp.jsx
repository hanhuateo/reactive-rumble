import { useState, useEffect, useRef, useCallback } from "react";
import GameCanvas from './components/GameCanvas'
import Leaderboard from './components/Leaderboard'
import StatusBadge from './components/StatusBadge'
import ControlsHint from './components/ControlsHint'

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
				.catch(() => { });
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
