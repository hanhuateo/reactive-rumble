import { useState } from "react";

export default function AuthForm({ onAuth }) {
  const [tab, setTab] = useState("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [color, setColor] = useState("#00ff00");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError("");
    setLoading(true);
    try {
      const res = await fetch(`/auth/${tab}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password, color }),
      });
      if (!res.ok) {
        const text = await res.text();
        // Spring ResponseStatusException wraps the message in JSON
        try {
          console.log("try block");
          const json = JSON.parse(text);
          throw new Error(json.detail || json.message || text);
        } catch {
          console.log("catch block");
          throw new Error(text);
        }
      }
      onAuth(await res.json());
    } catch (err) {
      setError(err.message);
    }
    setLoading(false);
  };

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100 flex flex-col items-center justify-center px-4">
      <h1 className="text-4xl font-black tracking-tight text-white mb-2">
        Reactive <span className="text-green-400">Rumble</span>
      </h1>
      <p className="text-gray-500 text-sm mb-8">Distributed multiplayer snake</p>

      <div className="bg-gray-900 border border-gray-800 rounded-xl p-6 w-full max-w-sm">
        <div className="flex mb-6 bg-gray-800 rounded-lg p-1">
          {["login", "register"].map((t) => (
            <button
              key={t}
              type="button"
              onClick={() => { setTab(t); setError(""); }}
              className={`flex-1 py-1.5 rounded-md text-sm font-semibold transition-colors capitalize ${tab === t ? "bg-green-600 text-white" : "text-gray-400 hover:text-white"
                }`}
            >
              {t}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <input
            type="text"
            placeholder="Username"
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            required
            className="bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm text-white placeholder-gray-500 focus:outline-none focus:border-green-500"
          />
          <input
            type="password"
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            className="bg-gray-800 border border-gray-700 rounded-lg px-3 py-2 text-sm text-white placeholder-gray-500 focus:outline-none focus:border-green-500"
          />
          {tab === "register" && (
            <div className="flex items-center gap-3">
              <label className="text-xs text-gray-400">Snake color</label>
              <input
                type="color"
                value={color}
                onChange={(e) => setColor(e.target.value)}
                className="w-9 h-9 rounded-lg border border-gray-700 bg-gray-800 cursor-pointer p-0.5"
              />
            </div>
          )}
          {error && <p className="text-red-400 text-xs">Username and/or password is wrong.</p>}
          <button
            type="submit"
            disabled={loading}
            className="bg-green-600 hover:bg-green-500 disabled:bg-gray-700 disabled:text-gray-500 text-white font-semibold py-2 rounded-lg text-sm transition-colors"
          >
            {loading ? "Please wait…" : tab === "login" ? "Log In" : "Create Account"}
          </button>
        </form>
      </div>
    </div>
  );
}
