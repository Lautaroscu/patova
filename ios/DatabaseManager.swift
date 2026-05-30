import Foundation
import SQLite3

/// Manejador de Base de Datos SQLite optimizado para Patova iOS.
/// Esta clase gestiona el almacenamiento de números bloqueados y sospechosos
/// utilizando un contenedor de App Group compartido para que tanto la aplicación
/// principal como la extensión de CallKit tengan acceso concurrente ultrarrápido.
public final class DatabaseManager {
    
    public static let shared = DatabaseManager()
    
    private var db: OpaquePointer?
    private let appGroupIdentifier = "group.agency.serra.patova"
    private let dbName = "patova_spam_store.sqlite"
    
    private init() {
        setupDatabase()
    }
    
    deinit {
        if db != nil {
            sqlite3_close(db)
        }
    }
    
    /// Obtiene la URL del contenedor compartido de App Group.
    private func getDatabaseURL() -> URL? {
        guard let containerURL = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroupIdentifier) else {
            print("❌ Patova: Error al acceder al App Group Container. ¿Configuraste las Capabilities?")
            // Fallback al directorio de documentos de la App para pruebas locales sin App Groups firmados
            return FileManager.default.urls(for: .documentDirectory, in: .userDomainMask).first?.appendingPathComponent(dbName)
        }
        return containerURL.appendingPathComponent(dbName)
    }
    
    /// Inicializa la base de datos y crea las tablas e índices si no existen.
    private func setupDatabase() {
        guard let dbURL = getDatabaseURL() else { return }
        
        // Abrir base de datos en modo multi-hilo para concurrencia segura
        if sqlite3_open_v2(dbURL.path, &db, SQLITE_OPEN_READWRITE | SQLITE_OPEN_CREATE | SQLITE_OPEN_FULLMUTEX, nil) != SQLITE_OK {
            print("❌ Patova: Error al abrir la base de datos SQLite.")
            return
        }
        
        // Habilitar modo WAL (Write-Ahead Logging) para permitir lecturas simultáneas durante la escritura
        sqlite3_exec(db, "PRAGMA journal_mode=WAL;", nil, nil, nil)
        
        let createTableQuery = """
        CREATE TABLE IF NOT EXISTS spam_entries (
            phone_number INTEGER PRIMARY KEY,
            spam_score INTEGER NOT NULL,
            label TEXT
        );
        """
        
        var errorStatement: UnsafeMutablePointer<Int8>?
        if sqlite3_exec(db, createTableQuery, nil, nil, &errorStatement) != SQLITE_OK {
            let errorMsg = String(cString: errorStatement!)
            print("❌ Patova: Error al crear la tabla: \(errorMsg)")
            sqlite3_free(errorStatement)
        } else {
            print("✨ Patova: Base de datos SQLite inicializada correctamente en: \(dbURL.lastPathComponent)")
        }
    }
    
    // MARK: - Operaciones CRUD
    
    /// Inserta o actualiza un registro de número de teléfono con su score y etiqueta.
    /// - Parameters:
    ///   - phoneNumber: Número en formato numérico puro sin el signo "+" (ej: 5491112345678).
    ///   - score: Nivel de sospecha del 0 al 100.
    ///   - label: Etiqueta identificadora opcional (ej: "Patova: Cobranzas").
    public func saveEntry(phoneNumber: Int64, score: Int, label: String?) {
        let query = "INSERT OR REPLACE INTO spam_entries (phone_number, spam_score, label) VALUES (?, ?, ?);"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int64(statement, 1, phoneNumber)
            sqlite3_bind_int(statement, 2, Int32(score))
            if let label = label {
                sqlite3_bind_text(statement, 3, (label as NSString).utf8String, -1, nil)
            } else {
                sqlite3_bind_null(statement, 3)
            }
            
            if sqlite3_step(statement) != SQLITE_DONE {
                print("❌ Patova: Error al guardar entrada telefónica.")
            }
        }
        sqlite3_finalize(statement)
    }
    
    /// Elimina un número de teléfono de la base de datos local.
    public func deleteEntry(phoneNumber: Int64) {
        let query = "DELETE FROM spam_entries WHERE phone_number = ?;"
        var statement: OpaquePointer?
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int64(statement, 1, phoneNumber)
            if sqlite3_step(statement) != SQLITE_DONE {
                print("❌ Patova: Error al eliminar entrada telefónica.")
            }
        }
        sqlite3_finalize(statement)
    }
    
    /// Limpia todos los registros de la base de datos.
    public func clearDatabase() {
        sqlite3_exec(db, "DELETE FROM spam_entries;", nil, nil, nil)
        print("🧹 Patova: Base de datos local limpiada completamente.")
    }
    
    // MARK: - Consultas para CallKit (Requisito: ORDENADAS ASCENDENTEMENTE)
    
    /// Obtiene todos los números de teléfono destinados a BLOQUEO ABSOLUTO.
    /// CallKit exige que la lista esté estrictamente ordenada de forma ascendente.
    /// - Parameter threshold: Umbral mínimo de score para considerarse bloqueo duro (Default: 75).
    public func getBlockingNumbers(threshold: Int = 75) -> [Int64] {
        let query = "SELECT phone_number FROM spam_entries WHERE spam_score >= ? ORDER BY phone_number ASC;"
        var statement: OpaquePointer?
        var numbers = [Int64]()
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int(statement, 1, Int32(threshold))
            
            while sqlite3_step(statement) == SQLITE_ROW {
                let number = sqlite3_column_int64(statement, 0)
                numbers.append(number)
            }
        }
        sqlite3_finalize(statement)
        return numbers
    }
    
    /// Obtiene todos los números de teléfono y sus etiquetas destinados a IDENTIFICACIÓN de llamada.
    /// CallKit exige que la lista esté estrictamente ordenada de forma ascendente por número de teléfono.
    /// - Parameter threshold: Umbral de bloqueo. Los números bajo este score serán solo identificados.
    public func getIdentificationEntries(threshold: Int = 75) -> (numbers: [Int64], labels: [String]) {
        let query = "SELECT phone_number, label FROM spam_entries WHERE spam_score < ? ORDER BY phone_number ASC;"
        var statement: OpaquePointer?
        var numbers = [Int64]()
        var labels = [String]()
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int(statement, 1, Int32(threshold))
            
            while sqlite3_step(statement) == SQLITE_ROW {
                let number = sqlite3_column_int64(statement, 0)
                let labelCString = sqlite3_column_text(statement, 1)
                
                let label = labelCString != nil ? String(cString: labelCString!) : "Patova: Posible Spam"
                numbers.append(number)
                labels.append(label)
            }
        }
        sqlite3_finalize(statement)
        return (numbers, labels)
    }
    
    /// Obtiene la cantidad de números configurados para bloqueo absoluto.
    public func getBlockingCount(threshold: Int = 75) -> Int {
        let query = "SELECT COUNT(*) FROM spam_entries WHERE spam_score >= ?;"
        var statement: OpaquePointer?
        var count = 0
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int(statement, 1, Int32(threshold))
            if sqlite3_step(statement) == SQLITE_ROW {
                count = Int(sqlite3_column_int(statement, 0))
            }
        }
        sqlite3_finalize(statement)
        return count
    }
    
    /// Obtiene la cantidad de números configurados para identificación.
    public func getIdentificationCount(threshold: Int = 75) -> Int {
        let query = "SELECT COUNT(*) FROM spam_entries WHERE spam_score < ?;"
        var statement: OpaquePointer?
        var count = 0
        
        if sqlite3_prepare_v2(db, query, -1, &statement, nil) == SQLITE_OK {
            sqlite3_bind_int(statement, 1, Int32(threshold))
            if sqlite3_step(statement) == SQLITE_ROW {
                count = Int(sqlite3_column_int(statement, 0))
            }
        }
        sqlite3_finalize(statement)
        return count
    }
}
