import SwiftUI
import shared

@main
struct iOSApp: App {
    init() {
        StartDIKt.startDI(
            nativeModule: nativeModule,
            appDeclaration: { _ in }
        )
    }
    
	var body: some Scene {
		WindowGroup {
			ContentView()
		}
	}
}
