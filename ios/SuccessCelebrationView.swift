import SwiftUI

struct SuccessCelebrationView: View {
    let onDismiss: () -> Void

    private let patovaDark = Color(red: 0.04, green: 0.04, blue: 0.06)
    private let patovaGreen = Color(red: 0.00, green: 0.78, blue: 0.33)
    private let patovaGreenLight = Color(red: 0.27, green: 0.90, blue: 0.49)

    var body: some View {
        ZStack {
            patovaDark.ignoresSafeArea()

            VStack(spacing: 28) {
                Spacer()

                ZStack {
                    Circle()
                        .fill(patovaGreen.opacity(0.15))
                        .frame(width: 130, height: 130)
                        .blur(radius: 20)

                    RoundedRectangle(cornerRadius: 24)
                        .fill(RadialGradient(
                            colors: [patovaGreenLight, patovaGreen, Color(red: 0.0, green: 0.5, blue: 0.2)],
                            center: .topLeading,
                            startRadius: 5,
                            endRadius: 70
                        ))
                        .frame(width: 96, height: 96)
                        .overlay(
                            RoundedRectangle(cornerRadius: 24)
                                .stroke(Color.white.opacity(0.2), lineWidth: 1.5)
                        )
                        .shadow(color: patovaGreen.opacity(0.4), radius: 12)

                    RoundedRectangle(cornerRadius: 22)
                        .fill(patovaDark)
                        .frame(width: 90, height: 90)
                        .overlay(
                            ZStack {
                                Image(systemName: "shield.checkerboard")
                                    .font(.system(size: 40))
                                    .foregroundColor(patovaGreen)

                                Image(systemName: "checkmark")
                                    .font(.system(size: 16, weight: .black))
                                    .foregroundColor(patovaDark)
                                    .offset(y: -1)
                            }
                        )
                }

                VStack(spacing: 12) {
                    Text("Ya sos Patova Premium!")
                        .font(.system(.title2, design: .rounded))
                        .fontWeight(.black)
                        .foregroundColor(patovaGreenLight)
                        .tracking(0.5)
                        .multilineTextAlignment(.center)

                    Text("Felicitaciones! Tu proteccion activa esta al 100%. Patova va a filtrar y rechazar de forma automatica y silenciosa el spam para que vuelvas a tener paz mental.")
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
                        Text("Excelente!")
                            .font(.system(.subheadline, design: .rounded))
                            .fontWeight(.black)
                            .foregroundColor(.black)
                        Spacer()
                    }
                    .padding()
                    .background(patovaGreen)
                    .cornerRadius(14)
                    .shadow(color: patovaGreen.opacity(0.3), radius: 8, x: 0, y: 4)
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
