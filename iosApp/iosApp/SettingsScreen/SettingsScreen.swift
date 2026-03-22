import SwiftUI

/// Settings screen — design matches Android SettingsScreen.
struct SettingsScreen: View {
    @StateObject private var viewModel = SettingsViewModel()
    private let strings = SettingsStrings()

    /// Радиус скругления поля ввода ключа (можно менять)
    private let apiKeyFieldCornerRadius: CGFloat = 12
    /// Высота поля ввода ключа
    private let apiKeyFieldHeight: CGFloat = 56

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 0) {
                // Header
                VStack(alignment: .leading, spacing: 4) {
                    Text(strings.screenTitle)
                        .font(.largeTitle)
                        .fontWeight(.bold)
                    Text(strings.subtitle)
                        .font(.subheadline)
                        .foregroundStyle(.secondary)
                }
                .padding(.horizontal, 24)
                .padding(.top, 24)
                .padding(.bottom, 8)

                VStack(alignment: .leading, spacing: 16) {  
                    // Section: API key
                    HStack(spacing: 8) {
                        Image(systemName: "key")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                        Text(strings.sectionApiKey)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }

                    // Key saved state
                    if viewModel.hasSavedKey {
                        HStack(spacing: 10) {
                            Image(systemName: "checkmark.circle.fill")
                                .foregroundStyle(.green)
                            VStack(alignment: .leading, spacing: 2) {
                                Text(strings.keySaved)
                                    .font(.subheadline)
                                    .fontWeight(.medium)
                                Text(viewModel.maskedKey)
                                    .font(.caption)
                                    .foregroundStyle(.secondary)
                            }
                            Spacer()
                            Button(strings.deleteKey) {
                                viewModel.clearSavedKey()
                            }
                            .foregroundStyle(.red)
                        }
                        .padding(.horizontal, 16)
                        .padding(.vertical, 16)
                        .background(Color.accentColor.opacity(0.15))
                        .clipShape(RoundedRectangle(cornerRadius: 12))
                    }

                    // Key input (when no key or adding new)
                    if !viewModel.hasSavedKey {
                        VStack(alignment: .leading, spacing: 8) {
                            Group {
                                if viewModel.keyVisible {
                                    TextField(strings.apiKeyLabel, text: $viewModel.keyDraft)
                                        .autocapitalization(.none)
                                        .autocorrectionDisabled()
                                } else {
                                    SecureField(strings.apiKeyPlaceholder, text: $viewModel.keyDraft)
                                }
                            }
                            .padding(.leading, 16)
                            .padding(.trailing, 44)
                            .padding(.vertical, 16)
                            .frame(minHeight: apiKeyFieldHeight)
                            .background(Color(.secondarySystemFill))
                            .clipShape(RoundedRectangle(cornerRadius: apiKeyFieldCornerRadius))
                            .overlay(alignment: .trailing) {
                                Button {
                                    viewModel.keyVisible.toggle()
                                } label: {
                                    Image(systemName: viewModel.keyVisible ? "eye.slash" : "eye")
                                        .font(.body)
                                        .foregroundStyle(.secondary)
                                }
                                .padding(.trailing, 8)
                            }
                            if viewModel.isKeyModified {
                                Button {
                                    viewModel.saveApiKey()
                                } label: {
                                    HStack(spacing: 8) {
                                        Image(systemName: "square.and.arrow.down")
                                        Text(strings.saveKey)
                                    }
                                    .frame(maxWidth: .infinity)
                                    .padding(.vertical, 12)
                                }
                                .buttonStyle(.borderedProminent)
                                .padding(.top, 12)
                            }
                        }
                    }

                    // Key info card
                    if viewModel.hasSavedKey {
                        SettingsKeyInfoCard(
                            loading: viewModel.keyInfoLoading,
                            error: viewModel.keyInfoError,
                            keyInfo: viewModel.keyInfo,
                            onRefresh: { viewModel.loadKeyInfo() }
                        )
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 16)

                Spacer(minLength: 24)
            }
        }
        .background(Color(.systemGroupedBackground))
        .onAppear {
            if viewModel.hasSavedKey {
                viewModel.loadKeyInfo()
            }
        }
        .overlay {
            if let msg = viewModel.toastMessage {
                SettingsToastView(message: msg)
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                    .onAppear {
                        DispatchQueue.main.asyncAfter(deadline: .now() + 2) {
                            withAnimation { viewModel.dismissToast() }
                        }
                    }
            }
        }
    }
}

struct SettingsKeyInfoCard: View {
    let loading: Bool
    let error: String?
    let keyInfo: SettingsViewModel.KeyInfo?
    let onRefresh: () -> Void
    private let strings = SettingsStrings()

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack {
                Image(systemName: "info.circle")
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(strings.keyStatus)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer()
                Button(action: onRefresh) {
                    Image(systemName: "arrow.clockwise")
                        .font(.caption)
                }
            }

            if loading {
                ProgressView()
                    .scaleEffect(0.8)
            } else if let err = error {
                Text(err)
                    .font(.caption)
                    .foregroundStyle(.red)
                    .padding(10)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(Color(.systemRed).opacity(0.15))
                    .clipShape(RoundedRectangle(cornerRadius: 8))
            } else if let info = keyInfo {
                HStack {
                    VStack {
                        Text(info.isFreeTier ? strings.tariffFree : strings.tariffPaid)
                            .font(.subheadline)
                            .fontWeight(.medium)
                        Text(strings.tariffLabel)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                    Spacer()
                    VStack {
                        Text(String(format: "$%.4f", info.usage))
                            .font(.subheadline)
                            .fontWeight(.medium)
                        Text(strings.spentLabel)
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .padding(16)
        .background(Color(.systemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(Color(.systemGray4), lineWidth: 1)
        )
    }
}

struct SettingsToastView: View {
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

#Preview {
    SettingsScreen()
}
