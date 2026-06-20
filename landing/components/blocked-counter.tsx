"use client"

import { useEffect, useRef, useState } from "react"

const START = 1247831

export function BlockedCounter() {
  const [count, setCount] = useState(START)
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const id = window.setInterval(() => {
      setCount((c) => c + Math.floor(Math.random() * 3) + 1)
    }, 1800)
    return () => window.clearInterval(id)
  }, [])

  const digits = count.toLocaleString("es-AR").split("")

  return (
    <div
      ref={ref}
      className="rounded-2xl border border-border bg-card/60 p-5 shadow-sm backdrop-blur-sm"
    >
      <div className="flex items-center justify-between">
        <span className="font-mono text-[11px] uppercase tracking-[0.2em] text-muted-foreground">
          Contador en vivo
        </span>
        <span className="flex items-center gap-1.5 font-mono text-[11px] uppercase tracking-[0.16em] text-primary">
          <span className="relative flex size-2">
            <span className="absolute inline-flex size-full animate-ping rounded-full bg-primary opacity-70" />
            <span className="relative inline-flex size-2 rounded-full bg-primary" />
          </span>
          activo
        </span>
      </div>

      <div className="mt-4 flex flex-wrap items-center gap-1.5" aria-label={`${count} llamadas bloqueadas`}>
        {digits.map((d, i) => (
          <span
            key={i}
            className={
              d === "."
                ? "px-0.5 font-heading text-3xl text-muted-foreground sm:text-4xl"
                : "flex h-12 min-w-9 items-center justify-center rounded-md bg-secondary font-heading text-3xl font-extrabold tabular-nums text-foreground sm:h-14 sm:min-w-10 sm:text-4xl"
            }
          >
            {d}
          </span>
        ))}
      </div>

      <p className="mt-4 text-sm text-muted-foreground">
        Llamadas de spam frenadas por la red Patova en Argentina.
      </p>
    </div>
  )
}
