import Foundation
import CallKit

@MainActor
final class MiPatovaViewModel: ObservableObject {
    @Published var isPremium: Bool = false
    @Published var premiumExpires: String = "-"

    @Published var callKitEnabled: Bool = false
    @Published var blockedCount: Int = 0
    @Published var identifiedCount: Int = 0
    @Published var lastSync: String = "Nunca"
    @Published var appVersion: String = ""
    @Published var buildNumber: String = ""

    @Published var userRank: CommunityRank = .centinela
    @Published var userBlocks: Int = 0
    @Published var userInvites: Int = 0
    @Published var userReports: Int = 0
    @Published var rankProgress: Double = 0.0
    @Published var blocksToNext: Int = 50
    @Published var invitesToNext: Int = 3
    @Published var reportsToNext: Int = 10

    init() {
        loadData()
    }

    func loadData() {
        isPremium = UserDefaults.standard.bool(forKey: "patova_is_premium")
        premiumExpires = UserDefaults.standard.string(forKey: "patova_premium_expires") ?? "-"

        blockedCount = DatabaseManager.shared.getBlockingCount()
        identifiedCount = DatabaseManager.shared.getIdentificationCount()
        lastSync = UserDefaults.standard.string(forKey: "patova_last_sync_timestamp") ?? "Nunca"

        appVersion = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        buildNumber = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"

        userBlocks = UserDefaults.standard.integer(forKey: "patova_user_blocks")
        userInvites = UserDefaults.standard.integer(forKey: "patova_user_invites")
        userReports = UserDefaults.standard.integer(forKey: "patova_user_reports")

        computeRank()

        CXCallDirectoryManager.sharedInstance.getEnabledStatusForExtension(
            withIdentifier: AppConfig.callKitExtensionIdentifier
        ) { [weak self] status, _ in
            DispatchQueue.main.async {
                self?.callKitEnabled = (status == .enabled)
            }
        }
    }

    private func computeRank() {
        let blocks = userBlocks
        let invites = userInvites
        let reports = userReports

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
            blocksToNext = max(0, next.blocksRequired - blocks)
            invitesToNext = max(0, next.invitesRequired - invites)
            reportsToNext = max(0, next.reportsRequired - reports)
        } else {
            rankProgress = 1.0
            blocksToNext = 0
            invitesToNext = 0
            reportsToNext = 0
        }
    }
}
