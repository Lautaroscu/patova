import Foundation
import Combine

public struct SubscriptionDetail: Codable {
    public let id: String
    public let status: String
    public let startedAt: String?
    public let expiresAt: String?
    public let provider: String
    public let renewalEnabled: Bool
    
    enum CodingKeys: String, CodingKey {
        case id
        case status
        case startedAt = "started_at"
        case expiresAt = "expires_at"
        case provider
        case renewalEnabled = "renewal_enabled"
    }
}

public struct SubscriptionMeResponse: Codable {
    public let userId: String
    public let isPremium: Bool
    public let subscription: SubscriptionDetail?
    
    enum CodingKeys: String, CodingKey {
        case userId = "user_id"
        case isPremium = "is_premium"
        case subscription
    }
}

public struct CreatePreferenceResponse: Codable {
    public let preferenceId: String
    public let initPoint: String
    public let userId: String
    
    enum CodingKeys: String, CodingKey {
        case preferenceId = "preference_id"
        case initPoint = "init_point"
        case userId = "user_id"
    }
}

@MainActor
public final class PaywallViewModel: ObservableObject {
    @Published public var isLoading = false
    @Published public var isPremium = false
    @Published public var initPointUrl: String? = nil
    @Published public var errorMessage: String? = nil
    @Published public var subscriptionStatus: String? = nil
    @Published public var expiresAtFormatted: String? = nil
    
    private let baseURL = URL(string: "https://patova-api.serra.agency/v1")!
    private let apiKey = "dev-dummy-key"
    private let userId = "usr_8341f3e48971"
    
    private let isPremiumKey = "patova_is_premium"
    private let expiresKey = "patova_premium_expires"
    private let statusKey = "patova_premium_status"
    
    public init() {
        loadCachedState()
        refreshSubscriptionStatus()
    }
    
    private func loadCachedState() {
        self.isPremium = UserDefaults.standard.bool(forKey: isPremiumKey)
        self.subscriptionStatus = UserDefaults.standard.string(forKey: statusKey)
        self.expiresAtFormatted = UserDefaults.standard.string(forKey: expiresKey)
    }
    
    public func refreshSubscriptionStatus() {
        self.isLoading = true
        self.errorMessage = nil
        
        var components = URLComponents(url: baseURL.appendingPathComponent("/subscriptions/me"), resolvingAgainstBaseURL: true)!
        components.queryItems = [URLQueryItem(name: "user_id", value: userId)]
        
        guard let url = components.url else { return }
        
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(apiKey, forHTTPHeaderField: "X-Patova-Key")
        
        let requestToSend = request
        Task {
            do {
                let (data, response) = try await URLSession.shared.data(for: requestToSend)
                
                guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                    throw NSError(domain: "PatovaPaywall", code: -1, userInfo: [NSLocalizedDescriptionKey: "Error revalidando suscripción"])
                }
                
                let result = try JSONDecoder().decode(SubscriptionMeResponse.self, from: data)
                
                // Cachear localmente
                UserDefaults.standard.set(result.isPremium, forKey: isPremiumKey)
                self.isPremium = result.isPremium
                
                if let sub = result.subscription {
                    UserDefaults.standard.set(sub.status, forKey: statusKey)
                    self.subscriptionStatus = sub.status
                    
                    if let expires = sub.expiresAt {
                        let formatted = formatISODate(expires)
                        UserDefaults.standard.set(formatted, forKey: expiresKey)
                        self.expiresAtFormatted = formatted
                    } else {
                        UserDefaults.standard.removeObject(forKey: expiresKey)
                        self.expiresAtFormatted = nil
                    }
                } else {
                    UserDefaults.standard.removeObject(forKey: statusKey)
                    UserDefaults.standard.removeObject(forKey: expiresKey)
                    self.subscriptionStatus = nil
                    self.expiresAtFormatted = nil
                }
                self.isLoading = false
            } catch {
                print("❌ Patova: Error al revalidar suscripción: \(error.localizedDescription)")
                // Mantener estado cached en caso de error de red
                loadCachedState()
                self.isLoading = false
            }
        }
    }
    
    public func activatePremium(planId: String) {
        self.isLoading = true
        self.errorMessage = nil
        self.initPointUrl = nil
        
        let url = baseURL.appendingPathComponent("/payments/create-preference")
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(apiKey, forHTTPHeaderField: "X-Patova-Key")
        
        // Cero Fricción de Correo (UX de Un Toque)
        // Generar un correo electrónico técnico único e invisible de fondo
        let email = "user-\(userId)@patova.com"
        
        let payload: [String: Any] = [
            "plan_id": planId,
            "email": email,
            "user_id": userId
        ]
        
        do {
            request.httpBody = try JSONSerialization.data(withJSONObject: payload)
        } catch {
            self.errorMessage = "Error al iniciar pago: \(error.localizedDescription)"
            self.isLoading = false
            return
        }
        
        let requestToSend = request
        Task {
            do {
                let (data, response) = try await URLSession.shared.data(for: requestToSend)
                
                guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                    throw NSError(domain: "PatovaPaywall", code: -1, userInfo: [NSLocalizedDescriptionKey: "Error de servidor al crear preferencia"])
                }
                
                let result = try JSONDecoder().decode(CreatePreferenceResponse.self, from: data)
                self.initPointUrl = result.initPoint
                self.isLoading = false
            } catch {
                self.errorMessage = "Fallo de conexión: \(error.localizedDescription)"
                self.isLoading = false
            }
        }
    }
    
    public func onCheckoutComplete() {
        self.initPointUrl = nil
        refreshSubscriptionStatus()
    }
    
    private func formatISODate(_ isoString: String) -> String {
        // Formatea "2026-06-30T18:04:31Z" a "30/06/2026"
        let cleanStr = isoString.prefix(19) // Quedarnos con yyyy-MM-dd'T'HH:mm:ss
        let fmt = DateFormatter()
        fmt.dateFormat = "yyyy-MM-dd'T'HH:mm:ss"
        fmt.locale = Locale(identifier: "en_US_POSIX")
        fmt.timeZone = TimeZone(secondsFromGMT: 0)
        
        if let date = fmt.date(from: String(cleanStr)) {
            let outFmt = DateFormatter()
            outFmt.dateFormat = "dd/MM/yyyy"
            return outFmt.string(from: date)
        }
        return isoString
    }
}
