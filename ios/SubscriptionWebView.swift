import SwiftUI
import WebKit

/// Vista del WebView de Suscripción de Mercado Pago en SwiftUI para Patova.
/// Carga dinámicamente la preferencia de suscripción desde nuestro backend,
/// abre el checkout oficial de Mercado Pago, e intercepta de forma segura el Deep Link
/// 'patova://checkout/success' para cerrar la vista y activar el Premium del usuario en tiempo récord.
struct SubscriptionWebView: View {
    
    @Binding var isPremium: Bool
    @Environment(\.dismiss) var dismiss
    var prefetchedURL: URL? = nil
    
    @State private var checkoutURL: URL? = nil
    @State private var isLoading = true
    @State private var errorMessage: String? = nil
    
    var body: some View {
        NavigationView {
            ZStack {
                // Fondo Oscuro
                Color(red: 0.05, green: 0.05, blue: 0.08)
                    .ignoresSafeArea()
                
                if let url = checkoutURL {
                    // Cargar WebView
                    SafariWebViewRepresentable(url: url, isPremium: $isPremium, dismissAction: {
                        dismiss()
                    })
                    .ignoresSafeArea(.all, edges: .bottom)
                } else if let error = errorMessage {
                    // Vista de Error
                    VStack(spacing: 16) {
                        Image(systemName: "exclamationmark.octagon.fill")
                            .font(.system(size: 64))
                            .foregroundColor(.red)
                        
                        Text("Algo salió mal")
                            .font(.title2)
                            .fontWeight(.bold)
                            .foregroundColor(.white)
                        
                        Text(error)
                            .font(.subheadline)
                            .foregroundColor(.white.opacity(0.6))
                            .multilineTextAlignment(.center)
                            .padding(.horizontal)
                        
                        Button(action: {
                            fetchSubscriptionPreference()
                        }) {
                            Text("Reintentar")
                                .fontWeight(.semibold)
                                .foregroundColor(.black)
                                .padding()
                                .background(Color.teal)
                                .cornerRadius(10)
                        }
                    }
                } else if isLoading {
                    // Spinner de Carga
                    VStack(spacing: 16) {
                        ProgressView()
                            .progressViewStyle(CircularProgressViewStyle(tint: .teal))
                            .scaleEffect(1.5)
                        
                        Text("Generando enlace seguro de Mercado Pago...")
                            .font(.caption)
                            .foregroundColor(.white.opacity(0.6))
                    }
                }
            }
            .navigationTitle("Suscribirse a Patova")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Cancelar") {
                        dismiss()
                    }
                    .foregroundColor(.teal)
                }
            }
        }
        .preferredColorScheme(.dark)
        .onAppear {
            if let prefetched = prefetchedURL {
                self.checkoutURL = prefetched
                self.isLoading = false
            } else {
                fetchSubscriptionPreference()
            }
        }
    }
    
    // MARK: - Generar Preferencia desde Backend
    
    private func fetchSubscriptionPreference() {
        self.isLoading = true
        self.errorMessage = nil
        
        let apiURL = URL(string: AppConfig.apiBaseURL + "/payments/create-preference")!
        var request = URLRequest(url: apiURL)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(AppConfig.apiKey, forHTTPHeaderField: "X-Patova-Key")
        
        let payload: [String: Any] = [
            "plan_id": "premium_monthly",
            "email": "usuario@test.com", // En producción se toma del perfil del usuario
            "user_id": AppConfig.defaultUserId
        ]
        
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: payload)
        } catch {
            self.errorMessage = "Error al codificar payload: \(error.localizedDescription)"
            self.isLoading = false
            return
        }
        
        URLSession.shared.dataTask(with: request) { data, response, error in
            DispatchQueue.main.async {
                if let error = error {
                    self.errorMessage = "Fallo de conexión: \(error.localizedDescription)"
                    self.isLoading = false
                    return
                }
                
                guard let data = data else {
                    self.errorMessage = "No se recibieron datos del servidor"
                    self.isLoading = false
                    return
                }
                
                do {
                    if let json = try JSONSerialization.jsonObject(with: data) as? [String: Any],
                       let checkoutUrlStr = json["init_point"] as? String,
                       let url = URL(string: checkoutUrlStr) {
                        self.checkoutURL = url
                    } else {
                        self.errorMessage = "Respuesta de servidor inválida"
                    }
                } catch {
                    self.errorMessage = "Error de parseo: \(error.localizedDescription)"
                }
                self.isLoading = false
            }
        }.resume()
    }
}

// MARK: - UIViewRepresentable para WKWebView

struct SafariWebViewRepresentable: UIViewRepresentable {
    
    let url: URL
    @Binding var isPremium: Bool
    let dismissAction: () -> Void
    
    func makeUIView(context: Context) -> WKWebView {
        let webView = WKWebView()
        webView.navigationDelegate = context.coordinator
        webView.backgroundColor = .clear
        webView.isOpaque = false
        
        let request = URLRequest(url: url)
        webView.load(request)
        return webView
    }
    
    func updateUIView(_ uiView: WKWebView, context: Context) {}
    
    func makeCoordinator() -> Coordinator {
        Coordinator(self)
    }
    
    class Coordinator: NSObject, WKNavigationDelegate {
        var parent: SafariWebViewRepresentable
        
        init(_ parent: SafariWebViewRepresentable) {
            self.parent = parent
        }
        
        func webView(_ webView: WKWebView, decidePolicyFor navigationAction: WKNavigationAction, decisionHandler: @escaping (WKNavigationActionPolicy) -> Void) {
            if let url = navigationAction.request.url {
                print("🌐 WebView navegando a: \(url.absoluteString)")
                
                // Interceptar el esquema de Deep Link de Patova
                if url.scheme == "patova" {
                    decisionHandler(.cancel) // Cancelar la navegación normal en el navegador
                    
                    if url.host == "checkout" {
                        if url.path == "/success" {
                            print("🎉 Pago Exitoso! Activando Patova Premium...")
                            parent.isPremium = true
                            
                            // Consultar /subscriptions/me para asegurar actualización en base local de SQLite
                            Task {
                                do {
                                    try await PatovaSyncClient.shared.performSync(userId: AppConfig.defaultUserId)
                                } catch {
                                    print("Error de sync post-pago: \(error)")
                                }
                                DispatchQueue.main.async {
                                    self.parent.dismissAction()
                                }
                            }
                        } else {
                            // En caso de fracaso o pendiente, cerrar el WebView
                            parent.dismissAction()
                        }
                    }
                    return
                }
            }
            decisionHandler(.allow)
        }
    }
}
