import SwiftUI
import Shared

@main
struct iOSApp: App {
    init() {
        DocHintKmpInitKt.doInitKoinForIos(appDeclaration: { _ in })
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}