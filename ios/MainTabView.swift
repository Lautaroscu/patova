import SwiftUI

struct MainTabView: View {
    @State private var selectedTab = 0
    
    init() {
        // Estilizar barra de pestañas (Tab Bar) en modo oscuro premium
        let appearance = UITabBarAppearance()
        appearance.configureWithOpaqueBackground()
        appearance.backgroundColor = UIColor(red: 0.05, green: 0.05, blue: 0.08, alpha: 1.0)
        
        // Elementos activos (Azul Premium de Patova)
        appearance.stackedLayoutAppearance.selected.iconColor = UIColor(red: 0.18, green: 0.48, blue: 0.96, alpha: 1.0)
        appearance.stackedLayoutAppearance.selected.titleTextAttributes = [
            .foregroundColor: UIColor(red: 0.18, green: 0.48, blue: 0.96, alpha: 1.0),
            .font: UIFont.systemFont(ofSize: 10, weight: .bold)
        ]
        
        // Elementos inactivos
        appearance.stackedLayoutAppearance.normal.iconColor = UIColor.white.withAlphaComponent(0.4)
        appearance.stackedLayoutAppearance.normal.titleTextAttributes = [
            .foregroundColor: UIColor.white.withAlphaComponent(0.4),
            .font: UIFont.systemFont(ofSize: 10, weight: .semibold)
        ]
        
        UITabBar.appearance().standardAppearance = appearance
        if #available(iOS 15.0, *) {
            UITabBar.appearance().scrollEdgeAppearance = appearance
        }
    }
    
    var body: some View {
        TabView(selection: $selectedTab) {
            DashboardView()
                .tabItem {
                    Label("Inicio", systemImage: "shield.fill")
                }
                .tag(0)
            
            NetworkView()
                .tabItem {
                    Label("Red", systemImage: "globe.americas.fill")
                }
                .tag(1)
            
            PaywallView()
                .tabItem {
                    Label("Premium", systemImage: "star.fill")
                }
                .tag(2)

            MiPatovaView()
                .tabItem {
                    Label("Mi Patova", systemImage: "person.crop.circle.fill")
                }
                .tag(3)
        }
        .preferredColorScheme(.dark)
    }
}

struct MainTabView_Previews: PreviewProvider {
    static var previews: some View {
        MainTabView()
    }
}
