import Foundation
import CallKit

/// Clase principal de la Extensión CallKit de Patova.
/// Esta extensión es levantada directamente por el sistema operativo iOS al
/// momento de recargar el directorio de bloqueo.
/// Lee la base de datos local SQLite (compartida en el App Group) e indexa los números
/// de spam y sospechosos en el motor nativo del iPhone en menos de 100ms.
public final class CallDirectoryHandler: CXCallDirectoryProvider {

    override public func beginRequest(with context: CXCallDirectoryExtensionContext) {
        context.delegate = self

        print("🔌 Patova Extension: Inicializando carga de CallKit...")

        // 1. Agregar números destinados a Bloqueo Duro (Rechazo automático)
        if !addAllBlockingPhoneNumbers(to: context) {
            let error = NSError(domain: "PatovaExtension", code: 1, userInfo: [NSLocalizedDescriptionKey: "Error en carga de bloqueados"])
            context.cancelRequest(withError: error)
            return
        }

        // 2. Agregar números destinados a Identificación en Pantalla (Filtro Suave)
        if !addAllIdentificationPhoneNumbers(to: context) {
            let error = NSError(domain: "PatovaExtension", code: 2, userInfo: [NSLocalizedDescriptionKey: "Error en carga de sospechosos"])
            context.cancelRequest(withError: error)
            return
        }

        print("🎉 Patova Extension: CompleteRequest enviado con éxito a iOS.")
        context.completeRequest()
    }

    /// Lee el archivo binario plano e inyecta los números a bloquear.
    /// CallKit requiere obligatoriamente que estén ordenados ascendentemente.
    private func addAllBlockingPhoneNumbers(to context: CXCallDirectoryExtensionContext) -> Bool {
        print("🚫 Patova Extension: Iniciando streaming binario de números bloqueados...")
        
        guard let containerURL = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: "group.com.serra.app.patova") else {
            print("❌ Patova Extension: App Group URL no disponible.")
            // Intentamos fallback a SQLite si no está disponible el contenedor
            return fallbackToSQLiteBlocking(to: context)
        }
        let fileURL = containerURL.appendingPathComponent("blocked_numbers.bin")
        
        guard let fileHandle = try? FileHandle(forReadingFrom: fileURL) else {
            print("⚠️ Patova Extension: No se encontró el archivo binario. Usando SQLite como fallback...")
            return fallbackToSQLiteBlocking(to: context)
        }
        defer {
            try? fileHandle.close()
        }
        
        let bufferSize = 4096 // 4 KB por lectura
        let elementSize = MemoryLayout<Int64>.size
        
        while true {
            let data = fileHandle.readData(ofLength: bufferSize)
            if data.isEmpty { break }
            
            data.withUnsafeBytes { (rawBuffer: UnsafeRawBufferPointer) in
                let count = rawBuffer.count / elementSize
                let typedBuffer = rawBuffer.bindMemory(to: Int64.self)
                
                for i in 0..<count {
                    let number = typedBuffer[i]
                    guard number > 0 else { continue }
                    context.addBlockingEntry(withNextSequentialPhoneNumber: number)
                }
            }
        }
        
        return true
    }
    
    /// Fallback en caso de que falte el binario
    private func fallbackToSQLiteBlocking(to context: CXCallDirectoryExtensionContext) -> Bool {
        DatabaseManager.shared.streamBlockingNumbers(threshold: 75) { number in
            guard number > 0 else { return }
            context.addBlockingEntry(withNextSequentialPhoneNumber: number)
        }
        return true
    }

    /// Lee la base de datos SQLite compartida e inyecta los números sospechosos para mostrar la etiqueta.
    /// CallKit requiere obligatoriamente que estén ordenados ascendentemente por número de teléfono.
    private func addAllIdentificationPhoneNumbers(to context: CXCallDirectoryExtensionContext) -> Bool {
        print("ℹ️ Patova Extension: Iniciando streaming de identificadores de pantalla...")
        
        DatabaseManager.shared.streamIdentificationEntries(threshold: 75) { number, label in
            guard number > 0 else { return }
            // Registramos el número y la etiqueta que aparecerá en pantalla nativa (ej: "Patova: Cobranzas")
            context.addIdentificationEntry(withNextSequentialPhoneNumber: number, label: label)
        }
        
        return true
    }
}

// MARK: - CXCallDirectoryExtensionContextDelegate

extension CallDirectoryHandler: CXCallDirectoryExtensionContextDelegate {
    
    /// Se gatilla si el sistema operativo iOS falla durante la importación.
    public func requestFailed(for extensionContext: CXCallDirectoryExtensionContext, withError error: Error) {
        print("❌ Patova Extension: La carga de CallKit falló críticamente en iOS: \(error.localizedDescription)")
    }
}
