import SwiftUI
import SceneKit

struct NetworkView: View {
    @StateObject private var viewModel = NetworkViewModel()

    private let patovaGreen = Color(red: 0.00, green: 0.78, blue: 0.33)
    private let patovaGreenLight = Color(red: 0.27, green: 0.90, blue: 0.49)
    private let patovaDark = Color(red: 0.04, green: 0.04, blue: 0.06)
    private let patovaCard = Color(red: 0.06, green: 0.10, blue: 0.06)
    private let patovaSurface = Color(red: 0.08, green: 0.12, blue: 0.08)
    private let rankGold = Color(red: 0.94, green: 0.82, blue: 0.44)
    private let rankBlue = Color(red: 0.18, green: 0.48, blue: 0.96)
    private let dangerRed = Color(red: 0.92, green: 0.26, blue: 0.26)

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

                    // MARK: - Globo 3D
                    Globe3DView()
                        .frame(height: 280)
                        .padding(.top, -20)

                    // MARK: - Header y Badge
                    VStack(spacing: 6) {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text("RED DE CONFIANZA")
                                    .font(.system(.subheadline, design: .rounded))
                                    .fontWeight(.bold)
                                    .foregroundColor(.white)
                                    .tracking(1.5)
                                Text("Comunidad protegiendose entre todos")
                                    .font(.caption)
                                    .foregroundColor(.white.opacity(0.6))
                            }
                            Spacer()
                            HStack(spacing: 4) {
                                Circle()
                                    .fill(viewModel.isRealData ? patovaGreen : rankBlue)
                                    .frame(width: 6, height: 6)
                                Text(viewModel.isRealData ? "LIVE" : "LOCAL")
                                    .font(.system(size: 10, weight: .black, design: .rounded))
                                    .foregroundColor(viewModel.isRealData ? patovaGreen : rankBlue)
                            }
                            .padding(.horizontal, 10)
                            .padding(.vertical, 5)
                            .background((viewModel.isRealData ? patovaGreen : rankBlue).opacity(0.12))
                            .cornerRadius(8)
                        }
                    }
                    .padding(.horizontal)

                    if viewModel.isLoading && viewModel.topReported.isEmpty {
                        VStack(spacing: 12) {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: patovaGreen))
                                .scaleEffect(1.3)
                            Text("Conectando con la comunidad...")
                                .font(.footnote)
                                .foregroundColor(.white.opacity(0.5))
                        }
                        .frame(height: 200)
                    } else {

                        // MARK: - Contador de Comunidad
                        VStack(spacing: 8) {
                            Text(viewModel.totalUsers)
                                .font(.system(size: 44, weight: .black, design: .rounded))
                                .foregroundColor(patovaGreenLight)

                            Text("personas protegiendo la red hoy")
                                .font(.subheadline)
                                .foregroundColor(.white.opacity(0.6))

                            Text("\(viewModel.communityBlockedTotal) llamadas bloqueadas por la comunidad")
                                .font(.caption)
                                .foregroundColor(patovaGreen.opacity(0.7))
                                .padding(.top, 2)
                        }
                        .padding(.vertical, 8)

                        // MARK: - Metricas del dia
                        HStack(spacing: 12) {
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
                                iconColor: patovaGreen
                            )
                            NetworkStatCard(
                                title: viewModel.lastUpdate,
                                subtitle: "Ultima act.",
                                icon: "clock.fill",
                                iconColor: patovaGreenLight
                            )
                        }
                        .padding(.horizontal)

                        // MARK: - Tu Rango Comunitario
                        VStack(alignment: .leading, spacing: 16) {
                            HStack {
                                Text("TU RANGO COMUNITARIO")
                                    .font(.system(.caption, design: .rounded))
                                    .fontWeight(.bold)
                                    .foregroundColor(.white.opacity(0.4))
                                    .tracking(0.8)
                                Spacer()
                                Text(viewModel.userRank.rawValue.uppercased())
                                    .font(.system(.caption, design: .rounded))
                                    .fontWeight(.black)
                                    .foregroundColor(rankColor(for: viewModel.userRank))
                            }

                            // Insignia y progreso
                            HStack(spacing: 16) {
                                ZStack {
                                    Circle()
                                        .fill(rankColor(for: viewModel.userRank).opacity(0.12))
                                        .frame(width: 64, height: 64)

                                    Image(systemName: viewModel.userRank.icon)
                                        .font(.system(size: 28))
                                        .foregroundColor(rankColor(for: viewModel.userRank))
                                }

                                VStack(alignment: .leading, spacing: 6) {
                                    if viewModel.userRank == .leyenda {
                                        Text("Rango maximo alcanzado")
                                            .font(.subheadline)
                                            .fontWeight(.bold)
                                            .foregroundColor(rankColor(for: viewModel.userRank))
                                        Text("Sos un pilar de la comunidad Patova.")
                                            .font(.caption)
                                            .foregroundColor(.white.opacity(0.6))
                                    } else if let next = viewModel.userRank.nextRank {
                                        Text("Proximo rango: \(next.rawValue)")
                                            .font(.subheadline)
                                            .fontWeight(.bold)
                                            .foregroundColor(.white)
                                        Text("Te faltan \(viewModel.blocksToNextRank) bloqueos, \(viewModel.invitesToNextRank) invitaciones y \(viewModel.reportsToNextRank) reportes")
                                            .font(.caption)
                                            .foregroundColor(.white.opacity(0.5))
                                        ProgressView(value: viewModel.rankProgress)
                                            .tint(rankColor(for: next))
                                    }
                                }
                                Spacer()
                            }

                            // Stats detalladas
                            VStack(spacing: 10) {
                                rankStatRow(label: "Llamadas bloqueadas", value: "\(viewModel.userBlocksCount)", icon: "hand.raised.fill", color: patovaGreen)
                                Divider().background(Color.white.opacity(0.05))
                                rankStatRow(label: "Personas invitadas", value: "\(viewModel.userInvitesCount)", icon: "person.2.fill", color: rankBlue)
                                Divider().background(Color.white.opacity(0.05))
                                rankStatRow(label: "Reportes contribuidos", value: viewModel.userReportsCount, icon: "flag.fill", color: dangerRed)
                            }
                            .padding(12)
                            .background(patovaDark.opacity(0.6))
                            .cornerRadius(12)

                            // Todos los rangos
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

                        // MARK: - Boton invitar
                        Button(action: {
                            let text = "Te invito a Patova, la app que bloquea llamadas spam automaticamente. Bajatela y sumate a la comunidad: patova.serra.agency"
                            let av = UIActivityViewController(activityItems: [text], applicationActivities: nil)
                            if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                               let rootVC = windowScene.windows.first?.rootViewController {
                                rootVC.present(av, animated: true)
                                viewModel.incrementInvites()
                            }
                        }) {
                            HStack {
                                Spacer()
                                Image(systemName: "person.badge.plus")
                                    .foregroundColor(.black)
                                Text("Invitar amigos · +1 a tu rango")
                                    .font(.system(.subheadline, design: .rounded))
                                    .fontWeight(.bold)
                                    .foregroundColor(.black)
                                Spacer()
                            }
                            .padding()
                            .background(patovaGreen)
                            .cornerRadius(14)
                            .shadow(color: patovaGreen.opacity(0.3), radius: 8, x: 0, y: 4)
                        }
                        .padding(.horizontal)

                        // MARK: - Top Spammers
                        if !viewModel.topReported.isEmpty {
                            VStack(alignment: .leading, spacing: 14) {
                                Text("NUMEROS MAS REPORTADOS EN ARGENTINA")
                                    .font(.system(.caption, design: .rounded))
                                    .fontWeight(.bold)
                                    .foregroundColor(.white.opacity(0.5))
                                    .tracking(0.8)
                                    .padding(.leading, 4)

                                VStack(spacing: 0) {
                                    ForEach(Array(viewModel.topReported.enumerated()), id: \.element.id) { index, item in
                                        HStack(spacing: 12) {
                                            Text("\(index + 1).")
                                                .font(.system(.subheadline, design: .rounded))
                                                .fontWeight(.black)
                                                .foregroundColor(index < 3 ? patovaGreen : .white.opacity(0.4))
                                                .frame(width: 24, alignment: .leading)

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

                                            let isSpam = item.status == "SPAM"
                                            Text("\(item.reportCount) denuncias")
                                                .font(.system(size: 10, weight: .bold, design: .rounded))
                                                .foregroundColor(isSpam ? dangerRed : patovaGreen)
                                                .padding(.horizontal, 8)
                                                .padding(.vertical, 4)
                                                .background((isSpam ? dangerRed : patovaGreen).opacity(0.12))
                                                .cornerRadius(6)
                                        }
                                        .padding(.vertical, 12)

                                        if index < viewModel.topReported.count - 1 {
                                            Divider().background(Color.white.opacity(0.06))
                                        }
                                    }
                                }
                                .padding(.horizontal, 16)
                                .background(patovaSurface)
                                .cornerRadius(18)
                                .overlay(RoundedRectangle(cornerRadius: 18).stroke(Color.white.opacity(0.06), lineWidth: 1))
                            }
                            .padding(.horizontal)
                            .padding(.bottom, 32)
                        }
                    }
                }
            }
        }
        .preferredColorScheme(.dark)
    }

    private func rankColor(for rank: CommunityRank) -> Color {
        switch rank {
        case .centinela: return .gray
        case .guardian: return patovaGreen
        case .protector: return rankBlue
        case .leyenda: return rankGold
        }
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
}

// MARK: - Globo 3D

struct Globe3DView: UIViewRepresentable {

    func makeUIView(context: Context) -> SCNView {
        let sceneView = SCNView()
        sceneView.backgroundColor = .clear
        sceneView.isOpaque = false
        sceneView.antialiasingMode = .multisampling4X
        sceneView.allowsCameraControl = false

        let scene = SCNScene()
        sceneView.scene = scene

        // Esfera principal
        let globe = SCNSphere(radius: 1.2)
        globe.segmentCount = 72
        let globeMaterial = SCNMaterial()
        globeMaterial.diffuse.contents = UIColor(red: 0.02, green: 0.08, blue: 0.04, alpha: 1.0)
        globeMaterial.specular.contents = UIColor(red: 0.0, green: 0.78, blue: 0.33, alpha: 0.3)
        globeMaterial.shininess = 0.3
        globe.materials = [globeMaterial]
        let globeNode = SCNNode(geometry: globe)
        scene.rootNode.addChildNode(globeNode)

        // Wireframe superpuesto
        let wireframe = SCNSphere(radius: 1.22)
        wireframe.segmentCount = 36
        let wireMaterial = SCNMaterial()
        wireMaterial.diffuse.contents = UIColor.clear
        wireMaterial.isDoubleSided = true
        wireMaterial.fillMode = .lines
        wireMaterial.emission.contents = UIColor(red: 0.0, green: 0.78, blue: 0.33, alpha: 0.3)
        wireframe.materials = [wireMaterial]
        let wireNode = SCNNode(geometry: wireframe)
        scene.rootNode.addChildNode(wireNode)

        // Puntos luminosos (miembros de la comunidad)
        let dotCount = 120
        for _ in 0..<dotCount {
            let dot = SCNSphere(radius: 0.015)
            let dotMat = SCNMaterial()
            let brightness = Float.random(in: 0.4...1.0)
            dotMat.diffuse.contents = UIColor(red: 0.0, green: CGFloat(brightness * 0.78), blue: CGFloat(brightness * 0.33), alpha: 1.0)
            dotMat.emission.contents = UIColor(red: 0.0, green: CGFloat(brightness * 0.78), blue: CGFloat(brightness * 0.33), alpha: 1.0)
            dot.materials = [dotMat]

            let dotNode = SCNNode(geometry: dot)

            // Distribuir sobre la superficie de la esfera
            let theta = Float.random(in: 0...(2 * .pi))
            let phi = acos(2 * Float.random(in: 0...1) - 1)
            let r: Float = 1.24
            dotNode.position = SCNVector3(
                r * sin(phi) * cos(theta),
                r * sin(phi) * sin(theta),
                r * cos(phi)
            )
            scene.rootNode.addChildNode(dotNode)
        }

        // Anillos orbitales
        for i in 0..<3 {
            let ringRadius: CGFloat = 1.5 + CGFloat(i) * 0.2
            let torus = SCNTorus(ringRadius: ringRadius, pipeRadius: 0.003)
            let torusMat = SCNMaterial()
            torusMat.diffuse.contents = UIColor(red: 0.0, green: 0.78, blue: 0.33, alpha: 0.15 - CGFloat(i) * 0.04)
            torusMat.emission.contents = UIColor(red: 0.0, green: 0.78, blue: 0.33, alpha: 0.15 - CGFloat(i) * 0.04)
            torus.materials = [torusMat]
            let ringNode = SCNNode(geometry: torus)
            ringNode.eulerAngles = SCNVector3(
                Float.random(in: 0...(2 * .pi)),
                Float.random(in: 0...(2 * .pi)),
                0
            )
            scene.rootNode.addChildNode(ringNode)

            // Rotacion lenta del anillo
            let rotateAction = SCNAction.rotateBy(x: 0, y: CGFloat.pi * 2, z: 0, duration: 20 + Double(i) * 5)
            let rotateForever = SCNAction.repeatForever(rotateAction)
            ringNode.runAction(rotateForever)
        }

        // Iluminacion
        let ambientLight = SCNLight()
        ambientLight.type = .ambient
        ambientLight.color = UIColor(white: 0.3, alpha: 1.0)
        let ambientNode = SCNNode()
        ambientNode.light = ambientLight
        scene.rootNode.addChildNode(ambientNode)

        let omniLight = SCNLight()
        omniLight.type = .omni
        omniLight.color = UIColor(red: 0.0, green: 0.78, blue: 0.33, alpha: 0.5)
        let omniNode = SCNNode()
        omniNode.light = omniLight
        omniNode.position = SCNVector3(3, 2, 3)
        scene.rootNode.addChildNode(omniNode)

        // Camara
        let cameraNode = SCNNode()
        cameraNode.camera = SCNCamera()
        cameraNode.camera?.fieldOfView = 40
        cameraNode.position = SCNVector3(0, 0, 4.5)
        scene.rootNode.addChildNode(cameraNode)

        // Rotacion del globo y wireframe
        let spin = SCNAction.rotateBy(x: 0, y: CGFloat.pi * 2, z: 0.05, duration: 30)
        let spinForever = SCNAction.repeatForever(spin)
        globeNode.runAction(spinForever)
        wireNode.runAction(spinForever.copy() as! SCNAction)

        return sceneView
    }

    func updateUIView(_ uiView: SCNView, context: Context) {}
}

// MARK: - Tarjeta de Estadistica

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
        .background(Color(red: 0.06, green: 0.10, blue: 0.06))
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
