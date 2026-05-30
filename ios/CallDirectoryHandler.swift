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

    /// Lee la base de datos SQLite compartida e inyecta los números a bloquear.
    /// CallKit requiere obligatoriamente que estén ordenados ascendentemente.
    private func addAllBlockingPhoneNumbers(to context: CXCallDirectoryExtensionContext) -> Bool {
        // Obtenemos los números con score >= 75 (Spam severo) de SQLite.
        // La consulta de SQLite ya devuelve la lista ordenada ascendentemente por 'phone_number'.
        let blockedNumbers = DatabaseManager.shared.getBlockingNumbers(threshold: 75)
        
        print("🚫 Patova Extension: Inyectando \(blockedNumbers.count) números bloqueados...")
        
        for number in blockedNumbers {
            // Evitar inyectar el número 0 que rompe CallKit
            guard number > 0 else { continue }
            context.addBlockingEntry(withNextSequentialPhoneNumber: number)
        }
        
        return true
    }

    /// Lee la base de datos SQLite compartida e inyecta los números sospechosos para mostrar la etiqueta.
    /// CallKit requiere obligatoriamente que estén ordenados ascendentemente por número de teléfono.
    private func addAllIdentificationPhoneNumbers(to context: CXCallDirectoryExtensionContext) -> Bool {
        // Obtenemos los números con score < 75 (Sospechosos / Repartos) y sus etiquetas.
        // La consulta ya está indexada y ordenada de menor a mayor.
        let entries = DatabaseManager.shared.getIdentificationEntries(threshold: 75)
        let numbers = entries.numbers
        let labels = entries.labels
        
        print("ℹ️ Patova Extension: Inyectando \(numbers.count) identificadores de pantalla...")
        
        for index in 0..<numbers.count {
            let number = numbers[index]
            let label = labels[index]
            
            guard number > 0 else { continue }
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
