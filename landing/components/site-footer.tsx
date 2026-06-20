import Image from "next/image"

const groups = [
  {
    title: "Producto",
    links: ["Cómo funciona", "Protección", "Planes", "Denunciar"],
  },
  {
    title: "Empresa",
    links: ["Sobre Patova", "Privacidad", "Términos", "Contacto"],
  },
  {
    title: "Soporte",
    links: ["Centro de ayuda", "Estado del servicio", "Comunidad"],
  },
]

export function SiteFooter() {
  return (
    <footer className="border-t border-border">
      <div className="mx-auto max-w-6xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="grid gap-10 md:grid-cols-[1.5fr_1fr_1fr_1fr]">
          <div>
            <div className="flex items-center gap-2.5">
              <span className="flex size-9 items-center justify-center rounded-lg bg-primary/15 ring-1 ring-primary/30">
                <Image
                  src="/patova-mascot.png"
                  alt="Patova"
                  width={28}
                  height={28}
                  className="size-7 object-contain"
                />
              </span>
              <span className="font-heading text-lg font-extrabold">
                Patova
              </span>
            </div>
            <p className="mt-4 max-w-xs text-sm leading-relaxed text-muted-foreground">
              El patova de tu teléfono. Bloqueá spam, telemarketing y estafas en
              Argentina.
            </p>
          </div>

          {groups.map((group) => (
            <div key={group.title}>
              <h3 className="font-mono text-[11px] uppercase tracking-[0.18em] text-muted-foreground">
                {group.title}
              </h3>
              <ul className="mt-4 space-y-2.5">
                {group.links.map((link) => (
                  <li key={link}>
                    <a
                      href="#"
                      className="text-sm text-foreground/80 transition-colors hover:text-primary"
                    >
                      {link}
                    </a>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>

        <div className="mt-10 flex flex-col items-start justify-between gap-3 border-t border-border pt-6 text-sm text-muted-foreground sm:flex-row sm:items-center">
          <p>© {new Date().getFullYear()} Patova. Hecho en Argentina.</p>
          <p className="font-mono text-xs uppercase tracking-[0.16em]">
            Protección activa · v2.0
          </p>
        </div>
      </div>
    </footer>
  )
}
