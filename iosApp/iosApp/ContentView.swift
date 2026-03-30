import SwiftUI

struct ContentView: View {
    var body: some View {
        TabView {
            AIScannerScreen()
                .tabItem {
                    Label(String(localized: "tab_ai_scanner"), systemImage: "doc.text.viewfinder")
                }

            OfflineScannerScreen()
                .tabItem {
                    Label(String(localized: "tab_offline"), systemImage: "doc.text.viewfinder")
                }

            DocumentsScreen()
                .tabItem {
                    Label(String(localized: "tab_documents"), systemImage: "folder")
                }

            SettingsScreen()
                .tabItem {
                    Label(String(localized: "tab_settings"), systemImage: "gearshape")
                }
        }
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}
