import SwiftUI
import Shared

// MARK: - Close Button (unified across all screens)

struct CloseButton: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Image(systemName: "xmark.circle.fill")
                .font(.system(size: 32))
                .symbolRenderingMode(.hierarchical)
                .foregroundStyle(.secondary)
        }
        .padding(8)
    }
}

// MARK: - File Type Placeholder

struct OfflineFileTypePlaceholder: View {
    let fileType: OfflineFileType

    private var iconColor: Color {
        fileType == .pdf ? Color(.systemRed) : Color(.tertiaryLabel)
    }

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: fileType.iconName)
                .font(.system(size: 48))
                .foregroundStyle(iconColor)
            Text(fileType.localizedLabel)
                .font(.subheadline)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity)
        .frame(height: 200)
    }
}

// MARK: - Loading View (matches Android pipeline steps)

struct OfflineLoadingView: View {
    private let strings = OfflineScannerStrings()

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            ProgressView()
                .frame(maxWidth: .infinity)
                .padding(.vertical, 4)
            OfflinePipelineStep(label: strings.loadingOcr, done: true)
            OfflinePipelineStep(label: strings.loadingCleanup, done: true)
            OfflinePipelineStep(label: strings.pipelineExtract, done: false)
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
    }
}

private struct OfflinePipelineStep: View {
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

// MARK: - Error Card (matches Android errorContainer style)

struct OfflineErrorCard: View {
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

// MARK: - Expandable Result Card (matches Android OfflineSuccessCard)

struct ExpandableResultCard: View {
    let data: HousingPaymentDocument
    @State private var expanded = true
    private let strings = OfflineScannerStrings()

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
                OfflineExpandedContent(data: data)
            }
        }
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .shadow(color: .black.opacity(0.06), radius: 8, y: 2)
    }
}

private struct OfflineExpandedContent: View {
    let data: HousingPaymentDocument
    private let strings = OfflineScannerStrings()

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
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
                OfflineMetaRow(title: strings.labelProvider, value: inst)
            }
            if let period = data.documentDate {
                OfflineMetaRow(title: strings.labelBillingPeriod, value: period)
            }
            if let account = data.personalAccountNumber {
                OfflineMetaRow(title: strings.labelPersonalAccount, value: account)
            }
            if let address = data.propertyAddress {
                OfflineMetaRow(title: strings.labelPropertyAddress, value: address)
            }
            if let amount = data.amountDueForPeriod {
                OfflineMetaRow(title: strings.labelAmountDue, value: "\(amount) ₽")
            }

            if let services = data.serviceLines, !services.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text(strings.servicesCount(services.count))
                        .font(.caption)
                        .foregroundStyle(Color(.secondaryLabel))
                    VStack(spacing: 4) {
                        ForEach(services.indices, id: \.self) { i in
                            let line = services[i]
                            HStack(alignment: .top) {
                                VStack(alignment: .leading, spacing: 2) {
                                    Text(line.name)
                                        .font(.subheadline)
                                        .foregroundStyle(Color(.secondaryLabel))
                                    if let tariff = line.tariff {
                                        Text(strings.tariffValue(tariff))
                                            .font(.caption2)
                                            .foregroundStyle(Color(.tertiaryLabel))
                                    }
                                }
                                .frame(maxWidth: .infinity, alignment: .leading)
                                Text("\(line.amountToPay) ₽")
                                    .font(.subheadline)
                                    .fontWeight(.medium)
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

struct OfflineMetaRow: View {
    let title: String
    let value: String

    var body: some View {
        HStack(alignment: .top, spacing: 8) {
            Text(title)
                .font(.caption)
                .foregroundStyle(Color(.secondaryLabel))
                .frame(width: 110, alignment: .leading)
            Text(value)
                .font(.subheadline)
        }
    }
}

// MARK: - Toast (bottom, framed with rounded corners)

struct OfflineToastView: View {
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
