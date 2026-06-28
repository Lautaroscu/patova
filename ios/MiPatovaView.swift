import SwiftUI

struct MiPatovaView: View {
    @StateObject private var viewModel = MiPatovaViewModel()

    private let patovaGreen = Color(red: 0.00, green: 0.78, blue: 0.33)
    private let patovaGreenLight = Color(red: 0.27, green: 0.90, blue: 0.49)
    private let patovaDark = Color(red: 0.04, green: 0.04, blue: 0.06)
    private let patovaCard = Color(red: 0.06, green: 0.10, blue: 0.06)
    private let patovaSurface = Color(red: 0.08, green: 0.12, blue: 0.08)
    private let rankGold = Color(red: 0.94, green: 0.82, blue: 0.44)
    private let rankBlue = Color(red: 0.18, green: 0.48, blue: 0.96)

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
                    headerSection
                    personalInfoCard
                    systemConfigCard
                    rankCard
                    Spacer().frame(height: 30)
                }
            }
        }
        .preferredColorScheme(.dark)
        .onAppear { viewModel.loadData() }
    }

    private var headerSection: some View {
        VStack(spacing: 16) {
            ZStack {
                Circle()
                    .fill(patovaGreen.opacity(0.12))
                    .frame(width: 80, height: 80)
                    .blur(radius: 12)

                Circle()
                    .fill(Color.white.opacity(0.06))
                    .frame(width: 72, height: 72)
                    .overlay(
                        Circle().stroke(patovaGreen.opacity(0.3), lineWidth: 1)
                    )

                Image(systemName: "person.fill")
                    .font(.system(size: 32))
                    .foregroundColor(patovaGreen)
            }
            .padding(.top, 20)

            Text("MI PATOVA")
                .font(.system(.title2, design: .rounded))
                .fontWeight(.black)
                .foregroundColor(.white)
                .tracking(2)

            Text("Tu perfil y configuracion personal")
                .font(.footnote)
                .foregroundColor(.white.opacity(0.5))
        }
    }

    private var personalInfoCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            sectionHeader(title: "INFORMACION PERSONAL", icon: "person.text.rectangle.fill")

            VStack(spacing: 0) {
                profileInfoRow(label: "Usuario ID", value: AppConfig.defaultUserId, icon: "number")
                Divider().background(Color.white.opacity(0.06))
                profileInfoRow(
                    label: "Plan",
                    value: viewModel.isPremium ? "Premium" : "Gratuito",
                    icon: "star.fill",
                    valueColor: viewModel.isPremium ? patovaGreenLight : .gray
                )
                if viewModel.isPremium, !viewModel.premiumExpires.isEmpty, viewModel.premiumExpires != "-" {
                    Divider().background(Color.white.opacity(0.06))
                    profileInfoRow(label: "Vencimiento", value: viewModel.premiumExpires, icon: "calendar")
                }
                Divider().background(Color.white.opacity(0.06))
                profileInfoRow(label: "Version", value: "\(viewModel.appVersion) (\(viewModel.buildNumber))", icon: "apps.iphone")
            }
            .padding(12)
            .background(patovaSurface)
            .cornerRadius(14)
        }
        .padding(.horizontal)
    }

    private var systemConfigCard: some View {
        VStack(alignment: .leading, spacing: 14) {
            sectionHeader(title: "CONFIGURACION DEL SISTEMA", icon: "gearshape.2.fill")

            VStack(spacing: 0) {
                profileInfoRow(
                    label: "CallKit",
                    value: viewModel.callKitEnabled ? "Activo" : "Inactivo",
                    icon: viewModel.callKitEnabled ? "shield.checkerboard" : "shield.slash.fill",
                    valueColor: viewModel.callKitEnabled ? patovaGreen : .red
                )
                Divider().background(Color.white.opacity(0.06))
                profileInfoRow(label: "Bloqueos realizados", value: "\(viewModel.blockedCount)", icon: "hand.raised.fill", valueColor: patovaGreen)
                Divider().background(Color.white.opacity(0.06))
                profileInfoRow(label: "Numeros identificados", value: "\(viewModel.identifiedCount)", icon: "eye.fill", valueColor: patovaGreenLight)
                Divider().background(Color.white.opacity(0.06))
                profileInfoRow(label: "Ultima sincronizacion", value: viewModel.lastSync, icon: "arrow.triangle.2.circlepath")
            }
            .padding(12)
            .background(patovaSurface)
            .cornerRadius(14)
        }
        .padding(.horizontal)
    }

    private var rankCard: some View {
        VStack(alignment: .leading, spacing: 16) {
            sectionHeader(title: "TU RANGO COMUNITARIO", icon: "trophy.fill")

            HStack(spacing: 16) {
                ZStack {
                    Circle()
                        .fill(rankColor.opacity(0.12))
                        .frame(width: 64, height: 64)

                    Image(systemName: viewModel.userRank.icon)
                        .font(.system(size: 28))
                        .foregroundColor(rankColor)
                }

                VStack(alignment: .leading, spacing: 6) {
                    Text(viewModel.userRank.rawValue.uppercased())
                        .font(.headline)
                        .fontWeight(.black)
                        .foregroundColor(rankColor)

                    if viewModel.userRank == .leyenda {
                        Text("Rango maximo alcanzado")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.6))
                    } else if let next = viewModel.userRank.nextRank {
                        Text("Proximo rango: \(next.rawValue)")
                            .font(.subheadline)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                        Text("Te faltan \(viewModel.blocksToNext) bloqueos, \(viewModel.invitesToNext) invitaciones y \(viewModel.reportsToNext) reportes")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.5))
                        ProgressView(value: viewModel.rankProgress)
                            .tint(rankColor(for: next))
                    }
                }
                Spacer()
            }
            .padding(12)
            .background(patovaDark.opacity(0.6))
            .cornerRadius(12)

            VStack(spacing: 10) {
                rankStatRow(label: "Llamadas bloqueadas", value: "\(viewModel.userBlocks)", icon: "hand.raised.fill", color: patovaGreen)
                Divider().background(Color.white.opacity(0.05))
                rankStatRow(label: "Personas invitadas", value: "\(viewModel.userInvites)", icon: "person.2.fill", color: rankBlue)
                Divider().background(Color.white.opacity(0.05))
                rankStatRow(label: "Reportes contribuidos", value: "\(viewModel.userReports)", icon: "flag.fill", color: .red)
            }
            .padding(12)
            .background(patovaDark.opacity(0.6))
            .cornerRadius(12)

            VStack(alignment: .leading, spacing: 8) {
                Text("RANGOS DE LA COMUNIDAD")
                    .font(.system(size: 10, weight: .bold))
                    .foregroundColor(.white.opacity(0.4))
                    .tracking(0.5)

                ForEach(CommunityRank.allCases, id: \.self) { rank in
                    HStack(spacing: 10) {
                        Image(systemName: rank.icon)
                            .foregroundColor(viewModel.userRank == rank ? rankColor(for: rank) : .white.opacity(0.3))
                            .frame(width: 20)
                        Text(rank.rawValue)
                            .font(.system(size: 12, weight: viewModel.userRank == rank ? .bold : .regular))
                            .foregroundColor(viewModel.userRank == rank ? rankColor(for: rank) : .white.opacity(0.4))
                        if viewModel.userRank == rank {
                            Text("◀ VOS")
                                .font(.system(size: 9, weight: .black))
                                .foregroundColor(patovaGreenLight)
                        }
                        Spacer()
                        Text("+\(rank.blocksRequired) bloq  ·  +\(rank.invitesRequired) inv  ·  +\(rank.reportsRequired) rep")
                            .font(.system(size: 9))
                            .foregroundColor(.white.opacity(0.25))
                    }
                }
            }
        }
        .padding()
        .background(patovaCard)
        .cornerRadius(18)
        .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.white.opacity(0.06), lineWidth: 1))
        .padding(.horizontal)
    }

    private func sectionHeader(title: String, icon: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
                .font(.caption)
                .foregroundColor(patovaGreen)
            Text(title)
                .font(.system(.caption, design: .rounded))
                .fontWeight(.bold)
                .foregroundColor(.white.opacity(0.4))
                .tracking(0.8)
        }
        .padding(.leading, 4)
    }

    private func profileInfoRow(label: String, value: String, icon: String, valueColor: Color = .white) -> some View {
        HStack(spacing: 10) {
            Image(systemName: icon)
                .font(.system(size: 14))
                .foregroundColor(patovaGreen.opacity(0.7))
                .frame(width: 20)
            Text(label)
                .font(.subheadline)
                .foregroundColor(.white.opacity(0.7))
            Spacer()
            Text(value)
                .font(.system(.subheadline, design: .rounded))
                .fontWeight(.semibold)
                .foregroundColor(valueColor)
        }
        .padding(.vertical, 10)
    }

    private func rankStatRow(label: String, value: String, icon: String, color: Color) -> some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(color)
                .frame(width: 20)
            Text(label)
                .font(.subheadline)
                .foregroundColor(.white.opacity(0.7))
            Spacer()
            Text(value)
                .font(.system(.subheadline, design: .rounded))
                .fontWeight(.bold)
                .foregroundColor(color)
        }
    }

    private var rankColor: Color {
        rankColor(for: viewModel.userRank)
    }

    private func rankColor(for rank: CommunityRank) -> Color {
        switch rank {
        case .centinela: return .gray
        case .guardian: return patovaGreen
        case .protector: return rankBlue
        case .leyenda: return rankGold
        }
    }
}

struct MiPatovaView_Previews: PreviewProvider {
    static var previews: some View {
        MiPatovaView()
    }
}
