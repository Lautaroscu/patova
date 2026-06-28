import SwiftUI
import CallKit

/// Vista Principal de Patova para iOS en SwiftUI.
/// Diseñada con estética premium de modo oscuro, efectos de glassmorphism,
/// halos de brillo neón dinámicos, estadísticas animadas y una tarjeta instructiva
/// interactiva para guiar al usuario en la habilitación del permiso en Ajustes de iOS.
struct DashboardView: View {
    
    @State private var isShieldActive = false
    @State private var isPremium = false
    @State private var totalBlocked = 0
    @State private var identifiedNumbers = 0
    @State private var showSubscriptionSheet = false

    private let patovaGreen = Color(red: 0.00, green: 0.78, blue: 0.33)
    private let patovaGreenLight = Color(red: 0.27, green: 0.90, blue: 0.49)
    private let patovaDark = Color(red: 0.04, green: 0.04, blue: 0.06)
    private let patovaCard = Color(red: 0.06, green: 0.10, blue: 0.06)
    private let patovaSurface = Color(red: 0.08, green: 0.12, blue: 0.08)
    
    var body: some View {
        ZStack {
            LinearGradient(
                gradient: Gradient(colors: [patovaDark, Color(red: 0.06, green: 0.09, blue: 0.06)]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 28) {
                    
                    // MARK: - Barra Superior (Header)
                    HStack {
                        VStack(alignment: .leading, spacing: 4) {
                            Text("PATOVA")
                                .font(.system(.title2, design: .rounded))
                                .fontWeight(.black)
                                .foregroundColor(.white)
                                .tracking(2)
                            
                            HStack(spacing: 6) {
                                Circle()
                                    .fill(isPremium ? patovaGreenLight : Color.gray)
                                    .frame(width: 8, height: 8)
                                Text(isPremium ? "PREMIUM ✨" : "PLAN GRATUITO")
                                    .font(.caption)
                                    .fontWeight(.bold)
                                    .foregroundColor(isPremium ? patovaGreenLight : .gray)
                            }
                        }
                        
                        Spacer()
                        
                        // Botón de Perfil / Ajustes
                        Button(action: {
                            // Abrir configuraciones del dispositivo
                            if let url = URL(string: UIApplication.openSettingsURLString) {
                                UIApplication.shared.open(url)
                            }
                        }) {
                            Image(systemName: "gearshape.fill")
                                .font(.title2)
                                .foregroundColor(.white.opacity(0.8))
                                .padding(12)
                                .background(Color.white.opacity(0.06))
                                .clipShape(Circle())
                                .overlay(
                                    Circle().stroke(Color.white.opacity(0.1), lineWidth: 1)
                                )
                        }
                    }
                    .padding(.horizontal)
                    .padding(.top, 10)
                    
                    // MARK: - Escudo Central Interactivo
                    VStack(spacing: 16) {
                        ZStack {
                            Circle()
                                .fill(isShieldActive ? patovaGreen : Color.red)
                                .opacity(isShieldActive ? 0.15 : 0.08)
                                .frame(width: 220, height: 220)
                                .blur(radius: isShieldActive ? 40 : 25)
                                .scaleEffect(isShieldActive ? 1.1 : 0.95)
                                .animation(.easeInOut(duration: 2).repeatForever(autoreverses: true), value: isShieldActive)
                            
                            Circle()
                                .fill(Color.white.opacity(0.04))
                                .frame(width: 200, height: 200)
                                .overlay(
                                    Circle()
                                        .stroke(
                                            LinearGradient(
                                                gradient: Gradient(colors: [
                                                    isShieldActive ? patovaGreen : Color.red.opacity(0.6),
                                                    Color.white.opacity(0.05)
                                                ]),
                                                startPoint: .topLeading,
                                                endPoint: .bottomTrailing
                                            ),
                                            lineWidth: 3
                                        )
                                )
                                .shadow(color: (isShieldActive ? patovaGreen : Color.red).opacity(0.2), radius: 15, x: 0, y: 10)
                            
                            VStack(spacing: 8) {
                                Image(systemName: isShieldActive ? "shield.checkerboard" : "shield.slash.fill")
                                    .font(.system(size: 64, weight: .semibold, design: .rounded))
                                    .foregroundColor(isShieldActive ? patovaGreen : .red)
                                
                                Text(isShieldActive ? "ACTIVO" : "INACTIVO")
                                    .font(.system(.headline, design: .rounded))
                                    .fontWeight(.black)
                                    .foregroundColor(isShieldActive ? patovaGreen : .red)
                                    .tracking(1)
                            }
                        }
                        .onTapGesture {
                            // Simula activar el escudo o recheckear el permiso CallKit
                            checkCallKitPermission()
                        }
                        
                        Text(isShieldActive ? "Patova está protegiendo tus llamadas entrantes de spammers molestos." : "Se requiere tu permiso para comenzar a bloquear el spam.")
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.6))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal, 30)
                    }
                    .padding(.vertical, 10)
                    
                    // MARK: - Tarjeta de Tutorial (Solo si no está activo)
                    if !isShieldActive {
                        VStack(alignment: .leading, spacing: 14) {
                            HStack {
                                Image(systemName: "exclamationmark.triangle.fill")
                                    .foregroundColor(patovaGreenLight)
                                    .font(.title2)
                                Text("Activación Requerida")
                                    .font(.headline)
                                    .foregroundColor(.white)
                            }
                            
                            Text("Para que Patova pueda bloquear e identificar llamadas en tu iPhone, Apple requiere que lo habilites manualmente:")
                                .font(.footnote)
                                .foregroundColor(.white.opacity(0.7))
                            
                            VStack(alignment: .leading, spacing: 8) {
                                TutorialStepRow(number: "1", text: "Tocá el botón de abajo para ir directo a los permisos.")
                                TutorialStepRow(number: "2", text: "Activá el interruptor de Patova.")
                            }
                            .padding(.vertical, 4)
                            
                            Button(action: {
                                if let url = URL(string: "App-prefs:root=Phone&path=CallBlockingAndIdentification") {
                                    UIApplication.shared.open(url)
                                } else if let url = URL(string: UIApplication.openSettingsURLString) {
                                    UIApplication.shared.open(url)
                                }
                            }) {
                                HStack {
                                    Spacer()
                                    Text("Activar Bloqueo de Llamadas")
                                        .font(.system(.subheadline, design: .rounded))
                                        .fontWeight(.bold)
                                        .foregroundColor(.black)
                                    Image(systemName: "arrow.right.circle.fill")
                                        .foregroundColor(.black)
                                    Spacer()
                                }
                                .padding()
                                .background(patovaGreen)
                                .cornerRadius(12)
                                .shadow(color: patovaGreen.opacity(0.3), radius: 8, x: 0, y: 4)
                            }
                        }
                        .padding()
                        .background(Color.white.opacity(0.04))
                        .cornerRadius(18)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(Color.white.opacity(0.08), lineWidth: 1)
                        )
                        .padding(.horizontal)
                        .transition(.slide.combined(with: .opacity))
                    }
                    
                    // MARK: - Tarjeta de Promoción Premium (Si no es premium)
                    if !isPremium {
                        VStack(alignment: .leading, spacing: 14) {
                            HStack {
                                Text("✨")
                                    .font(.title)
                                VStack(alignment: .leading, spacing: 2) {
                                    Text("Desbloquea Protección Total")
                                        .font(.system(.headline, design: .rounded))
                                        .fontWeight(.bold)
                                        .foregroundColor(.white)
                                    Text("Consigue Patova Premium hoy mismo.")
                                        .font(.caption)
                                        .foregroundColor(.white.opacity(0.6))
                                }
                                Spacer()
                                Image(systemName: "chevron.right.circle.fill")
                                    .font(.title2)
                                    .foregroundColor(patovaGreen)
                            }
                            .padding()
                            .background(
                                LinearGradient(
                                    gradient: Gradient(colors: [patovaGreen.opacity(0.15), patovaGreenLight.opacity(0.08)]),
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                            )
                            .cornerRadius(18)
                            .overlay(
                                RoundedRectangle(cornerRadius: 18)
                                    .stroke(LinearGradient(colors: [patovaGreen.opacity(0.4), patovaGreenLight.opacity(0.2)], startPoint: .topLeading, endPoint: .bottomTrailing), lineWidth: 1)
                            )
                            .onTapGesture {
                                showSubscriptionSheet = true
                            }
                            .padding(.horizontal)
                        }
                    }
                    
                    // MARK: - Grilla de Estadísticas
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 16) {
                        StatCardView(title: "Spams Bloqueados", value: "\(totalBlocked)", icon: "hand.raised.fill", color: .red)
                        StatCardView(title: "Números Sospechosos", value: "\(identifiedNumbers)", icon: "eye.fill", color: patovaGreen)
                    }
                    .padding(.horizontal)
                    
                    // MARK: - Botón Sincronizar Manual
                    Button(action: {
                        triggerManualSync()
                    }) {
                        HStack(spacing: 8) {
                            Image(systemName: "arrow.triangle.2.circlepath")
                            Text("Sincronizar base de spam")
                                .fontWeight(.semibold)
                        }
                        .font(.footnote)
                        .foregroundColor(.white.opacity(0.7))
                        .padding(.vertical, 10)
                        .padding(.horizontal, 20)
                        .background(Color.white.opacity(0.05))
                        .cornerRadius(20)
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .stroke(Color.white.opacity(0.1), lineWidth: 1)
                        )
                    }
                    .padding(.bottom, 30)
                }
            }
        }
        .onAppear {
            checkCallKitPermission()
            updateCounts()
        }
        .sheet(isPresented: $showSubscriptionSheet) {
            PaywallView()
        }
        .preferredColorScheme(.dark)
    }
    
    // MARK: - Métodos de Control
    
    private func updateCounts() {
        self.totalBlocked = DatabaseManager.shared.getBlockingCount()
        self.identifiedNumbers = DatabaseManager.shared.getIdentificationCount()
        self.isPremium = UserDefaults.standard.bool(forKey: "patova_is_premium")
    }
    
    private func checkCallKitPermission() {
        CXCallDirectoryManager.sharedInstance.getEnabledStatusForExtension(withIdentifier: AppConfig.callKitExtensionIdentifier) { status, error in
            DispatchQueue.main.async {
                if status == .enabled {
                    self.isShieldActive = true
                } else {
                    self.isShieldActive = false
                }
            }
        }
    }
    
    private func triggerManualSync() {
        Task {
            do {
                // Sincroniza contra FastAPI en segundo plano y recarga CallKit
                try await PatovaSyncClient.shared.performSync(userId: AppConfig.defaultUserId)
                checkCallKitPermission()
                updateCounts()
            } catch {
                print("Error sincronizando manualmente: \(error)")
            }
        }
    }
}

private let dashboardGreen = Color(red: 0.00, green: 0.78, blue: 0.33)

// MARK: - Vistas de Soporte Auxiliares

struct TutorialStepRow: View {
    let number: String
    let text: String
    
    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Text(number)
                .font(.system(.caption, design: .rounded))
                .fontWeight(.bold)
                .foregroundColor(.black)
                .frame(width: 18, height: 18)
                .background(dashboardGreen)
                .clipShape(Circle())
                .padding(.top, 2)
            
            Text(text)
                .font(.footnote)
                .foregroundColor(.white.opacity(0.8))
            Spacer()
        }
    }
}

struct StatCardView: View {
    let title: String
    let value: String
    let icon: String
    let color: Color
    
    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: icon)
                    .font(.title2)
                    .foregroundColor(color)
                Spacer()
            }
            
            VStack(alignment: .leading, spacing: 4) {
                Text(value)
                    .font(.system(.title, design: .rounded))
                    .fontWeight(.black)
                    .foregroundColor(.white)
                Text(title)
                    .font(.caption2)
                    .foregroundColor(.white.opacity(0.5))
            }
        }
        .padding()
        .background(Color.white.opacity(0.04))
        .cornerRadius(18)
        .overlay(
            RoundedRectangle(cornerRadius: 18)
                .stroke(Color.white.opacity(0.08), lineWidth: 1)
        )
    }
}
