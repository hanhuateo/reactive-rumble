export default function ControlsHint() {
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