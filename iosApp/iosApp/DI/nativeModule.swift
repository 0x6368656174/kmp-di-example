import Foundation
import shared

var nativeModule: Koin_coreModule = MakeNativeModuleKt.makeNativeModule(
    analytic: { scope in
        return AnalyticImpl(logger: scope.get())
    }
)
