export default function StatusBadge({ status }) {
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