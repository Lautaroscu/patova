import SwiftUI

struct NetworkView: View {
    @StateObject private var viewModel = NetworkViewModel()
    
    // Paleta de colores Premium de Patova
    private let navy900 = Color(red: 0.05, green: 0.05, blue: 0.08)
    private let navy850 = Color(red: 0.08, green: 0.08, blue: 0.12)
    private let navy800 = Color(red: 0.11, green: 0.12, blue: 0.18)
    private let premiumBlue = Color(red: 0.18, green: 0.48, blue: 0.96)
    private let safeGreen = Color(red: 0.20, green: 0.70, blue: 0.40)
    private let dangerRed = Color(red: 0.92, green: 0.26, blue: 0.26)
    private let warningAmber = Color(red: 0.98, green: 0.65, blue: 0.12)
    private let goldYellow = Color(red: 0.94, green: 0.82, blue: 0.44)
    
    var body: some View {
        ZStack {
            // Fondo Espacial Oscuro Gradiente
            LinearGradient(
                gradient: Gradient(colors: [navy900, Color(red: 0.08, green: 0.08, blue: 0.14)]),
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea()
            
            ScrollView(showsIndicators: false) {
                VStack(spacing: 24) {
                    
                    // MARK: - Barra Superior (Header)
                    VStack(alignment: .leading, spacing: 6) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("RED DE CONFIANZA")
                                    .font(.system(.subheadline, design: .rounded))
                                    .fontWeight(.bold)
                                    .foregroundColor(.white)
                                    .tracking(1.5)
                                
                                Text("Protección comunitaria en tiempo real")
                                    .font(.caption)
                                    .foregroundColor(.white.opacity(0.6))
                            }
                            
                            Spacer()
                            
                            // Badge LIVE vs LOCAL
                            HStack(spacing: 4) {
                                Circle()
                                    .fill(viewModel.isRealData ? safeGreen : premiumBlue)
                                    .frame(width: 6, height: 6)
                                    .shadow(color: (viewModel.isRealData ? safeGreen : premiumBlue).opacity(0.5), radius: 3)
                                
                                Text(viewModel.isRealData ? "LIVE" : "LOCAL")
                                    .font(.system(size: 10, weight: .black, design: .rounded))
                                    .foregroundColor(viewModel.isRealData ? safeGreen : premiumBlue)
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background((viewModel.isRealData ? safeGreen : premiumBlue).opacity(0.12))
                            .cornerRadius(8)
                            .overlay(
                                RoundedRectangle(cornerRadius: 8)
                                    .stroke((viewModel.isRealData ? safeGreen : premiumBlue).opacity(0.2), lineWidth: 1)
                            )
                        }
                    }
                    .padding(.horizontal)
                    .padding(.top, 12)
                    
                    if viewModel.isLoading && viewModel.topReported.isEmpty {
                        // Spinner de carga central
                        VStack(spacing: 12) {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: premiumBlue))
                                .scaleEffect(1.3)
                            Text("Sincronizando red...")
                                .font(.footnote)
                                .foregroundColor(.white.opacity(0.5))
                        }
                        .frame(height: 300)
                    } else {
                        // MARK: - Aura y Contador Central
                        VStack(spacing: 12) {
                            ZStack {
                                // Brillo neón difuminado
                                Circle()
                                    .fill(premiumBlue.opacity(0.12))
                                    .frame(width: 140, height: 140)
                                    .blur(radius: 20)
                                
                                // Círculo de Vidrio Esmerilado (Glassmorphism)
                                Circle()
                                    .fill(Color.white.opacity(0.03))
                                    .frame(width: 120, height: 120)
                                    .overlay(
                                        Circle()
                                            .stroke(
                                                LinearGradient(
                                                    colors: [premiumBlue.opacity(0.5), Color.white.opacity(0.05)],
                                                    startPoint: .topLeading,
                                                    endPoint: .bottomTrailing
                                                ),
                                                lineWidth: 2
                                            )
                                    )
                                
                                Image(systemName: "globe.americas.fill")
                                    .font(.system(size: 48))
                                    .foregroundColor(premiumBlue)
                                    .shadow(color: premiumBlue.opacity(0.4), radius: 8)
                            }
                            
                            Text(viewModel.totalUsers)
                                .font(.system(size: 40, weight: .black, design: .rounded))
                                .foregroundColor(.white)
                            
                            Text("usuarios protegiendo la red hoy")
                                .font(.subheadline)
                                .foregroundColor(.white.opacity(0.6))
                        }
                        .padding(.vertical, 10)
                        
                        // MARK: - Grilla de Métricas del Día
                        HStack(spacing: 12) {
                            NetworkStatCard(
                                title: viewModel.lastUpdate,
                                subtitle: "Última act.",
                                icon: "clock.fill",
                                iconColor: safeGreen
                            )
                            
                            NetworkStatCard(
                                title: viewModel.totalReports,
                                subtitle: "Reportes",
                                icon: "exclamationmark.bubble.fill",
                                iconColor: dangerRed
                            )
                            
                            NetworkStatCard(
                                title: viewModel.newToday,
                                subtitle: "Bloqueos hoy",
                                icon: "hand.raised.fill",
                                iconColor: warningAmber
                            )
                        }
                        .padding(.horizontal)
                        
                        // MARK: - Tarjeta de Contribución Personal
                        VStack(alignment: .leading, spacing: 16) {
                            Text("Tu contribución")
                                .font(.system(.headline, design: .rounded))
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                            
                            VStack(spacing: 12) {
                                HStack {
                                    Text("Reportes validados")
                                        .font(.subheadline)
                                        .foregroundColor(.white.opacity(0.7))
                                    Spacer()
                                    Text(viewModel.userReportsCount)
                                        .font(.system(.subheadline, design: .rounded))
                                        .fontWeight(.bold)
                                        .foregroundColor(premiumBlue)
                                }
                                
                                Divider()
                                    .background(Color.white.opacity(0.08))
                                
                                HStack {
                                    Text("Rango comunitario")
                                        .font(.subheadline)
                                        .foregroundColor(.white.opacity(0.7))
                                    Spacer()
                                    Text(viewModel.userRank)
                                        .font(.system(.subheadline, design: .rounded))
                                        .fontWeight(.bold)
                                        .foregroundColor(warningAmber)
                                }
                            }
                            
                            Button(action: {
                                // Compartir la app
                                let text = "¡Estoy usando Patova para bloquear llamadas spam y estafas en mi celular! Bajátela en patova.serra.agency"
                                let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
                                if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                                   let rootVC = windowScene.windows.first?.rootViewController {
                                    rootVC.present(av, animated: true, completion: nil)
                                }
                            }) {
                                HStack {
                                    Spacer()
                                    Text("Invitar a la comunidad")
                                        .font(.system(.subheadline, design: .rounded))
                                        .fontWeight(.bold)
                                        .foregroundColor(.white)
                                    Image(systemName: "square.and.arrow.up")
                                        .foregroundColor(.white)
                                    Spacer()
                                }
                                .padding()
                                .background(premiumBlue)
                                .cornerRadius(12)
                                .shadow(color: premiumBlue.opacity(0.3), radius: 8, x: 0, y: 4)
                            }
                        }
                        .padding()
                        .background(navy850)
                        .cornerRadius(18)
                        .overlay(
                            RoundedRectangle(cornerRadius: 18)
                                .stroke(Color.white.opacity(0.06), lineWidth: 1)
                        )
                        .padding(.horizontal)
                        
                        // MARK: - Top 10 Spammers en Argentina
                        if !viewModel.topReported.isEmpty {
                            VStack(alignment: .leading, spacing: 14) {
                                Text("NÚMEROS MÁS REPORTADOS EN ARGENTINA")
                                    .font(.system(.caption, design: .rounded))
                                    .fontWeight(.bold)
                                    .foregroundColor(.white.opacity(0.5))
                                    .tracking(0.8)
                                    .padding(.leading, 4)
                                
                                VStack(spacing: 0) {
                                    ForEach(Array(viewModel.topReported.enumerated()), id: \.element.id) { index, item in
                                        HStack(spacing: 12) {
                                            // Posición
                                            Text("\(index + 1).")
                                                .font(.system(.subheadline, design: .rounded))
                                                .fontWeight(.black)
                                                .foregroundColor(goldYellow)
                                                .frame(width: 24, alignment: .leading)
                                            
                                            // Número y Score
                                            VStack(alignment: .leading, spacing: 2) {
                                                Text(item.numberE164Masked)
                                                    .font(.system(.subheadline, design: .rounded))
                                                    .fontWeight(.bold)
                                                    .foregroundColor(.white)
                                                Text("Score de Spam: \(item.spamScore)%")
                                                    .font(.system(.caption2, design: .rounded))
                                                    .foregroundColor(.white.opacity(0.5))
                                            }
                                            
                                            Spacer()
                                            
                                            // Badge de Reportes
                                            let isSpam = item.status == "SPAM"
                                            Text("\(item.reportCount) denuncias")
                                                .font(.system(size: 10, weight: .bold, design: .rounded))
                                                .foregroundColor(isSpam ? dangerRed : warningAmber)
                                                .padding(.horizontal, 8)
                                                .padding(.vertical, 4)
                                                .background((isSpam ? dangerRed : warningAmber).opacity(0.12))
                                                .cornerRadius(6)
                                                .overlay(
                                                    RoundedRectangle(cornerRadius: 6)
                                                        .stroke((isSpam ? dangerRed : warningAmber).opacity(0.2), lineWidth: 1)
                                                )
                                        }
                                        .padding(.vertical, 12)
                                        
                                        if index < viewModel.topReported.count - 1 {
                                            Divider()
                                                .background(Color.white.opacity(0.06))
                                        }
                                    }
                                }
                                .padding(.horizontal, 16)
                                .background(navy850)
                                .cornerRadius(18)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 18)
                                        .stroke(Color.white.opacity(0.06), lineWidth: 1)
                                )
                            }
                            .padding(.horizontal)
                            .padding(.bottom, 24)
                        }
                    }
                }
            }
        }
        .preferredColorScheme(.dark)
    }
}

// MARK: - Tarjeta de Estadística Individual
struct NetworkStatCard: View {
    let title: String
    let subtitle: String
    let icon: String
    let iconColor: Color
    
    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title3)
                .foregroundColor(iconColor)
                .padding(8)
                .background(iconColor.opacity(0.1))
                .clipShape(Circle())
            
            Text(title)
                .font(.system(.headline, design: .rounded))
                .fontWeight(.bold)
                .foregroundColor(.white)
            
            Text(subtitle)
                .font(.system(size: 10))
                .foregroundColor(.white.opacity(0.5))
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .background(Color(red: 0.08, green: 0.08, blue: 0.12))
        .cornerRadius(16)
        .overlay(
            RoundedRectangle(cornerRadius: 16)
                .stroke(Color.white.opacity(0.05), lineWidth: 1)
        )
    }
}

struct NetworkView_Previews: PreviewProvider {
    static var previews: some View {
        NetworkView()
    }
}
