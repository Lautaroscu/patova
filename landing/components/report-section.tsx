"use client"

import { useState, useEffect } from "react"
import { PhoneOff, Send, CheckCircle2, Loader2, AlertTriangle } from "lucide-react"

type ReportType = "SPAM_CALL" | "SCAM" | "ROBOCALL" | "OTHER"

const REASONS: { label: string; value: ReportType }[] = [
  { label: "Telemarketing", value: "SPAM_CALL" },
  { label: "Estafa", value: "SCAM" },
  { label: "Robocall", value: "ROBOCALL" },
  { label: "Cobranza", value: "SCAM" },
  { label: "Otro", value: "OTHER" },
]

function getBackendUrl(): string {
  if (typeof window === "undefined") return "https://patova-api-253350661454.southamerica-east1.run.app"
  const host = window.location.hostname
  if (host === "localhost" || host === "127.0.0.1") {
    return "http://127.0.0.1:8000"
  }
  return "https://patova-api-253350661454.southamerica-east1.run.app"
}

function getDeviceId(): string {
  const key = "patova_public_device_id"
  const existing = localStorage.getItem(key)
  if (existing) return existing
  let id: string
  try {
    id = "web-client-" + self.crypto.randomUUID()
  } catch {
    id = "web-client-" + Math.random().toString(36).substring(2) + Date.now().toString(36)
  }
  localStorage.setItem(key, id)
  return id
}

interface ApiSuccess {
  number_e164: string
  new_spam_score: number
}

export function ReportSection() {
  const [phone, setPhone] = useState("")
  const [reason, setReason] = useState<ReportType>(REASONS[0].value)
  const [sent, setSent] = useState(false)
  const [sending, setSending] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [lastReport, setLastReport] = useState<ApiSuccess | null>(null)
  const [deviceId, setDeviceId] = useState("")

  useEffect(() => {
    setDeviceId(getDeviceId())
  }, [])

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    if (!phone.trim()) return

    setSending(true)
    setError(null)

    try {
      const response = await fetch(`${getBackendUrl()}/v1/report/public`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          number: phone.trim(),
          device_id: deviceId || getDeviceId(),
          report_type: reason,
          description: null,
        }),
      })

      const data = await response.json()

      if (!response.ok) {
        const detail = data.detail || "Error al enviar la denuncia."
        throw new Error(detail)
      }

      setLastReport(data)
      setSent(true)
    } catch (err) {
      const message = err instanceof Error ? err.message : "Error de conexión."
      setError(message)
    } finally {
      setSending(false)
    }
  }

  function handleReset() {
    setSent(false)
    setError(null)
    setPhone("")
    setLastReport(null)
  }

  const selectedLabel = REASONS.find((r) => r.value === reason)?.label ?? REASONS[0].label

  const digits = phone.replace(/\D/g, "")
  const digitsOk = digits.length === 10
  const digitsWarn = digits.length > 0 && digits.length < 10
  const digitsOver = digits.length > 10

  return (
    <section
      id="denunciar"
      className="scroll-mt-20 border-y border-border bg-card/30"
    >
      <div className="mx-auto grid max-w-6xl items-center gap-10 px-4 py-16 sm:px-6 lg:grid-cols-2 lg:py-24 lg:px-8">
        <div>
          <span className="font-mono text-[11px] uppercase tracking-[0.2em] text-spam">
            Denunciar
          </span>
          <h2 className="mt-3 text-balance font-heading text-3xl font-extrabold tracking-tight sm:text-4xl">
            ¿Te llamó un número molesto?
          </h2>
          <p className="mt-4 max-w-md text-pretty text-lg leading-relaxed text-muted-foreground">
            Denuncialo en segundos. Tu reporte alimenta la red Patova y ayuda a
            que ese número no moleste a nadie más.
          </p>
          <ul className="mt-6 space-y-2 text-sm text-muted-foreground">
            <li className="flex items-center gap-2">
              <CheckCircle2 className="size-4 text-primary" /> Anónimo y gratis
            </li>
            <li className="flex items-center gap-2">
              <CheckCircle2 className="size-4 text-primary" /> Sin registrarte
            </li>
          </ul>
        </div>

        <div className="rounded-2xl border border-border bg-card p-6 sm:p-8">
          {sent ? (
            <div className="flex flex-col items-center py-8 text-center">
              <span className="flex size-14 items-center justify-center rounded-full bg-green-500/15 text-green-500 ring-1 ring-green-500/30">
                <CheckCircle2 className="size-7" />
              </span>
              <h3 className="mt-4 font-heading text-xl font-bold">
                ¡Gracias! Denuncia recibida
              </h3>
              <p className="mt-2 text-muted-foreground">
                Sumamos{" "}
                <span className="font-mono text-foreground">
                  {lastReport?.number_e164 ?? phone}
                </span>{" "}
                a la red.
                {lastReport?.new_spam_score !== undefined && (
                  <span className="block mt-1 font-mono text-xs">
                    Reputación actualizada:{" "}
                    <span className="font-bold text-foreground">
                      {lastReport.new_spam_score}/100
                    </span>
                  </span>
                )}
              </p>
              <button
                type="button"
                onClick={handleReset}
                className="mt-6 text-sm font-semibold text-primary hover:underline"
              >
                Denunciar otro número
              </button>
            </div>
          ) : (
            <form onSubmit={handleSubmit} className="space-y-5">
              {error && (
                <div className="flex items-start gap-3 rounded-lg border border-red-500/25 bg-red-500/8 p-3 text-sm text-red-600 dark:text-red-400">
                  <AlertTriangle className="mt-0.5 size-4 flex-shrink-0" />
                  <span>{error}</span>
                </div>
              )}

              <div>
                <label
                  htmlFor="phone"
                  className="mb-2 block text-sm font-medium"
                >
                  Número a denunciar
                </label>
                <div
                  className={
                    "flex items-center gap-2 rounded-xl border bg-background px-3 transition-colors focus-within:ring-2 focus-within:ring-ring" +
                    (digitsOk
                      ? " border-green-500/50"
                      : digitsWarn
                        ? " border-amber-500/50"
                        : " border-border")
                  }
                >
                  <PhoneOff
                    className={
                      "size-4 transition-colors" +
                      (digitsOk
                        ? " text-green-500"
                        : digitsWarn
                          ? " text-amber-500"
                          : " text-spam")
                    }
                  />
                  <input
                    id="phone"
                    type="tel"
                    inputMode="tel"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    placeholder="11 5555-0000"
                    className="w-full bg-transparent py-3 font-mono text-sm outline-none placeholder:text-muted-foreground"
                    disabled={sending}
                  />
                  {digits.length > 0 && (
                    <span
                      className={
                        "flex-shrink-0 font-mono text-xs tabular-nums" +
                        (digitsOk
                          ? " text-green-500"
                          : " text-amber-500")
                      }
                    >
                      {digits.length}/10
                    </span>
                  )}
                </div>
                {digitsWarn && (
                  <p className="mt-1.5 text-xs text-amber-600 dark:text-amber-400">
                    Faltan {10 - digits.length} dígito{10 - digits.length !== 1 ? "s" : ""}. Los números argentinos tienen 10 dígitos (código de área + número).
                  </p>
                )}
                {digitsOk && (
                  <p className="mt-1.5 text-xs text-green-600 dark:text-green-400">
                    Formato detectado ✓
                  </p>
                )}
                {digitsOver && (
                  <p className="mt-1.5 text-xs text-muted-foreground">
                    {digits.length} dígitos — se normalizará automáticamente al enviar.
                  </p>
                )}
              </div>

              <div>
                <span className="mb-2 block text-sm font-medium">Motivo</span>
                <div className="flex flex-wrap gap-2">
                  {REASONS.map((r) => (
                    <button
                      key={r.value}
                      type="button"
                      onClick={() => setReason(r.value)}
                      disabled={sending}
                      className={
                        reason === r.value
                          ? "rounded-full bg-primary px-3.5 py-1.5 text-sm font-medium text-primary-foreground"
                          : "rounded-full border border-border bg-secondary px-3.5 py-1.5 text-sm font-medium text-muted-foreground transition-colors hover:text-foreground"
                      }
                    >
                      {r.label}
                    </button>
                  ))}
                </div>
              </div>

              <button
                type="submit"
                disabled={sending || !phone.trim()}
                className="inline-flex w-full items-center justify-center gap-2 rounded-full bg-primary px-5 py-3.5 text-base font-semibold text-primary-foreground transition-transform hover:-translate-y-0.5 disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0"
              >
                {sending ? (
                  <>
                    <Loader2 className="size-4 animate-spin" />
                    Enviando...
                  </>
                ) : (
                  <>
                    <Send className="size-4" />
                    Enviar denuncia
                  </>
                )}
              </button>
            </form>
          )}
        </div>
      </div>
    </section>
  )
}