import Foundation
import shared

class AnalyticImpl: Analytic {
    private let logger: Logger
    
    init(logger: Logger) {
        self.logger = logger
    }
    
    func logEvent(event: String) {
        logger.log(text: "Event \"\(event)\" sent to analytic by iOS implementation")
    }
}
