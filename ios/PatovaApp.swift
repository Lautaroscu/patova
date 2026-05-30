import SwiftUI

/// Punto de Entrada Principal de Patova para iOS en SwiftUI.
/// Declara el ciclo de vida de la aplicación e inicializa el DashboardView.
@main
struct PatovaApp: App {
    
    // Inicialización global opcional al iniciar la app
    init() {
        // Personalización de estilos globales en iOS
        UINavigationBar.appearance().largeTitleTextAttributes = [.foregroundColor: UIColor.white]
        UINavigationBar.appearance().titleTextAttributes = [.foregroundColor: UIColor.white]
        
        // Registrar llamadas de sincronización en segundo plano aquí más adelante
        print("🚀 Patova App inicializada exitosamente.")
    }
    
    var body: some Scene {
        WindowGroup {
            MainTabView()
        }
    }
}
