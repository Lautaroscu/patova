import {
  Cpu,
  Users,
  Lock,
  Bell,
  ListChecks,
  Globe2,
} from "lucide-react"

const features = [
  {
    icon: Cpu,
    title: "Filtrado 100% local",
    desc: "El análisis corre en tu teléfono. Tus contactos y llamadas no salen del dispositivo.",
  },
  {
    icon: Users,
    title: "Base colaborativa",
    desc: "Cada denuncia entrena a la red. Lo que molesta a uno, lo bloqueamos para todos.",
  },
  {
    icon: Bell,
    title: "Identificación al instante",
    desc: "Sabés quién llama antes de atender, con etiquetas claras de spam o confiable.",
  },
  {
    icon: ListChecks,
    title: "Listas a tu medida",
    desc: "Sumá números o prefijos a tu lista negra y blanca en dos toques.",
  },
  {
    icon: Globe2,
    title: "Pensado para Argentina",
    desc: "Reconoce prefijos, 0800 y patrones de telemarketing locales.",
  },
  {
    icon: Lock,
    title: "Privacidad real",
    desc: "Sin venta de datos, sin anuncios. Tu información es tuya y punto.",
  },
]

export function FeaturesSection() {
  return (
    <section
      id="proteccion"
      className="scroll-mt-20 border-y border-border bg-card/30"
    >
      <div className="mx-auto max-w-6xl px-4 py-16 sm:px-6 lg:py-24 lg:px-8">
        <div className="max-w-2xl">
          <span className="font-mono text-[11px] uppercase tracking-[0.2em] text-primary">
            Protección
          </span>
          <h2 className="mt-3 text-balance font-heading text-3xl font-extrabold tracking-tight sm:text-4xl">
            Un patova que no se distrae.
          </h2>
          <p className="mt-4 text-pretty text-lg leading-relaxed text-muted-foreground">
            Todo lo que necesitás para recuperar el control de tu teléfono, sin
            complicaciones y sin resignar privacidad.
          </p>
        </div>

        <div className="mt-12 grid gap-px overflow-hidden rounded-2xl border border-border bg-border sm:grid-cols-2 lg:grid-cols-3">
          {features.map(({ icon: Icon, title, desc }) => (
            <div
              key={title}
              className="group bg-card p-6 transition-colors hover:bg-secondary"
            >
              <span className="flex size-11 items-center justify-center rounded-lg bg-primary/15 text-primary ring-1 ring-primary/25 transition-transform group-hover:-translate-y-0.5">
                <Icon className="size-5" />
              </span>
              <h3 className="mt-4 font-heading text-lg font-bold">{title}</h3>
              <p className="mt-2 leading-relaxed text-muted-foreground">
                {desc}
              </p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
