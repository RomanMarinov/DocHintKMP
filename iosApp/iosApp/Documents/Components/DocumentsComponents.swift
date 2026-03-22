import SwiftUI
import Shared

// MARK: - Result Card

struct DocumentsResultCard: View {
    let data: DomainMedicalData
    let onDelete: () -> Void
    private let strings = DocumentsStrings()

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(strings.labelDocumentType)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Text(data.documentType ?? strings.analysisDefault)
                        .font(.headline)
                        .lineLimit(1)
                    if let date = data.analysisDate {
                        Text(date)
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                Spacer()
                Button(action: onDelete) {
                    Image(systemName: "trash")
                        .font(.body)
                        .foregroundStyle(.red)
                }
            }

            Divider()

            if let inst = data.institution {
                DocumentsMetaRow(title: strings.labelInstitution, value: inst)
            }
            if let doc = data.doctorName {
                DocumentsMetaRow(title: strings.labelDoctor, value: doc)
            }

            if let indicators = data.indicators, !indicators.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text(strings.indicatorsFormat(Int32(indicators.count)))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    VStack(spacing: 4) {
                        ForEach(Array(indicators.enumerated()), id: \.offset) { _, ind in
                            DocumentsIndicatorRow(indicator: ind)
                        }
                    }
                    .padding(12)
                    .background(Color(.secondarySystemBackground).opacity(0.5))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
        .padding(20)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
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

struct DocumentsIndicatorRow: View {
    let indicator: DomainAnalysisIndicator

    var body: some View {
        HStack {
            Text(indicator.name)
                .foregroundStyle(.secondary)
            Spacer()
            Text(indicator.value)
                .fontWeight(.medium)
            if let ref = indicator.referenceRange {
                Text(ref)
                    .font(.caption)
                    .foregroundStyle(.tertiary)
            }
        }
        .font(.subheadline)
    }
}
