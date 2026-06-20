import { PhoneIncoming, ScanSearch, ShieldX } from "lucide-react"

const steps = [
  {
    icon: PhoneIncoming,
    step: "01",
    title: "Suena el teléfono",
    desc: "Una llamada entra. Antes de que vibre tu pantalla, Patova ya está revisando quién es del otro lado.",
  },
  {
    icon: ScanSearch,
    step: "02",
    title: "El patova revisa",
    desc: "Cruza el número con la base colaborativa y el análisis local de patrones de spam, telemarketing y estafas.",
  },
  {
    icon: ShieldX,
    step: "03",
    title: "Pasa o no pasa",
    desc: "Si es legítimo, te avisa. Si huele a spam, lo frena y suma el dato para proteger a toda la comunidad.",
  },
]

export function HowItWorks() {
  return (
    <section
      id="como-funciona"
      className="mx-auto max-w-6xl scroll-mt-20 px-4 py-16 sm:px-6 lg:py-24 lg:px-8"
    >
      <div className="flex flex-col items-start justify-between gap-4 sm:flex-row sm:items-end">
        <div className="max-w-2xl">
          <span className="font-mono text-[11px] uppercase tracking-[0.2em] text-primary">
            Cómo funciona
          </span>
          <h2 className="mt-3 text-balance font-heading text-3xl font-extrabold tracking-tight sm:text-4xl">
            Tres pasos. Cero llamadas molestas.
          </h2>
        </div>
        <p className="max-w-sm text-pretty text-muted-foreground">
          Todo pasa en menos de un segundo, sin que tengas que mover un dedo.
        </p>
      </div>

      <div className="mt-12 grid gap-4 md:grid-cols-3">
        {steps.map(({ icon: Icon, step, title, desc }) => (
          <div
            key={step}
            className="relative flex flex-col rounded-2xl border border-border bg-card p-6 transition-colors hover:border-primary/40"
          >
            <div className="flex items-center justify-between">
              <span className="flex size-12 items-center justify-center rounded-xl bg-primary/15 text-primary ring-1 ring-primary/25">
                <Icon className="size-6" />
              </span>
              <span className="font-heading text-4xl font-extrabold text-muted/60">
                {step}
              </span>
            </div>
            <h3 className="mt-5 font-heading text-xl font-bold">{title}</h3>
            <p className="mt-2 leading-relaxed text-muted-foreground">{desc}</p>
          </div>
        ))}
      </div>
    </section>
  )
}
