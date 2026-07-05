import SwiftUI
import Shared

// MARK: - Result Card

struct DocumentsResultCard: View {
    let data: HousingPaymentDocument
    var selectionMode: Bool = false
    var selected: Bool = false
    let onToggleSelect: () -> Void
    let onDelete: () -> Void
    private let strings = DocumentsStrings()

    @State private var expanded = false

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            HStack(alignment: .top, spacing: 8) {
                if selectionMode {
                    Button(action: onToggleSelect) {
                        Image(systemName: selected ? "checkmark.square.fill" : "square")
                            .font(.title3)
                            .foregroundStyle(selected ? Color.accentColor : .secondary)
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel(selected ? "Selected" : "Not selected")
                }

                Button {
                    if !selectionMode {
                        withAnimation(.easeInOut(duration: 0.25)) {
                            expanded.toggle()
                        }
                    } else {
                        onToggleSelect()
                    }
                } label: {
                    DocumentsCardCollapsedSummary(data: data)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
                .buttonStyle(.plain)
                .disabled(false)

                if !selectionMode {
                    VStack(spacing: 2) {
                        Button {
                            withAnimation(.easeInOut(duration: 0.25)) {
                                expanded.toggle()
                            }
                        } label: {
                            Image(systemName: "chevron.down")
                                .font(.system(size: 16, weight: .medium))
                                .foregroundStyle(Color.secondary)
                                .rotationEffect(.degrees(expanded ? 180 : 0))
                                .frame(width: 32, height: 32)
                        }
                        .buttonStyle(.plain)
                        .padding(.top, 1)

                        Button(action: onDelete) {
                            Image(systemName: "trash")
                                .font(.system(size: 15))
                                .foregroundStyle(.red)
                                .frame(width: 32, height: 32)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            if expanded {
                VStack(alignment: .leading, spacing: 12) {
                    Divider()
                        .padding(.top, 18)
                    DocumentsHousingBillContent(data: data)
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .padding(20)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
        .animation(.easeInOut(duration: 0.25), value: expanded)
    }
}

// MARK: - Collapsed Summary

private struct DocumentsCardCollapsedSummary: View {
    let data: HousingPaymentDocument

    var body: some View {
        HStack(alignment: .top, spacing: 10) {
            Image(systemName: "doc.text")
                .font(.system(size: 18))
                .foregroundStyle(Color.accentColor)
                .padding(.top, 2)

            VStack(alignment: .leading, spacing: 4) {
                Text(data.documentType)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .lineLimit(2)
                    .multilineTextAlignment(.leading)

                let subtitle = housingCollapsedSubtitle(data)
                if !subtitle.isEmpty {
                    Text(subtitle)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundStyle(Color.accentColor)
                }
                if let detail = housingCollapsedDetail(data) {
                    Text(detail)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
            }
        }
    }

    private func housingCollapsedSubtitle(_ bill: HousingPaymentDocument) -> String {
        var parts: [String] = []
        if let date = bill.documentDate, !date.isEmpty { parts.append(date) }
        if let amount = bill.amountDueForPeriod, !amount.isEmpty { parts.append("\(amount) ₽") }
        return parts.joined(separator: " · ")
    }

    private func housingCollapsedDetail(_ bill: HousingPaymentDocument) -> String? {
        if let inst = bill.institution, !inst.isEmpty { return inst }
        if let address = bill.propertyAddress, !address.isEmpty { return address }
        return nil
    }

}

// MARK: - Housing Bill Content

struct DocumentsHousingBillContent: View {
    let data: HousingPaymentDocument
    private let strings = OfflineScannerStrings()

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            paymentSummary

            if let inst = data.institution, !inst.isEmpty {
                DocumentsHousingTextRow(title: strings.labelProvider, value: inst)
            }
            if let payer = data.payerName, !payer.isEmpty, payer != data.institution {
                DocumentsHousingTextRow(title: strings.labelPayer, value: payer)
            }

            let compactFields = compactFieldItems
            if !compactFields.isEmpty {
                DocumentsHousingFieldGrid(items: compactFields)
            }

            if let address = data.propertyAddress, !address.isEmpty {
                DocumentsHousingTextRow(title: strings.labelPropertyAddress, value: address)
            }

            let stats = statItems
            if !stats.isEmpty {
                DocumentsHousingStatChips(items: stats)
            }

            if let services = data.serviceLines, !services.isEmpty {
                Divider()
                Text(strings.servicesCount(Int(services.count)))
                    .font(.subheadline)
                    .fontWeight(.semibold)
                VStack(spacing: 0) {
                    ForEach(Array(services.enumerated()), id: \.offset) { index, line in
                        DocumentsHousingServiceRow(line: line)
                        if index < services.count - 1 {
                            Divider().opacity(0.45)
                        }
                    }
                }
                .padding(12)
                .background(Color(.secondarySystemBackground).opacity(0.45))
                .clipShape(RoundedRectangle(cornerRadius: 14))
            }
        }
    }

    @ViewBuilder
    private var paymentSummary: some View {
        let amountDue = data.amountDueForPeriod.map { "\($0) ₽" }
        let amountPaid = data.amountPaid.map { "\($0) ₽" }
        let paymentDate = data.lastPaymentDate

        if amountDue != nil || amountPaid != nil || paymentDate != nil {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 4) {
                    Text(strings.labelAmountDue)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Text(amountDue ?? "—")
                        .font(.title2)
                        .fontWeight(.bold)
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                VStack(alignment: .trailing, spacing: 6) {
                    if let amountPaid {
                        VStack(alignment: .trailing, spacing: 2) {
                            Text(strings.labelAmountPaid)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            Text(amountPaid)
                                .font(.subheadline)
                                .fontWeight(.semibold)
                        }
                    }
                    if let paymentDate {
                        VStack(alignment: .trailing, spacing: 2) {
                            Text(strings.labelPaymentDate)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                            Text(paymentDate)
                                .font(.caption)
                        }
                    }
                }
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color.accentColor.opacity(0.12))
            .clipShape(RoundedRectangle(cornerRadius: 14))
        }
    }

    private var compactFieldItems: [(String, String)] {
        var items: [(String, String)] = []
        if let v = data.personalAccountNumber { items.append((strings.labelPersonalAccount, v)) }
        if let v = data.housingUtilitiesId { items.append((strings.labelHousingUtilitiesId, v)) }
        if let v = data.paymentDocumentId { items.append((strings.labelPaymentDocumentId, v)) }
        if let v = data.documentDate { items.append((strings.labelBillingPeriod, v)) }
        return items
    }

    private var statItems: [(String, String)] {
        var items: [(String, String)] = []
        if let v = data.totalAreaSqm { items.append((strings.labelTotalAreaShort, "\(v) м²")) }
        if let v = data.livingAreaSqm { items.append((strings.labelLivingAreaShort, "\(v) м²")) }
        if let v = data.residentsCount { items.append((strings.labelResidents, v)) }
        return items
    }
}

private struct DocumentsHousingTextRow: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.subheadline)
        }
        .padding(.vertical, 2)
    }
}


private struct DocumentsPlainMetaRow: View {
    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.subheadline)
        }
    }
}

private struct DocumentsHousingFieldGrid: View {
    let items: [(String, String)]

    var body: some View {
        let rows = stride(from: 0, to: items.count, by: 2).map { start in
            Array(items[start..<min(start + 2, items.count)])
        }
        VStack(spacing: 6) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                HStack(spacing: 8) {
                    ForEach(Array(row.enumerated()), id: \.offset) { _, item in
                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.0)
                                .font(.caption2)
                                .foregroundStyle(.secondary)
                                .lineLimit(1)
                            Text(item.1)
                                .font(.caption)
                                .fontWeight(.medium)
                                .lineLimit(2)
                        }
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 8)
                        .background(Color(.secondarySystemBackground).opacity(0.5))
                        .clipShape(RoundedRectangle(cornerRadius: 10))
                    }
                    if row.count == 1 {
                        Color.clear.frame(maxWidth: .infinity)
                    }
                }
            }
        }
    }
}

private struct DocumentsHousingStatChips: View {
    let items: [(String, String)]

    var body: some View {
        HStack(spacing: 8) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                VStack(spacing: 2) {
                    Text(item.1)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                    Text(item.0)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                }
                .frame(maxWidth: .infinity)
                .padding(.vertical, 8)
                .background(Color.accentColor.opacity(0.1))
                .clipShape(RoundedRectangle(cornerRadius: 10))
            }
        }
    }
}

private struct DocumentsHousingServiceRow: View {
    let line: HousingServiceLine

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 2) {
                Text(line.name)
                    .font(.subheadline)
                if let ref = serviceReferenceLabel {
                    Text(ref)
                        .font(.caption2)
                        .foregroundStyle(.tertiary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            Text("\(line.amountToPay) ₽")
                .font(.subheadline)
                .fontWeight(.semibold)
                .foregroundStyle(Color.accentColor)
        }
        .padding(.vertical, 7)
    }

    private var serviceReferenceLabel: String? {
        var parts: [String] = []
        if let volume = line.volume {
            parts.append("\(volume) \(line.unit ?? "")".trimmingCharacters(in: .whitespaces))
        }
        if let tariff = line.tariff {
            parts.append("тариф \(tariff)")
        }
        if let basis = line.volumeBasis {
            parts.append(basis.name.lowercased())
        }
        return parts.isEmpty ? nil : parts.joined(separator: " · ")
    }
}

struct DocumentsMetaRow: View {
    let title: String
    let value: String

    var body: some View {
        HStack(spacing: 8) {
            Text(title)
            Text(value)
        }
        .font(.subheadline)
    }
}

// Removed DocumentsIndicatorRow (medical "indicator" UI not used for housing documents)
