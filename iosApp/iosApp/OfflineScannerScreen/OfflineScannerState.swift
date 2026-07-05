import SwiftUI
import UniformTypeIdentifiers
import Shared

enum OfflineFileType: String, CaseIterable {
    case image
    case png
    case pdf
    case txt
    case docx

    init?(uti: String) {
        switch uti {
        case UTType.image.identifier, UTType.jpeg.identifier:
            self = .image
        case UTType.png.identifier:
            self = .png
        case UTType.pdf.identifier:
            self = .pdf
        case UTType.plainText.identifier, UTType.text.identifier:
            self = .txt
        case "org.openxmlformats.wordprocessingml.document":
            self = .docx
        default:
            if uti.hasPrefix("image/") { self = .image; return }
            return nil
        }
    }

    var isImage: Bool { self == .image || self == .png }

    /// SF Symbol для превью типа файла.
    var iconName: String {
        switch self {
        case .pdf: return "doc.fill"
        case .txt: return "doc.text"
        case .docx: return "doc.richtext"
        default: return "photo"
        }
    }

    /// Локализованная подпись типа файла.
    var localizedLabel: String {
        switch self {
        case .pdf: return String(localized: "file_type_pdf")
        case .txt: return String(localized: "file_type_txt")
        case .docx: return String(localized: "file_type_docx")
        default: return String(localized: "file_type_image")
        }
    }
}

enum OfflineContentState: Equatable {
    case idle
    case loading
    case success(HousingPaymentDocument, processedText: String)
    case error(String)
}
