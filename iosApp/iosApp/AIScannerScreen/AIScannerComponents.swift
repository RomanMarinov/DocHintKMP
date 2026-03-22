import SwiftUI
import Shared

// MARK: - Loading Card (matches Android pipeline steps)
struct AILoadingCard: View {
    private let strings = AIScannerStrings()

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ProgressView()
                .scaleEffect(1)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 4)
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
                .foregroundStyle(done ? Color.primary : Color(.tertiaryLabel))
        }
        .padding(.vertical, 4)
    }
}

// MARK: - Expandable Success Card (matches Android AiSuccessCard)
struct AIExpandableResultCard: View {
    let data: AIMedicalData
    @State private var expanded = false
    private let strings = AIScannerStrings()

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            Button(action: { withAnimation(.easeInOut(duration: 0.25)) { expanded.toggle() } }) {
                HStack(spacing: 12) {
                    ZStack {
                        Circle()
                            .fill(Color(.tertiarySystemFill))
                            .frame(width: 32, height: 32)
                        Image(systemName: "checkmark.circle.fill")
                            .font(.system(size: 20))
                            .foregroundStyle(Color.accentColor.opacity(0.8))
                    }
                    Text(strings.viewResult)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundStyle(Color.accentColor)
                    Spacer()
                    Image(systemName: expanded ? "chevron.up" : "chevron.down")
                        .font(.system(size: 14, weight: .medium))
                        .foregroundStyle(Color.secondary)
                }
                .padding(16)
                .frame(maxWidth: .infinity, alignment: .leading)
                .contentShape(Rectangle())
            }
            .buttonStyle(.plain)

            if expanded {
                AIExpandedContent(data: data)
            }
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
    }
}

private struct AIExpandedContent: View {
    let data: AIMedicalData
    private let strings = AIScannerStrings()

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Document type row
            HStack {
                Text(strings.labelDocumentType)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundStyle(Color(.secondaryLabel))
                Spacer()
                Text(data.documentType)
                    .font(.subheadline)
                    .fontWeight(.medium)
                    .foregroundStyle(Color.primary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Color(.tertiarySystemFill).opacity(0.5))
            .clipShape(RoundedRectangle(cornerRadius: 12))

            Divider().background(Color(.separator))

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
                        .foregroundStyle(Color(.secondaryLabel))
                    VStack(spacing: 4) {
                        ForEach(Array(data.indicators.enumerated()), id: \.offset) { _, ind in
                            HStack(alignment: .top) {
                                Text(ind.name)
                                    .font(.subheadline)
                                    .foregroundStyle(Color(.secondaryLabel))
                                    .frame(maxWidth: .infinity, alignment: .leading)
                                Text(ind.value)
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                if let ref = ind.referenceRange {
                                    Text(ref)
                                        .font(.caption2)
                                        .foregroundStyle(Color(.tertiaryLabel))
                                }
                            }
                        }
                    }
                    .padding(12)
                    .background(Color(.secondarySystemBackground).opacity(0.5))
                    .clipShape(RoundedRectangle(cornerRadius: 12))
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.bottom, 16)
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

// MARK: - Error Card (matches Android errorContainer style)
struct AIErrorCard: View {
    let message: String

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "exclamationmark.circle")
                .font(.system(size: 20))
                .foregroundStyle(.red)
            Text(message)
                .font(.subheadline)
                .foregroundStyle(.primary)
        }
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemRed).opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: 16))
    }
}

// MARK: - Toast (bottom, framed with rounded corners)
struct AIToastView: View {
    let message: String

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: "checkmark.circle.fill")
                .font(.system(size: 20))
                .foregroundStyle(Color.accentColor)
            Text(message)
                .font(.subheadline)
                .fontWeight(.medium)
                .foregroundStyle(Color.primary)
        }
        .padding(.horizontal, 20)
        .padding(.vertical, 14)
        .background(
            RoundedRectangle(cornerRadius: 16)
                .fill(Color(.systemGray6).opacity(0.98))
                .overlay(
                    RoundedRectangle(cornerRadius: 16)
                        .stroke(Color(.systemGray4), lineWidth: 0.5)
                )
                .shadow(color: .black.opacity(0.15), radius: 12, y: 4)
        )
        .padding(.horizontal, 24)
        .frame(maxWidth: .infinity)
        .padding(.bottom, 40)
    }
}
