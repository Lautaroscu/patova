import Foundation
import Combine

public struct TopReportedNumber: Codable, Identifiable {
    public var id: String { numberE164Masked }
    public let numberE164Masked: String
    public let spamScore: Int
    public let reportCount: Int
    public let status: String
    
    enum CodingKeys: String, CodingKey {
        case numberE164Masked = "number_e164_masked"
        case spamScore = "spam_score"
        case reportCount = "report_count"
        case status
    }
}

public struct StatsResponse: Codable {
    public let totalNumbers: Int
    public let totalReports: Int
    public let blockedToday: Int
    public let topReported: [TopReportedNumber]
    
    enum CodingKeys: String, CodingKey {
        case totalNumbers = "total_numbers"
        case totalReports = "total_reports"
        case blockedToday = "blocked_today"
        case topReported = "top_reported"
    }
}

@MainActor
public final class NetworkViewModel: ObservableObject {
    @Published public var isLoading = true
    @Published public var isRealData = false
    @Published public var totalUsers = "14,592"
    @Published public var lastUpdate = "24m"
    @Published public var totalReports = "+8.5k"
    @Published public var newToday = "1.2k"
    @Published public var userReportsCount = "12"
    @Published public var userRank = "Centinela"
    @Published public var topReported = [TopReportedNumber]()
    @Published public var errorMessage: String? = nil
    
    private let baseURL = URL(string: "https://patova-api.serra.agency/v1")!
    private let apiKey = "dev-dummy-key"
    
    public init() {
        fetchNetworkStats()
    }
    
    public func fetchNetworkStats() {
        self.isLoading = true
        self.errorMessage = nil
        
        var request = URLRequest(url: baseURL.appendingPathComponent("/stats"))
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(apiKey, forHTTPHeaderField: "X-Patova-Key")
        
        let requestToSend = request
        Task {
            do {
                let (data, response) = try await URLSession.shared.data(for: requestToSend)
                
                guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                    throw NSError(domain: "PatovaNetwork", code: -1, userInfo: [NSLocalizedDescriptionKey: "Error en API de estadísticas"])
                }
                
                let stats = try JSONDecoder().decode(StatsResponse.self, from: data)
                
                let threshold = 15
                if stats.totalReports >= threshold {
                    self.isRealData = true
                    self.totalUsers = String(format: "%,d", stats.totalNumbers)
                    self.lastUpdate = "1m"
                    self.totalReports = String(format: "%,d", stats.totalReports)
                    self.newToday = String(format: "+%,d", stats.blockedToday)
                    self.topReported = stats.topReported
                    self.errorMessage = nil
                } else {
                    // Fallback a social proof local pero cargando la lista real si el backend tiene datos
                    self.isRealData = false
                    self.totalUsers = "14,592"
                    self.lastUpdate = "24m"
                    self.totalReports = "+8.5k"
                    self.newToday = "1.2k"
                    self.topReported = stats.topReported
                    self.errorMessage = nil
                }
                self.isLoading = false
            } catch {
                print("❌ Patova: Error cargando estadísticas: \(error.localizedDescription)")
                self.isRealData = false
                self.totalUsers = "14,592"
                self.lastUpdate = "Modo Offline"
                self.totalReports = "+8.5k"
                self.newToday = "1.2k"
                self.errorMessage = "Usando datos locales pre-cargados"
                self.isLoading = false
            }
        }
    }
}
