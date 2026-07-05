package app.romanmarinov.dochintkmp.presentation.documents_screen.util

import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument

internal fun HousingPaymentDocument.sameAs(other: HousingPaymentDocument): Boolean {
    return documentType == other.documentType &&
            institution == other.institution &&
            documentDate == other.documentDate &&
            payerName == other.payerName &&
            propertyAddress == other.propertyAddress &&
            amountDueForPeriod == other.amountDueForPeriod &&
            serviceLines == other.serviceLines
}
