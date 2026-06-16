import { useRef, useEffect } from "react";

export default function GameCanvas({ gameState }) {
    const CELL = 10;
    const CANVAS_SIZE = 400;
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