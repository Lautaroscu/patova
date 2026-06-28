import SwiftUI

struct PaywallView: View {
    @StateObject private var viewModel = PaywallViewModel()
    @State private var showSuccessCelebration = false
    @State private var isPremiumLocalForWebView = false
    @State private var showSafariSheet = false

    private let patovaGreen = Color(red: 0.00, green: 0.78, blue: 0.33)
    private let patovaGreenLight = Color(red: 0.27, green: 0.90, blue: 0.49)
    private let patovaDark = Color(red: 0.04, green: 0.04, blue: 0.06)
    private let patovaCard = Color(red: 0.06, green: 0.10, blue: 0.06)
    private let patovaSurface = Color(red: 0.08, green: 0.12, blue: 0.08)

    private let features = [
        ("shield.checkerboard", "Bloqueo silencioso inteligente", "Cortamos las llamadas spam antes de que suene tu teléfono."),
        ("person.2.badge.gearshape.fill", "Identificación de llamadas", "Etiquetamos al instante cada número entrante."),
        ("globe.americas.fill", "Base ENACOM en tiempo real", "Base de datos oficial actualizada al instante."),
        ("bolt.shield.fill", "Detección de estafas (Vishing)", "IA que detecta fraudes telefónicos antes de que caigas."),
        ("wifi.slash", "Modo Offline 7 días", "Protegido incluso sin internet."),
        ("chart.bar.fill", "Dashboard de ahorro", "Estadísticas completas de todo lo que te ahorraste."),
        ("infinity", "Reportes ilimitados", "Denunciá todos los números que quieras sin límite."),
    ]

    var body: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [patovaDark, Color(red: 0.06, green: 0.09, blue: 0.06)]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {

                    // MARK: - Hero Section
                    if viewModel.isPremium {
                        premiumActiveView
                    } else {
                        paywallContentView
                    }
                }
            }
        }
        .preferredColorScheme(.dark)
        .onAppear { viewModel.refreshSubscriptionStatus() }
        .onChange(of: viewModel.initPointUrl ?? "") { newValue in
            if !newValue.isEmpty { self.showSafariSheet = true }
        }
        .onChange(of: isPremiumLocalForWebView) { newValue in
            if newValue == true {
                self.showSuccessCelebration = true
                self.viewModel.onCheckoutComplete()
            }
        }
        .sheet(isPresented: $showSafariSheet, onDismiss: {
            viewModel.onCheckoutComplete()
        }) {
            if let initPoint = viewModel.initPointUrl, let url = URL(string: initPoint) {
                SubscriptionWebView(isPremium: $isPremiumLocalForWebView, prefetchedURL: url)
            }
        }
        .sheet(isPresented: $showSuccessCelebration) {
            SuccessCelebrationView(onDismiss: {
                self.showSuccessCelebration = false
                self.isPremiumLocalForWebView = false
                self.viewModel.refreshSubscriptionStatus()
            })
        }
    }

    // MARK: - Premium Active View

    private var premiumActiveView: some View {
        VStack(spacing: 24) {
            VStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(patovaGreen.opacity(0.15))
                        .frame(width: 90, height: 90)
                        .blur(radius: 12)

                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color.white.opacity(0.04))
                        .frame(width: 72, height: 72)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(patovaGreen.opacity(0.3), lineWidth: 1)
                        )

                    Image(systemName: "checkmark.seal.fill")
                        .font(.system(size: 32))
                        .foregroundColor(patovaGreen)
                        .shadow(color: patovaGreen.opacity(0.4), radius: 6)
                }
                .padding(.top, 20)

                Text("Patova Premium Activo")
                    .font(.system(.title3, design: .rounded))
                    .fontWeight(.black)
                    .foregroundColor(patovaGreenLight)

                Text("Tu proteccion esta al 100%. Disfruta de la paz mental.")
                    .font(.footnote)
                    .foregroundColor(.white.opacity(0.7))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            VStack(alignment: .leading, spacing: 16) {
                HStack(spacing: 12) {
                    Image(systemName: "checkmark.seal.fill")
                        .font(.title2)
                        .foregroundColor(patovaGreen)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Suscripcion activa")
                            .font(.system(.subheadline, design: .rounded))
                            .fontWeight(.bold)
                            .foregroundColor(patovaGreen)
                        Text("Estado: \(viewModel.subscriptionStatus ?? "ACTIVE")")
                            .font(.caption2)
                            .foregroundColor(.white.opacity(0.5))
                    }
                    Spacer()
                }
                if let expires = viewModel.expiresAtFormatted {
                    Divider().background(Color.white.opacity(0.08))
                    HStack {
                        Text("Validez:")
                            .font(.footnote)
                            .foregroundColor(.white.opacity(0.7))
                        Spacer()
                        Text(expires)
                            .font(.footnote)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                    }
                }
                Divider().background(Color.white.opacity(0.08))
                HStack(spacing: 6) {
                    Image(systemName: "wifi.slash")
                        .font(.caption)
                        .foregroundColor(patovaGreenLight)
                    Text("Modo offline activo · Sincroniza en menos de 7 dias")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundColor(patovaGreenLight)
                }
            }
            .padding()
            .background(patovaCard)
            .cornerRadius(18)
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(patovaGreen.opacity(0.2), lineWidth: 1))
            .padding(.horizontal)
        }
    }

    // MARK: - Paywall Content

    private var paywallContentView: some View {
        VStack(spacing: 24) {
            // Hero con icono
            VStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(patovaGreen.opacity(0.12))
                        .frame(width: 90, height: 90)
                        .blur(radius: 12)

                    RoundedRectangle(cornerRadius: 18)
                        .fill(Color.white.opacity(0.04))
                        .frame(width: 72, height: 72)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(patovaGreen.opacity(0.3), lineWidth: 1)
                        )

                    Image(systemName: "shield.checkerboard")
                        .font(.system(size: 32))
                        .foregroundColor(patovaGreen)
                        .shadow(color: patovaGreen.opacity(0.4), radius: 6)
                }
                .padding(.top, 20)

                Text("Patova Premium")
                    .font(.system(.title2, design: .rounded))
                    .fontWeight(.black)
                    .foregroundColor(.white)

                Text("Una sola suscripcion. Proteccion total. Sin letra chica.")
                    .font(.footnote)
                    .foregroundColor(.white.opacity(0.6))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)
            }

            // Lista de servicios
            VStack(alignment: .leading, spacing: 14) {
                Text("QUE INCLUYE")
                    .font(.system(.caption, design: .rounded))
                    .fontWeight(.bold)
                    .foregroundColor(.white.opacity(0.4))
                    .tracking(0.8)
                    .padding(.leading, 4)

                VStack(spacing: 0) {
                    ForEach(Array(features.enumerated()), id: \.offset) { index, feature in
                        ServiceRow(
                            icon: feature.0,
                            title: feature.1,
                            description: feature.2,
                            color: patovaGreen,
                            isLast: index == features.count - 1
                        )
                    }
                }
                .padding(12)
                .background(patovaSurface)
                .cornerRadius(18)
                .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.white.opacity(0.06), lineWidth: 1))
            }
            .padding(.horizontal)

            // Plan Mensual
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Text("MENSUAL")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(.white.opacity(0.4))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(Color.white.opacity(0.06))
                        .cornerRadius(6)
                    Spacer()
                }

                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Plan Mensual")
                            .font(.system(.headline, design: .rounded))
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                        Text("Acceso total mes a mes")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.5))
                    }
                    Spacer()
                    VStack(alignment: .trailing, spacing: 2) {
                        Text("$1.000")
                            .font(.system(.title, design: .rounded))
                            .fontWeight(.black)
                            .foregroundColor(.white)
                        Text("/mes")
                            .font(.system(size: 11))
                            .foregroundColor(.white.opacity(0.5))
                    }
                }

                Button(action: { viewModel.activatePremium(planId: "premium_monthly") }) {
                    HStack {
                        Spacer()
                        if viewModel.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .black))
                        } else {
                            Text("Suscribirme · $1.000/mes")
                                .font(.system(.subheadline, design: .rounded))
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                        }
                        Spacer()
                    }
                    .padding()
                    .background(Color.white.opacity(0.08))
                    .cornerRadius(14)
                }
                .disabled(viewModel.isLoading)
            }
            .padding()
            .background(patovaCard)
            .cornerRadius(18)
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.white.opacity(0.08), lineWidth: 1))
            .padding(.horizontal)

            // Plan Anual
            VStack(alignment: .leading, spacing: 14) {
                HStack {
                    Text("RECOMENDADO")
                        .font(.system(size: 9, weight: .black))
                        .foregroundColor(.black)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(patovaGreen)
                        .cornerRadius(6)
                    Spacer()
                    Text("PAGO UNICO ANUAL")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundColor(patovaGreenLight)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(patovaGreen.opacity(0.12))
                        .cornerRadius(6)
                }

                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Plan Anual")
                            .font(.system(.headline, design: .rounded))
                            .fontWeight(.bold)
                            .foregroundColor(patovaGreenLight)
                        Text("Acceso total por 12 meses")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.5))
                    }
                    Spacer()
                    VStack(alignment: .trailing, spacing: 2) {
                        Text("$8.000")
                            .font(.system(.title, design: .rounded))
                            .fontWeight(.black)
                            .foregroundColor(patovaGreenLight)
                        Text("$667/mes")
                            .font(.system(size: 11))
                            .foregroundColor(.white.opacity(0.5))
                    }
                }

                Text("Menos de lo que sale un cafe por semana. Proteccion todo el ano sin preocuparte por renovaciones mensuales.")
                    .font(.caption)
                    .foregroundColor(.white.opacity(0.7))

                Button(action: { viewModel.activatePremium(planId: "premium_annual") }) {
                    HStack {
                        Spacer()
                        if viewModel.isLoading {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .black))
                        } else {
                            Text("Activar Premium · $8.000/ano")
                                .font(.system(.subheadline, design: .rounded))
                                .fontWeight(.black)
                                .foregroundColor(.black)
                        }
                        Spacer()
                    }
                    .padding()
                    .background(patovaGreen)
                    .cornerRadius(14)
                    .shadow(color: patovaGreen.opacity(0.3), radius: 10, x: 0, y: 4)
                }
                .disabled(viewModel.isLoading)
            }
            .padding()
            .background(patovaCard)
            .cornerRadius(18)
            .overlay(RoundedRectangle(cornerRadius: 18).stroke(patovaGreen.opacity(0.5), lineWidth: 2))
            .padding(.horizontal)

            // Error view
            if let error = viewModel.errorMessage {
                Text(error)
                    .font(.caption)
                    .foregroundColor(patovaGreenLight)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal)
            }

            // Trust badges
            HStack(spacing: 8) {
                TrustBadgeItem(icon: "lock.shield.fill", text: "Pago seguro via MP", color: patovaGreen)
                Spacer()
                TrustBadgeItem(icon: "bolt.fill", text: "Activacion inmediata", color: patovaGreen)
                Spacer()
                TrustBadgeItem(icon: "xmark.circle.fill", text: "Cancelacion simple", color: patovaGreen)
            }
            .padding(.horizontal)
            .padding(.bottom, 32)
        }
    }
}

// MARK: - Service Row

struct ServiceRow: View {
    let icon: String
    let title: String
    let description: String
    let color: Color
    let isLast: Bool

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 20))
                    .foregroundColor(color)
                    .frame(width: 28)

                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.white)
                    Text(description)
                        .font(.system(size: 11))
                        .foregroundColor(.white.opacity(0.50))
                        .lineLimit(2)
                }
                Spacer()
            }
            .padding(.vertical, 10)

            if !isLast {
                Divider()
                    .background(Color.white.opacity(0.05))
            }
        }
    }
}

// MARK: - Trust Badge

struct TrustBadgeItem: View {
    let icon: String
    let text: String
    let color: Color

    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 10))
                .foregroundColor(color)
            Text(text)
                .font(.system(size: 9))
                .foregroundColor(.white.opacity(0.5))
        }
    }
}

struct PaywallView_Previews: PreviewProvider {
    static var previews: some View {
        PaywallView()
    }
}
