export default function Leaderboard({ entries }) {
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