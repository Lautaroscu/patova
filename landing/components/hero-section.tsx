import Image from "next/image"
import { ShieldCheck, PhoneOff, ArrowRight, Star } from "lucide-react"
import { BlockedCounter } from "@/components/blocked-counter"

export function HeroSection() {
  return (
    <section id="top" className="relative overflow-hidden">
      {/* subtle radial glow */}
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-x-0 top-0 -z-10 h-[640px] bg-[radial-gradient(60%_60%_at_50%_0%,oklch(0.82_0.17_150/0.14),transparent_70%)]"
      />

      <div className="mx-auto grid max-w-6xl items-center gap-12 px-4 py-16 sm:px-6 lg:grid-cols-[1.05fr_0.95fr] lg:gap-8 lg:py-24 lg:px-8">
        <div className="flex flex-col items-start">
          <span className="inline-flex items-center gap-2 rounded-full border border-border bg-card/60 px-3 py-1 font-mono text-[11px] uppercase tracking-[0.18em] text-muted-foreground">
            <span className="size-1.5 rounded-full bg-primary" />
            Protección activa en Argentina
          </span>

          <h1 className="mt-6 text-balance font-heading text-4xl font-extrabold leading-[1.05] tracking-tight sm:text-5xl lg:text-6xl">
            Protegé tus llamadas.{" "}
            <span className="text-spam">Bloqueá el spam</span> de verdad.
          </h1>

          <p className="mt-5 max-w-xl text-pretty text-lg leading-relaxed text-muted-foreground">
            Patova es el patova de tu teléfono. Filtra telemarketing, estafas y
            llamadas molestas con detección local y una base colaborativa que
            mejora con cada denuncia.
          </p>

          <div className="mt-8 flex flex-col gap-3 sm:flex-row sm:items-center">
            <a
              href="#planes"
              className="inline-flex items-center justify-center gap-2 rounded-full bg-primary px-6 py-3.5 text-base font-semibold text-primary-foreground transition-transform hover:-translate-y-0.5"
            >
              <ShieldCheck className="size-5" />
              Activar protección
            </a>
            <a
              href="#denunciar"
              className="inline-flex items-center justify-center gap-2 rounded-full border border-border bg-card/40 px-6 py-3.5 text-base font-semibold text-foreground transition-colors hover:bg-card"
            >
              <PhoneOff className="size-5 text-spam" />
              Denunciar un número
            </a>
          </div>

          <div className="mt-8 flex flex-wrap items-center gap-x-6 gap-y-3 text-sm text-muted-foreground">
            <span className="flex items-center gap-1.5">
              <span className="flex">
                {Array.from({ length: 5 }).map((_, i) => (
                  <Star key={i} className="size-4 fill-primary text-primary" />
                ))}
              </span>
              4.8 en tiendas
            </span>
            <span className="hidden h-4 w-px bg-border sm:block" />
            <span>+250.000 teléfonos protegidos</span>
          </div>
        </div>

        <div className="relative">
          <div className="absolute -inset-4 -z-10 rounded-[2rem] bg-primary/5 blur-2xl" />
          <div className="grid gap-4">
            <div className="flex items-center gap-4 rounded-2xl border border-border bg-card p-5">
              <span className="flex size-14 shrink-0 items-center justify-center rounded-xl bg-primary/15 ring-1 ring-primary/30">
                <Image
                  src="/patova-mascot.png"
                  alt="Mascota Patova"
                  width={44}
                  height={44}
                  className="size-11 object-contain"
                />
              </span>
              <div>
                <p className="font-heading text-base font-bold">
                  Llamada entrante bloqueada
                </p>
                <p className="font-mono text-xs text-muted-foreground">
                  +54 11 5555-0142 · marcado como spam
                </p>
              </div>
              <span className="ml-auto inline-flex items-center gap-1.5 rounded-full bg-spam/15 px-3 py-1.5 text-xs font-semibold text-spam">
                <PhoneOff className="size-3.5" />
                Frenada
              </span>
            </div>

            <BlockedCounter />

            <a
              href="#como-funciona"
              className="group inline-flex items-center justify-between rounded-2xl border border-border bg-card p-5 transition-colors hover:bg-secondary"
            >
              <span className="font-medium">Mirá cómo trabaja el patova</span>
              <ArrowRight className="size-5 text-primary transition-transform group-hover:translate-x-1" />
            </a>
          </div>
        </div>
      </div>
    </section>
  )
}
