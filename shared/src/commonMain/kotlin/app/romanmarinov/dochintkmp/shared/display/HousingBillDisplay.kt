package app.romanmarinov.dochintkmp.shared.display

import app.romanmarinov.dochintkmp.data.mapper.isHousingBillDocument
import app.romanmarinov.dochintkmp.data.mapper.toHousingPaymentDocumentOrNull
import app.romanmarinov.dochintkmp.domain.model.HousingPaymentDocument
import app.romanmarinov.dochintkmp.domain.model.MedicalData

object HousingBillDisplay {
    fun isHousingBill(data: MedicalData): Boolean = data.isHousingBillDocument()

    fun parse(data: MedicalData): HousingPaymentDocument? = data.toHousingPaymentDocumentOrNull()
}
