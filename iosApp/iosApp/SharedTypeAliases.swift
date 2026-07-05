import Foundation
import Shared

// KMP exports domain types with `Domain` prefix (e.g. `DomainHousingPaymentDocument`).
// Existing Swift code expects `HousingPaymentDocument` etc. Provide aliases.
typealias HousingPaymentDocument = DomainHousingPaymentDocument
typealias HousingServiceLine = DomainHousingServiceLine
typealias HousingBillCategory = DomainHousingBillCategory
typealias HousingServiceGroup = DomainHousingServiceGroup
typealias HousingVolumeBasis = DomainHousingVolumeBasis
