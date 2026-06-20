import { Check, ShieldCheck } from "lucide-react"

const plans = [
  {
    name: "Vecino",
    price: "$0",
    period: "/ siempre",
    desc: "Lo esencial para frenar el spam más común.",
    features: [
      "Bloqueo automático de spam",
      "Identificación de llamadas",
      "Hasta 30 denuncias por mes",
      "Lista negra personal",
    ],
    cta: "Empezar gratis",
    featured: false,
  },
  {
    name: "Patova",
    price: "$2.900",
    period: "/ mes",
    desc: "Protección completa para vos y tu familia.",
    features: [
      "Todo lo del plan Vecino",
      "Denuncias ilimitadas",
      "Detección avanzada de estafas",
      "Listas blancas y negras sin límite",
      "Bloqueo de SMS spam",
      "Soporte prioritario",
    ],
    cta: "Activar Patova",
    featured: true,
  },
  {
    name: "Barrio",
    price: "$9.900",
    period: "/ mes",
    desc: "Para comercios y equipos de hasta 5 líneas.",
    features: [
      "Todo lo del plan Patova",
      "Hasta 5 líneas protegidas",
      "Panel de control compartido",
      "Reportes mensuales",
    ],
    cta: "Proteger mi negocio",
    featured: false,
  },
]

export function PlansSection() {
  return (
    <section
      id="planes"
      className="mx-auto max-w-6xl scroll-mt-20 px-4 py-16 sm:px-6 lg:py-24 lg:px-8"
    >
      <div className="mx-auto max-w-2xl text-center">
        <span className="font-mono text-[11px] uppercase tracking-[0.2em] text-primary">
          Planes
        </span>
        <h2 className="mt-3 text-balance font-heading text-3xl font-extrabold tracking-tight sm:text-4xl">
          Elegí cuánta tranquilidad querés.
        </h2>
        <p className="mt-4 text-pretty text-lg leading-relaxed text-muted-foreground">
          Sin permanencia. Cancelás cuando quieras, sin vueltas.
        </p>
      </div>

      <div className="mt-12 grid items-start gap-6 lg:grid-cols-3">
        {plans.map((plan) => (
          <div
            key={plan.name}
            className={
              plan.featured
                ? "relative rounded-2xl border border-primary/60 bg-card p-6 shadow-[0_0_0_1px_oklch(0.82_0.17_150/0.25),0_24px_60px_-30px_oklch(0.82_0.17_150/0.5)] lg:-mt-4 lg:p-8"
                : "rounded-2xl border border-border bg-card p-6"
            }
          >
            {plan.featured && (
              <span className="absolute -top-3 left-6 inline-flex items-center gap-1.5 rounded-full bg-primary px-3 py-1 text-xs font-semibold text-primary-foreground">
                <ShieldCheck className="size-3.5" />
                Más elegido
              </span>
            )}
            <h3 className="font-heading text-xl font-bold">{plan.name}</h3>
            <p className="mt-1 text-sm text-muted-foreground">{plan.desc}</p>
            <div className="mt-5 flex items-baseline gap-1">
              <span className="font-heading text-4xl font-extrabold tracking-tight">
                {plan.price}
              </span>
              <span className="text-sm text-muted-foreground">
                {plan.period}
              </span>
            </div>

            <a
              href="#denunciar"
              className={
                plan.featured
                  ? "mt-6 inline-flex w-full items-center justify-center gap-2 rounded-full bg-primary px-5 py-3 text-sm font-semibold text-primary-foreground transition-transform hover:-translate-y-0.5"
                  : "mt-6 inline-flex w-full items-center justify-center gap-2 rounded-full border border-border bg-secondary px-5 py-3 text-sm font-semibold text-foreground transition-colors hover:bg-accent"
              }
            >
              {plan.cta}
            </a>

            <ul className="mt-6 space-y-3">
              {plan.features.map((f) => (
                <li key={f} className="flex items-start gap-2.5 text-sm">
                  <Check className="mt-0.5 size-4 shrink-0 text-primary" />
                  <span className="text-muted-foreground">{f}</span>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </section>
  )
}
