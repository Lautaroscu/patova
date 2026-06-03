import SwiftUI

struct PaywallView: View {
    @StateObject private var viewModel = PaywallViewModel()
    @State private var showSuccessCelebration = false
    @State private var isPremiumLocalForWebView = false
    @State private var showSafariSheet = false
    
    // Paletas Premium
    private let navy950 = Color(red: 0.04, green: 0.04, blue: 0.06)
    private let navy900 = Color(red: 0.05, green: 0.05, blue: 0.08)
    private let navy850 = Color(red: 0.08, green: 0.08, blue: 0.12)
    private let navy800 = Color(red: 0.11, green: 0.12, blue: 0.18)
    private let goldYellow = Color(red: 0.94, green: 0.82, blue: 0.44)
    private let premiumBlue = Color(red: 0.18, green: 0.48, blue: 0.96)
    private let safeGreen = Color(red: 0.20, green: 0.70, blue: 0.40)
    private let warningAmber = Color(red: 0.98, green: 0.65, blue: 0.12)
    private let dangerRed = Color(red: 0.92, green: 0.26, blue: 0.26)
    
    private let goldMetallic = LinearGradient(
        colors: [
            Color(red: 1.0, green: 0.91, blue: 0.65), // Champagne gold
            Color(red: 0.83, green: 0.69, blue: 0.22), // Standard gold
            Color(red: 0.65, green: 0.49, blue: 0.12)  // Dark bronze/gold
        ],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
    
    var body: some View {
        ZStack {
            // Fondo Espacial Oscuro Gradiente
            LinearGradient(
                gradient: Gradient(colors: [navy900, navy850, navy800]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    
                    // MARK: - Hero Section
                    VStack(spacing: 16) {
                        ZStack {
                            Circle()
                                .fill(goldYellow.opacity(0.12))
                                .frame(width: 90, height: 90)
                                .blur(radius: 12)
                            
                            RoundedRectangle(cornerRadius: 18)
                                .fill(Color.white.opacity(0.04))
                                .frame(width: 72, height: 72)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 18)
                                        .stroke(goldYellow.opacity(0.3), lineWidth: 1)
                                )
                            
                            Image(systemName: viewModel.isPremium ? "star.fill" : "shield.fill")
                                .font(.system(size: 32))
                                .foregroundColor(goldYellow)
                                .shadow(color: goldYellow.opacity(0.4), radius: 6)
                        }
                        .padding(.top, 20)
                        
                        Text(viewModel.isPremium ? "Patova Premium Activo" : "Recuperá la paz en tu teléfono")
                            .font(.system(.title3, design: .rounded))
                            .fontWeight(.black)
                            .foregroundColor(.white)
                            .multilineTextAlignment(.center)
                        
                        Text("Olvidate de las llamadas molestas a la hora de la siesta, de trabajar o de cenar. Patova las ataja en silencio por vos.")
                            .font(.footnote)
                            .foregroundColor(.white.opacity(0.7))
                            .multilineTextAlignment(.center)
                            .lineSpacing(4)
                            .padding(.horizontal, 32)
                    }
                    
                    // MARK: - Contenedor Dependiendo de Suscripción
                    if viewModel.isPremium {
                        // Tarjeta Premium Activa
                        VStack(alignment: .leading, spacing: 16) {
                            HStack(spacing: 12) {
                                Image(systemName: "checkmark.seal.fill")
                                    .font(.title2)
                                    .foregroundColor(safeGreen)
                                
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Tu protección activa está al 100%")
                                        .font(.system(.subheadline, design: .rounded))
                                        .fontWeight(.bold)
                                        .foregroundColor(safeGreen)
                                    
                                    Text("Estado: \(viewModel.subscriptionStatus ?? "ACTIVE")")
                                        .font(.caption2)
                                        .foregroundColor(.white.opacity(0.5))
                                }
                                Spacer()
                            }
                            
                            if let expires = viewModel.expiresAtFormatted {
                                Divider()
                                    .background(Color.white.opacity(0.08))
                                
                                HStack {
                                    Text("Validez de la suscripción:")
                                        .font(.footnote)
                                        .foregroundColor(.white.opacity(0.7))
                                    Spacer()
                                    Text(expires)
                                        .font(.footnote)
                                        .fontWeight(.bold)
                                        .foregroundColor(.white)
                                }
                            }
                            
                            Divider()
                                .background(Color.white.opacity(0.08))
                            
                            HStack(spacing: 6) {
                                Image(systemName: "wifi.slash")
                                    .font(.caption)
                                    .foregroundColor(warningAmber)
                                Text("Modo offline activo · Sincroniza en menos de 7 días")
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundColor(warningAmber)
                            }
                        }
                        .padding()
                        .background(navy800)
                        .cornerRadius(18)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(safeGreen.opacity(0.2), lineWidth: 1)
                        )
                        .padding(.horizontal)
                        .padding(.top, 10)
                        
                    } else {
                        // MARK: - Tabla Comparativa
                        VStack(alignment: .leading, spacing: 12) {
                            Text("COMPARATIVA DE FUNCIONES")
                                .font(.system(.caption, design: .rounded))
                                .fontWeight(.bold)
                                .foregroundColor(.white.opacity(0.5))
                                .tracking(0.8)
                                .padding(.leading, 4)
                            
                            VStack(spacing: 0) {
                                FeatureRow(feature: "Identificación de spam", free: "Básica", premium: "Filtro Automático + IA", isLast: false)
                                FeatureRow(feature: "Base de datos ENACOM", free: "Manual básica", premium: "Completa en tiempo real", isLast: false)
                                FeatureRow(feature: "Bloqueo de llamadas", free: "Te avisa (tenés que cortar vos)", premium: "Bloqueo silencioso inteligente", isLast: false, isFreeDenied: true)
                                FeatureRow(feature: "Detección de estafas", free: "", premium: "Filtro Avanzado (Vishing)", isLast: false)
                                FeatureRow(feature: "Reportes comunitarios", free: "Solo 3 diarios", premium: "Ilimitados", isLast: false)
                                FeatureRow(feature: "Modo Offline", free: "", premium: "Hasta 7 días sin internet", isLast: false)
                                FeatureRow(feature: "Estadísticas", free: "", premium: "Dashboard total de ahorro", isLast: true)
                             }
                            .padding(.horizontal, 12)
                            .padding(.vertical, 8)
                            .background(navy800.opacity(0.7))
                            .cornerRadius(18)
                            .overlay(
                                RoundedRectangle(cornerRadius: 18)
                                    .stroke(Color.white.opacity(0.06), lineWidth: 1)
                            )
                        }
                        .padding(.horizontal)
                        
                        // MARK: - Planes de Venta Directa (Un Toque, Cero Fricción)
                        VStack(spacing: 16) {
                            
                            // Tarjeta Plan Mensual
                            VStack(alignment: .leading, spacing: 14) {
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text("Plan Mensual")
                                            .font(.system(.headline, design: .rounded))
                                            .fontWeight(.bold)
                                            .foregroundColor(.white)
                                        Text("Sin compromiso · Cancelá cuando quieras")
                                            .font(.caption)
                                            .foregroundColor(.white.opacity(0.5))
                                    }
                                    Spacer()
                                    Text("$1.000")
                                        .font(.system(.title3, design: .rounded))
                                        .fontWeight(.bold)
                                        .foregroundColor(.white)
                                }
                                
                                Text("Menos de lo que sale un café al paso ($1.000/mes) para liberarte del spam.")
                                    .font(.caption)
                                    .foregroundColor(.white.opacity(0.7))
                                
                                Button(action: {
                                    viewModel.activatePremium(planId: "premium_monthly")
                                }) {
                                    HStack {
                                        Spacer()
                                        if viewModel.isLoading {
                                            ProgressView()
                                                .progressViewStyle(CircularProgressViewStyle(tint: goldYellow))
                                        } else {
                                            Text("Suscribirme · $1.000/mes")
                                                .font(.system(.subheadline, design: .rounded))
                                                .fontWeight(.bold)
                                                .foregroundColor(goldYellow)
                                        }
                                        Spacer()
                                    }
                                    .padding()
                                    .background(navy900)
                                    .cornerRadius(12)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 12)
                                            .stroke(goldYellow.opacity(0.4), lineWidth: 1)
                                    )
                                }
                                .disabled(viewModel.isLoading)
                            }
                            .padding()
                            .background(navy800.opacity(0.5))
                            .cornerRadius(18)
                            .overlay(
                                RoundedRectangle(cornerRadius: 18)
                                    .stroke(Color.white.opacity(0.06), lineWidth: 1)
                            )
                            
                            // Tarjeta Plan Anual (El más destacado)
                            VStack(alignment: .leading, spacing: 14) {
                                // Badges
                                HStack {
                                    Text("RECOMENDADO · EL MÁS ELEGIDO")
                                        .font(.system(size: 9, weight: .black))
                                        .foregroundColor(navy950)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(goldMetallic)
                                        .cornerRadius(6)
                                    
                                    Spacer()
                                    
                                    Text("AHORRÁ 34%")
                                        .font(.system(size: 10, weight: .bold))
                                        .foregroundColor(goldYellow)
                                        .padding(.horizontal, 8)
                                        .padding(.vertical, 4)
                                        .background(goldYellow.opacity(0.15))
                                        .cornerRadius(6)
                                }
                                
                                HStack {
                                    VStack(alignment: .leading, spacing: 4) {
                                        Text("Plan Anual")
                                            .font(.system(.headline, design: .rounded))
                                            .fontWeight(.bold)
                                            .foregroundColor(goldYellow)
                                        Text("2 meses gratis · Mejor valor")
                                            .font(.caption)
                                            .foregroundColor(.white.opacity(0.5))
                                    }
                                    Spacer()
                                    VStack(alignment: .trailing, spacing: 2) {
                                        Text("$9.600")
                                            .font(.system(.title2, design: .rounded))
                                            .fontWeight(.black)
                                            .foregroundColor(goldYellow)
                                        Text("equivale a $800/mes")
                                            .font(.system(size: 10))
                                            .foregroundColor(.white.opacity(0.5))
                                    }
                                }
                                
                                Text("Equivale a menos de lo que cuesta un alfajor por mes para tener paz mental todo el año.")
                                    .font(.caption)
                                    .foregroundColor(.white.opacity(0.7))
                                
                                Button(action: {
                                    viewModel.activatePremium(planId: "premium_annual")
                                }) {
                                    HStack {
                                        Spacer()
                                        if viewModel.isLoading {
                                            ProgressView()
                                                .progressViewStyle(CircularProgressViewStyle(tint: navy950))
                                        } else {
                                            Text("Suscribirme · $800/mes (anual)")
                                                .font(.system(.subheadline, design: .rounded))
                                                .fontWeight(.black)
                                                .foregroundColor(navy950)
                                        }
                                        Spacer()
                                    }
                                    .padding()
                                    .background(goldMetallic)
                                    .cornerRadius(12)
                                    .shadow(color: goldYellow.opacity(0.3), radius: 8, x: 0, y: 4)
                                }
                                .disabled(viewModel.isLoading)
                            }
                            .padding()
                            .background(navy800)
                            .cornerRadius(18)
                            .overlay(
                                RoundedRectangle(cornerRadius: 18)
                                    .stroke(goldMetallic, lineWidth: 2)
                            )
                        }
                        .padding(.horizontal)
                        
                        // Error view
                        if let error = viewModel.errorMessage {
                            Text(error)
                                .font(.caption)
                                .foregroundColor(warningAmber)
                                .multilineTextAlignment(.center)
                                .padding(.horizontal)
                        }
                        
                        // MARK: - Trust Badges
                        HStack(spacing: 8) {
                            TrustBadgeItem(icon: "lock.shield.fill", text: "Pago seguro vía MP")
                            Spacer()
                            TrustBadgeItem(icon: "bolt.fill", text: "Activación inmediata")
                            Spacer()
                            TrustBadgeItem(icon: "xmark.circle.fill", text: "Cancelación simple")
                        }
                        .padding(.horizontal)
                        .padding(.vertical, 16)
                    }
                }
            }
        }
        .preferredColorScheme(.dark)
        .onAppear {
            viewModel.refreshSubscriptionStatus()
        }
        .onChange(of: viewModel.initPointUrl) { newValue in
            if newValue != nil {
                self.showSafariSheet = true
            }
        }
        .onChange(of: isPremiumLocalForWebView) { newValue in
            if newValue == true {
                // Pago exitoso! Mostrar overlay
                self.showSuccessCelebration = true
                self.viewModel.onCheckoutComplete()
            }
        }
        .sheet(isPresented: &showSafariSheet, onDismiss: {
            viewModel.onCheckoutComplete()
        }) {
            Group {
                if let initPoint = viewModel.initPointUrl, let url = URL(string: initPoint) {
                    SubscriptionWebView(isPremium: $isPremiumLocalForWebView, prefetchedURL: url)
                } else {
                    EmptyView()
                }
            }
        }
        .sheet(isPresented: &showSuccessCelebration) {
            SuccessCelebrationView(onDismiss: {
                self.showSuccessCelebration = false
                self.isPremiumLocalForWebView = false
                self.viewModel.refreshSubscriptionStatus()
            })
        }
    }
}

// MARK: - Fila Comparativa Individual
struct FeatureRow: View {
    let feature: String
    let free: String
    let premium: String
    let isLast: Bool
    var isFreeDenied: Bool = false
    
    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 8) {
                // Función
                Text(feature)
                    .font(.system(size: 11, weight: .medium))
                    .foregroundColor(.white)
                    .frame(maxWidth: .infinity, alignment: .leading)
                
                // Free
                HStack(spacing: 3) {
                    if free.isEmpty {
                        Image(systemName: "xmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(.red)
                    } else {
                        Image(systemName: isFreeDenied ? "minus.circle.fill" : "checkmark")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundColor(isFreeDenied ? .orange : .white.opacity(0.6))
                        
                        Text(free)
                            .font(.system(size: 9))
                            .foregroundColor(isFreeDenied ? .orange : .white.opacity(0.6))
                    }
                }
                .frame(width: 90, alignment: .center)
                
                // Premium
                HStack(spacing: 3) {
                    Image(systemName: "checkmark")
                        .font(.system(size: 10, weight: .black))
                        .foregroundColor(Color(red: 0.94, green: 0.82, blue: 0.44))
                    
                    Text(premium)
                        .font(.system(size: 9, weight: .bold))
                        .foregroundColor(Color(red: 0.94, green: 0.82, blue: 0.44))
                }
                .frame(width: 110, alignment: .center)
            }
            .padding(.vertical, 8)
            
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
    
    var body: some View {
        HStack(spacing: 4) {
            Image(systemName: icon)
                .font(.system(size: 10))
                .foregroundColor(Color(red: 0.94, green: 0.82, blue: 0.44))
            
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
