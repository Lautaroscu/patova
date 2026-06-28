import Foundation
import CallKit

/// Cliente de Sincronización de Red en Swift para Patova.
/// Se conecta de manera asíncrona con la API FastAPI de Patova, descarga
/// los deltas de spam locales/globales y actualiza el motor de bloqueo de CallKit.
public final class PatovaSyncClient {
    
    public static let shared = PatovaSyncClient()
    
    private let baseURL = URL(string: AppConfig.apiBaseURL)!
    private let apiKey = AppConfig.apiKey
    private let callKitExtensionIdentifier = AppConfig.callKitExtensionIdentifier
    
    private init() {}
    
    // MARK: - API Structs (Espejo de FastAPI Schemas)
    
    struct SyncRequest: Codable {
        let userId: String
        let clientLastSyncTimestamp: String
        let localChanges: LocalChanges
        
        enum CodingKeys: String, CodingKey {
            case userId = "user_id"
            case clientLastSyncTimestamp = "client_last_sync_timestamp"
            case localChanges = "local_changes"
        }
    }
    
    struct LocalChanges: Codable {
        let preferences: LocalPreferences?
        let newWhitelistEntries: [WhitelistDeltaEntry]
        let newBlacklistEntries: [BlacklistDeltaEntry]
        
        enum CodingKeys: String, CodingKey {
            case preferences
            case newWhitelistEntries = "new_whitelist_entries"
            case newBlacklistEntries = "new_blacklist_entries"
        }
    }
    
    struct LocalPreferences: Codable {
        let strictMode: Bool
        let blockUnknown: Bool
        let spamThreshold: Double
        let syncEnabled: Bool
        let updatedAt: String
        
        enum CodingKeys: String, CodingKey {
            case strictMode = "strict_mode"
            case blockUnknown = "block_unknown"
            case spamThreshold = "spam_threshold"
            case syncEnabled = "sync_enabled"
            case updatedAt = "updated_at"
        }
    }
    
    struct WhitelistDeltaEntry: Codable {
        let phoneHash: String
        let label: String
        let addedAt: String
        
        enum CodingKeys: String, CodingKey {
            case phoneHash = "phone_hash"
            case label
            case addedAt = "added_at"
        }
    }
    
    struct BlacklistDeltaEntry: Codable {
        let phoneHash: String
        let reason: String
        let addedAt: String
        
        enum CodingKeys: String, CodingKey {
            case phoneHash = "phone_hash"
            case reason
            case addedAt = "added_at"
        }
    }
    
    struct SyncResponse: Codable {
        let syncTimestamp: String
        let syncStatus: String
        let blacklistDelta: [BlacklistDeltaEntry]
        
        enum CodingKeys: String, CodingKey {
            case syncTimestamp = "sync_timestamp"
            case syncStatus = "sync_status"
            case blacklistDelta = "blacklist_delta"
        }
    }
    
    // MARK: - Sincronización
    
    /// Ejecuta la sincronización incremental de fondo contra la API FastAPI y actualiza CallKit.
    /// - Parameter userId: Identificador único del usuario.
    public func performSync(userId: String) async throws {
        print("🔄 Patova: Iniciando sincronización de red...")
        
        // 1. Recuperar última fecha de sincronización local
        let lastSyncKey = "patova_last_sync_timestamp"
        let lastSyncStr = UserDefaults.standard.string(forKey: lastSyncKey) ?? "1970-01-01T00:00:00Z"
        
        // 2. Construir la Request (En iOS no enviamos cambios locales por ahora, es pull-only)
        let localChanges = LocalChanges(preferences: nil, newWhitelistEntries: [], newBlacklistEntries: [])
        let requestBody = SyncRequest(
            userId: userId,
            clientLastSyncTimestamp: lastSyncStr,
            localChanges: localChanges
        )
        
        var request = URLRequest(url: baseURL.appendingPathComponent("behavior").appendingPathComponent("sync"))
        request.httpMethod = "POST"
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue(apiKey, forHTTPHeaderField: "X-Patova-Key") // Cabecera de Seguridad obligatoria
        request.httpBody = try JSONEncoder().encode(requestBody)
        
        // 3. Realizar la llamada HTTP
        let (data, response) = try await URLSession.shared.data(for: request)
        
        guard let httpResponse = response as? HTTPURLResponse else {
            throw NSError(domain: "PatovaSync", code: -1, userInfo: [NSLocalizedDescriptionKey: "Respuesta de servidor inválida"])
        }
        
        guard httpResponse.statusCode == 200 else {
            throw NSError(domain: "PatovaSync", code: httpResponse.statusCode, userInfo: [NSLocalizedDescriptionKey: "Error en API de Sincronización"])
        }
        
        // 4. Decodificar la Respuesta
        let syncResult = try JSONDecoder().decode(SyncResponse.self, from: data)
        
        guard syncResult.syncStatus == "SUCCESS" else {
            print("❌ Patova: La API devolvió status fallido.")
            return
        }
        
        // 5. Guardar los números en SQLite
        print("💾 Patova: Procesando \(syncResult.blacklistDelta.count) números nuevos del servidor...")
        for entry in syncResult.blacklistDelta {
            // Nota de conversión: El hash de la API suele ser String, pero CallKit requiere enteros.
            // Para simplicidad, si la API devuelve números E164 convertidos a hashes o números directos,
            // los convertimos a Int64 para SQLite.
            if let numberInt = Int64(entry.phoneHash.replacingOccurrences(of: "+", with: "")) {
                let score = entry.reason == "HIGH_REPORT_VOLUME" ? 90 : 65
                let label = entry.reason == "HIGH_REPORT_VOLUME" ? "Patova: Spam Confirmado" : "Patova: Sospechoso"
                
                DatabaseManager.shared.saveEntry(phoneNumber: numberInt, score: score, label: label)
            }
        }
        
        // 6. Actualizar timestamp de última sincronización exitosa
        UserDefaults.standard.set(syncResult.syncTimestamp, forKey: lastSyncKey)
        print("✅ Patova: Sincronización local de SQLite finalizada.")
        
        // 7. Notificar a iOS CallKit que debe recargar la extensión de bloqueo inmediatamente
        try await reloadCallKitExtension()
    }
    
    /// Le avisa a iOS que debe regenerar el listado de CallKit inmediatamente con la nueva base de datos.
    private func reloadCallKitExtension() async throws {
        print("🔌 Patova: Solicitando recarga de la extensión de CallKit...")
        return try await withCheckedThrowingContinuation { continuation in
            CXCallDirectoryManager.sharedInstance.reloadExtension(withIdentifier: callKitExtensionIdentifier) { error in
                if let error = error {
                    print("❌ Patova: Error al recargar extensión de CallKit: \(error.localizedDescription)")
                    continuation.resume(throwing: error)
                } else {
                    print("🎉 Patova: Extensión de CallKit recargada e indexada exitosamente por iOS.")
                    continuation.resume()
                }
            }
        }
    }
}
