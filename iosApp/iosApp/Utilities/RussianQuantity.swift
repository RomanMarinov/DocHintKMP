import Foundation

/// Склонение «анализ» для русского (1 анализ, 3 анализа, 5 анализов, 11 анализов, 21 анализ…).
enum RussianQuantity {
    static func analysisCountLabel(count: Int) -> String {
        let n100 = count % 100
        let n10 = count % 10
        let word: String
        if (11...19).contains(n100) {
            word = "анализов"
        } else if n10 == 1 {
            word = "анализ"
        } else if (2...4).contains(n10) {
            word = "анализа"
        } else {
            word = "анализов"
        }
        return "\(count) \(word)"
    }
}
