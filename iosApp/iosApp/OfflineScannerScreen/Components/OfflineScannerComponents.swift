import SwiftUI
import Shared

// MARK: - File Type Placeholder

struct OfflineFileTypePlaceholder: View {
    let fileType: OfflineFileType

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: fileType.iconName)
                .font(.system(size: 48))
                .foregroundStyle(.secondary)
            Text(fileType.localizedLabel)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 160)
    }
}

// MARK: - Loading View

struct OfflineLoadingView: View {
    private let strings = OfflineScannerStrings()

    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
            Text(strings.loadingOcr)
                .font(.subheadline)
                .foregroundStyle(.secondary)
            Text(strings.loadingCleanup)
                .font(.caption)
                .foregroundStyle(.tertiary)
        }
        .frame(maxWidth: .infinity)
        .padding(24)
    }
}

// MARK: - Error Card

struct OfflineErrorCard: View {
    let message: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(.red)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.primary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemRed).opacity(0.15))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

// MARK: - Expandable Result Card

struct ExpandableResultCard: View {
    let data: DomainMedicalData
    @State private var expanded = false
    private let strings = OfflineScannerStrings()

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Button(action: { withAnimation { expanded.toggle() } }) {
                HStack {
                    Image(systemName: "checkmark.circle.fill")
                        .foregroundStyle(.green)
                    Text(strings.viewResult)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundStyle(.primary)
                    Spacer()
                    Image(systemName: expanded ? "chevron.up" : "chevron.down")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .buttonStyle(.plain)

            if expanded {
                VStack(alignment: .leading, spacing: 12) {
                    if let docType = data.documentType {
                        OfflineMetaRow(title: strings.labelDocumentType, value: docType)
                    }
                    Divider()
                    if let inst = data.institution {
                        OfflineMetaRow(title: strings.labelInstitution, value: inst)
                    }
                    if let doc = data.doctorName {
                        OfflineMetaRow(title: strings.labelDoctor, value: doc)
                    }
                    if let date = data.analysisDate {
                        OfflineMetaRow(title: strings.labelAnalysisDate, value: date)
                    }
                    if let indicators = data.indicators, !indicators.isEmpty {
                        VStack(alignment: .leading, spacing: 8) {
                            Text(strings.indicatorsCount(indicators.count))
                                .font(.caption)
                                .foregroundStyle(.secondary)
                            VStack(spacing: 4) {
                                ForEach(indicators.indices, id: \.self) { i in
                                    let ind = indicators[i]
                                    HStack {
                                        Text(ind.name)
                                            .foregroundStyle(.secondary)
                                        Spacer()
                                        Text(ind.value)
                                            .fontWeight(.medium)
                                        if let ref = ind.referenceRange {
                                            Text(ref)
                                                .font(.caption2)
                                                .foregroundStyle(.tertiary)
                                        }
                                    }
                                    .font(.subheadline)
                                }
                            }
                            .padding(12)
                            .background(Color(.secondarySystemBackground))
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                        }
                    }
                }
            }
        }
        .padding(16)
        .background(Color(.secondarySystemBackground).opacity(0.5))
        .clipShape(RoundedRectangle(cornerRadius: 12))
    }
}

struct OfflineMetaRow: View {
    let title: String
    let value: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text(title)
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(width: 100, alignment: .leading)
            Text(value)
                .font(.subheadline)
        }
    }
}

// MARK: - Toast

struct OfflineToastView: View {
    let message: String

    var body: some View {
        Text(message)
            .font(.subheadline)
            .padding(.horizontal, 20)
            .padding(.vertical, 12)
            .background(.ultraThinMaterial)
            .clipShape(Capsule())
            .padding(.bottom, 32)
    }
}
