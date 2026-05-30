import SwiftUI

struct SuccessCelebrationView: View {
    let onDismiss: () -> Void
    
    // Paleta Premium
    private let navy950 = Color(red: 0.04, green: 0.04, blue: 0.06)
    private let navy900 = Color(red: 0.05, green: 0.05, blue: 0.08)
    private let goldYellow = Color(red: 0.94, green: 0.82, blue: 0.44)
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
            // Fondo oscuro
            navy950.ignoresSafeArea()
            
            VStack(spacing: 28) {
                Spacer()
                
                // Icono de Blindaje Dorado Brillante
                ZStack {
                    Circle()
                        .fill(goldYellow.opacity(0.12))
                        .frame(width: 130, height: 130)
                        .blur(radius: 20)
                    
                    // Contenedor Dorado
                    RoundedRectangle(cornerRadius: 24)
                        .fill(goldMetallic)
                        .frame(width: 96, height: 96)
                        .overlay(
                            RoundedRectangle(cornerRadius: 24)
                                .stroke(Color.white.opacity(0.2), lineWidth: 1.5)
                        )
                        .shadow(color: goldYellow.opacity(0.4), radius: 12)
                    
                    // Escudo Interno Negro
                    RoundedRectangle(cornerRadius: 22)
                        .fill(navy900)
                        .frame(width: 90, height: 90)
                        .overlay(
                            ZStack {
                                Image(systemName: "shield.fill")
                                    .font(.system(size: 40))
                                    .foregroundColor(goldYellow)
                                
                                Image(systemName: "checkmark")
                                    .font(.system(size: 16, weight: .black))
                                    .foregroundColor(navy900)
                                    .offset(y: -1)
                            }
                        )
                }
                
                VStack(spacing: 12) {
                    Text("¡Ya sos Patova Premium!")
                        .font(.system(.title2, design: .rounded))
                        .fontWeight(.black)
                        .foregroundColor(goldYellow)
                        .tracking(0.5)
                        .multilineTextAlignment(.center)
                    
                    Text("¡Felicitaciones! Tu protección activa está al 100%. Patova va a filtrar y rechazar de forma automática y silenciosa el spam por vos para que vuelvas a tener paz mental.")
                        .font(.subheadline)
                        .foregroundColor(.white.opacity(0.7))
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)
                        .padding(.horizontal, 24)
                }
                
                Spacer()
                
                Button(action: onDismiss) {
                    HStack {
                        Spacer()
                        Text("¡Excelente!")
                            .font(.system(.subheadline, design: .rounded))
                            .fontWeight(.black)
                            .foregroundColor(navy950)
                        Spacer()
                    }
                    .padding()
                    .background(goldMetallic)
                    .cornerRadius(14)
                    .shadow(color: goldYellow.opacity(0.3), radius: 8, x: 0, y: 4)
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 20)
            }
        }
        .preferredColorScheme(.dark)
    }
}

struct SuccessCelebrationView_Previews: PreviewProvider {
    static var previews: some View {
        SuccessCelebrationView(onDismiss: {})
    }
}
