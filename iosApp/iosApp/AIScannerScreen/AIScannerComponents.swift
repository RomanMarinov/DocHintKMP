import SwiftUI
import Shared

// MARK: - Loading Card (matches Android pipeline steps)
struct AILoadingCard: View {
    private let strings = AIScannerStrings()

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ProgressView()
                .scaleEffect(0.9)
            AIPipelineStep(label: strings.pipelineOcr, done: true)
            AIPipelineStep(label: strings.pipelineCleanup, done: true)
            AIPipelineStep(label: strings.pipelineDetect, done: true)
            AIPipelineStep(label: strings.pipelineLlm, done: false)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
    }
}

struct AIPipelineStep: View {
    let label: String
    let done: Bool

    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(done ? Color.accentColor : Color(.systemGray4))
                .frame(width: 8, height: 8)
            Text(label)
                .font(.subheadline)
                .foregroundStyle(done ? Color.primary : Color.secondary)
        }
    }
}

// MARK: - Success Card
struct AISuccessCard: View {
    let data: AIMedicalData
    let inDropdown: Bool
    private let strings = AIScannerStrings()

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 12) {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
                    .font(.title3)
                Text(strings.dataExtracted)
                    .font(.headline)
            }

            HStack {
                Text(strings.labelDocumentType)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                Text(data.documentType)
                    .font(.subheadline)
                    .fontWeight(.semibold)
                    .foregroundStyle(Color.accentColor)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 10)
            .background(Color.accentColor.opacity(0.15))
            .clipShape(RoundedRectangle(cornerRadius: 12))

            Divider()

            if let inst = data.institution {
                AIDataField(label: strings.labelInstitution, value: inst)
            }
            if let doc = data.doctorName {
                AIDataField(label: strings.labelDoctor, value: doc)
            }
            if let date = data.analysisDate {
                AIDataField(label: strings.labelAnalysisDate, value: date)
            }

            if !data.indicators.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text(String(format: String(localized: "offline_indicators_count"), data.indicators.count))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    VStack(spacing: 4) {
                        ForEach(Array(data.indicators.enumerated()), id: \.offset) { _, ind in
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
                    .background(Color(.secondarySystemBackground).opacity(0.5))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
        .padding(inDropdown ? 16 : 20)
        .background(inDropdown ? Color(.secondarySystemBackground) : Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: inDropdown ? 12 : 16))
    }
}

struct AIDataField: View {
    let label: String
    let value: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text(label)
                .font(.caption)
                .foregroundStyle(.secondary)
                .frame(width: 110, alignment: .leading)
            Text(value)
                .font(.subheadline)
        }
    }
}

// MARK: - Error Card
struct AIErrorCard: View {
    let message: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.triangle.fill")
                .foregroundStyle(.red)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.primary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemRed).opacity(0.15))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - Toast
struct AIToastView: View {
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
