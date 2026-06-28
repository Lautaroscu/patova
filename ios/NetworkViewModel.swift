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

public enum CommunityRank: String, CaseIterable {
    case centinela = "Centinela"
    case guardian = "Guardian"
    case protector = "Protector"
    case leyenda = "Leyenda"

    var icon: String {
        switch self {
        case .centinela: return "shield.fill"
        case .guardian: return "shield.lefthalf.filled"
        case .protector: return "shield.checkerboard"
        case .leyenda: return "star.shield.fill"
        }
    }

    var color: String {
        switch self {
        case .centinela: return "gray"
        case .guardian: return "green"
        case .protector: return "blue"
        case .leyenda: return "gold"
        }
    }

    var blocksRequired: Int {
        switch self {
        case .centinela: return 0
        case .guardian: return 50
        case .protector: return 200
        case .leyenda: return 500
        }
    }

    var invitesRequired: Int {
        switch self {
        case .centinela: return 0
        case .guardian: return 3
        case .protector: return 10
        case .leyenda: return 30
        }
    }

    var reportsRequired: Int {
        switch self {
        case .centinela: return 0
        case .guardian: return 10
        case .protector: return 50
        case .leyenda: return 150
        }
    }

    var nextRank: CommunityRank? {
        let all = CommunityRank.allCases
        guard let idx = all.firstIndex(of: self), idx < all.count - 1 else { return nil }
        return all[idx + 1]
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
    @Published public var userRank = CommunityRank.centinela
    @Published public var userBlocksCount = 0
    @Published public var userInvitesCount = 0
    @Published public var communityBlockedTotal = "1,423,800"
    @Published public var topReported = [TopReportedNumber]()
    @Published public var errorMessage: String? = nil
    @Published public var blocksToNextRank: Int = 50
    @Published public var invitesToNextRank: Int = 3
    @Published public var reportsToNextRank: Int = 10
    @Published public var rankProgress: Double = 0.0

    private let baseURL = URL(string: AppConfig.apiBaseURL)!
    private let apiKey = AppConfig.apiKey
    public init() {
        loadCachedRank()
        fetchNetworkStats()
    }

    private func loadCachedRank() {
        let blocks = UserDefaults.standard.integer(forKey: "patova_user_blocks")
        let invites = UserDefaults.standard.integer(forKey: "patova_user_invites")
        let reports = UserDefaults.standard.integer(forKey: "patova_user_reports")
        userBlocksCount = blocks
        userInvitesCount = invites
        userReportsCount = "\(reports)"
        computeRank()
    }

    public func computeRank() {
        let blocks = userBlocksCount
        let invites = userInvitesCount
        let reports = Int(userReportsCount) ?? 0

        let maxRank = CommunityRank.allCases.last { rank in
            blocks >= rank.blocksRequired && invites >= rank.invitesRequired && reports >= rank.reportsRequired
        }
        userRank = maxRank ?? .centinela

        if let next = userRank.nextRank {
            let blockProgress = userRank.blocksRequired > 0
                ? Double(min(blocks, next.blocksRequired) - userRank.blocksRequired) / Double(next.blocksRequired - userRank.blocksRequired)
                : Double(blocks) / Double(next.blocksRequired)
            let inviteProgress = userRank.invitesRequired > 0
                ? Double(min(invites, next.invitesRequired) - userRank.invitesRequired) / Double(next.invitesRequired - userRank.invitesRequired)
                : Double(invites) / Double(next.invitesRequired)
            let reportProgress = userRank.reportsRequired > 0
                ? Double(min(reports, next.reportsRequired) - userRank.reportsRequired) / Double(next.reportsRequired - userRank.reportsRequired)
                : Double(reports) / Double(next.reportsRequired)

            rankProgress = min(1.0, (blockProgress + inviteProgress + reportProgress) / 3.0)
            blocksToNextRank = max(0, next.blocksRequired - blocks)
            invitesToNextRank = max(0, next.invitesRequired - invites)
            reportsToNextRank = max(0, next.reportsRequired - reports)
        } else {
            rankProgress = 1.0
            blocksToNextRank = 0
            invitesToNextRank = 0
            reportsToNextRank = 0
        }
    }

    public func incrementBlocks(by count: Int = 1) {
        userBlocksCount += count
        UserDefaults.standard.set(userBlocksCount, forKey: "patova_user_blocks")
        computeRank()
    }

    public func incrementInvites(by count: Int = 1) {
        userInvitesCount += count
        UserDefaults.standard.set(userInvitesCount, forKey: "patova_user_invites")
        computeRank()
    }

    public func incrementReports(by count: Int = 1) {
        let current = Int(userReportsCount) ?? 0
        userReportsCount = "\(current + count)"
        UserDefaults.standard.set(current + count, forKey: "patova_user_reports")
        computeRank()
    }

    public func fetchNetworkStats() {
        self.isLoading = true
        self.errorMessage = nil
        var request = URLRequest(url: baseURL.appendingPathComponent("stats"))
        request.httpMethod = "GET"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(apiKey, forHTTPHeaderField: "X-Patova-Key")

        let requestToSend = request
        Task {
            do {
                let (data, response) = try await URLSession.shared.data(for: requestToSend)

                guard let httpResponse = response as? HTTPURLResponse, httpResponse.statusCode == 200 else {
                    throw NSError(domain: "PatovaNetwork", code: -1, userInfo: [NSLocalizedDescriptionKey: "Error en API de estadisticas"])
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
                    self.communityBlockedTotal = String(format: "%,d", stats.totalReports * 186)
                    self.errorMessage = nil
                } else {
                    self.isRealData = false
                    self.totalUsers = "14,592"
                    self.lastUpdate = "24m"
                    self.totalReports = "+8.5k"
                    self.newToday = "1.2k"
                    self.topReported = stats.topReported
                    self.communityBlockedTotal = "1,423,800"
                    self.errorMessage = nil
                }
                self.isLoading = false
            } catch {
                print("Patova: Error cargando estadisticas: \(error.localizedDescription)")
                self.isRealData = false
                self.totalUsers = "14,592"
                self.lastUpdate = "Modo Offline"
                self.totalReports = "+8.5k"
                self.newToday = "1.2k"
                self.communityBlockedTotal = "1,423,800"
                self.errorMessage = "Usando datos locales pre-cargados"
                self.isLoading = false
            }
        }
    }
}
