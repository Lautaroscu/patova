import Foundation

/// Configuración centralizada de Patova.
/// Los valores se leen de Info.plist para permitir inyección desde Xcode Cloud.
/// Si no existen en el plist, se usan los defaults de desarrollo.
enum AppConfig {
    
    // MARK: - API
    
    static let apiBaseURL: String = {
        Bundle.main.object(forInfoDictionaryKey: "PATOVA_API_BASE_URL") as? String
            ?? "https://patova-api.serra.agency/v1"
    }()
    
    static let apiKey: String = {
        Bundle.main.object(forInfoDictionaryKey: "PATOVA_API_KEY") as? String
            ?? "dev-dummy-key"
    }()
    
    // MARK: - Identificadores
    
    static let callKitExtensionIdentifier = "com.serra.app.patova.CallDirectory"
    static let appGroupIdentifier = "group.com.serra.app.patova"
    static let defaultUserId = "usr_8341f3e48971"
}
