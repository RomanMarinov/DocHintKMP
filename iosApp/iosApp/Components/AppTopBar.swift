import SwiftUI

/// Компактная верхняя панель: тот же фон, что и у экранов с `systemGroupedBackground`.
enum AppTopBarMetrics {
    static let horizontalPadding: CGFloat = 16
}

struct AppTopBar<Trailing: View>: View {
    let title: String
    var subtitle: String?
    @ViewBuilder var trailing: () -> Trailing

    init(title: String, subtitle: String? = nil, @ViewBuilder trailing: @escaping () -> Trailing = { EmptyView() }) {
        self.title = title
        self.subtitle = subtitle
        self.trailing = trailing
    }

    private var barBackground: Color {
        Color(.systemGroupedBackground)
    }

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .center, spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.title2)
                        .fontWeight(.bold)
                        .lineLimit(1)
                    if let subtitle, !subtitle.isEmpty {
                        Text(subtitle)
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)

                trailing()
            }
            .padding(.horizontal, AppTopBarMetrics.horizontalPadding)
            .padding(.top, 8)
            .padding(.bottom, 8)

            Divider()
                .opacity(0.35)
        }
        .background {
            barBackground
                .ignoresSafeArea(edges: .top)
        }
    }
}
