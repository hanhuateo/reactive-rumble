import { useState, useEffect, useRef, useCallback } from "react";
import GameCanvas from './components/GameCanvas'
import Leaderboard from './components/Leaderboard'
import StatusBadge from './components/StatusBadge'
import ControlsHint from './components/ControlsHint'
import AuthForm from './components/AuthForm'

// ─── Main App ─────────────────────────────────────────────────────────────────
export default function App() {
	const [user, setUser] = useState(null); // { token, id, username, color }
	const [statusText, setStatusText] = useState("Status: Disconnected");
	const [gameState, setGameState] = useState(null);
	const [leaderboard, setLeaderboard] = useState([]);
	const [joined, setJoined] = useState(false);
	const [joining, setJoining] = useState(false);
	const myPlayerIdRef = useRef(null);

	const authFetch = useCallback((url, options = {}) => {
		return fetch(url, {
			...options,
			headers: {
				...options.headers,
				"Authorization": `Bearer ${user?.token}`,
			},
		});
	}, [user]);

	// SSE stream — EventSource can't set headers, permitted without token
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
		if (!myPlayerIdRef.current || !user) return;
		const map = {
			arrowup: "UP", w: "UP",
			arrowdown: "DOWN", s: "DOWN",
			arrowleft: "LEFT", a: "LEFT",
			arrowright: "RIGHT", d: "RIGHT",
		};
		const dir = map[e.key.toLowerCase()];
		if (dir) {
			e.preventDefault();
			authFetch(`/game/move?dir=${dir}`, { method: "POST" });
		}
	}, [user, authFetch]);

	useEffect(() => {
		window.addEventListener("keydown", handleKeyDown);
		return () => window.removeEventListener("keydown", handleKeyDown);
	}, [handleKeyDown]);

	const handleJoin = async () => {
		if (joining || !user) return;
		setJoining(true);
		try {
			await authFetch("/game/profile", {
				method: "POST",
				headers: { "Content-Type": "application/json" },
				body: JSON.stringify({ color: user.color }),
			});
			await authFetch("/game/join", { method: "POST" });
			myPlayerIdRef.current = user.id;
			setJoined(true);
			setStatusText("Status: Playing as " + user.username);
		} catch {
			setStatusText("Status: Failed to join");
		}
		setJoining(false);
	};

	if (!user) return <AuthForm onAuth={setUser} />;

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
				<span className="text-sm text-gray-400">
					Logged in as <span className="text-white font-semibold">{user.username}</span>
				</span>
				<div className="flex items-center gap-2">
					<label className="text-xs text-gray-500">Color</label>
					<div
						className="w-6 h-6 rounded-full border border-gray-600"
						style={{ backgroundColor: user.color }}
					/>
				</div>
				<button
					onClick={handleJoin}
					disabled={joined || joining}
					className="bg-green-600 hover:bg-green-500 disabled:bg-gray-700 disabled:text-gray-500 disabled:cursor-not-allowed text-white font-semibold px-5 py-2 rounded-lg text-sm transition-colors"
				>
					{joining ? "Joining…" : joined ? "In Game" : "Join Game"}
				</button>
				<button
					onClick={() => { setUser(null); setJoined(false); myPlayerIdRef.current = null; }}
					className="text-gray-500 hover:text-gray-300 text-xs underline"
				>
					Log out
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
