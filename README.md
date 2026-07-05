# DocHint — Compose Multiplatform

[![Kotlinx Serialization](https://img.shields.io/badge/Kotlinx%20Serialization-1.10.0-00C853?style=flat-square)](https://github.com/Kotlin/kotlinx.serialization)
[![Ktor Client](https://img.shields.io/badge/Ktor%20Client-3.0.3-087CFA?style=flat-square)](https://ktor.io/)
[![SQLDelight](https://img.shields.io/badge/SQLDelight-2.0.2-7C4DFF?style=flat-square)](https://cashapp.github.io/sqldelight/)
[![Koin](https://img.shields.io/badge/Koin-4.0.0-6C63FF?style=flat-square)](https://insert-koin.io/)
[![ONNX Runtime](https://img.shields.io/badge/ONNX%20Runtime-1.23.0-000000?style=flat-square&logo=onnx&logoColor=white)](https://onnxruntime.ai/)
[![OpenRouter](https://img.shields.io/badge/OpenRouter-gpt--4o--mini-412991?style=flat-square)](https://openrouter.ai/)

## Требования к сборке

[![Kotlin 2.3.0](https://img.shields.io/badge/Kotlin-2.3.0-blue?style=flat-square)](https://kotlinlang.org/)&nbsp;
[![Compose Multiplatform 1.10.3](https://img.shields.io/badge/Compose%20Multiplatform-1.10.3-purple?style=flat-square)](https://github.com/JetBrains/compose-multiplatform)&nbsp;
[![Android Gradle Plugin 9.1.0](https://img.shields.io/badge/Android%20Gradle%20Plugin-9.1.0-green?style=flat-square)](https://developer.android.com/studio/releases/gradle-plugin)&nbsp;
[![Gradle 9.3.1 (Wrapper)](https://img.shields.io/badge/Gradle-9.3.1-02303A?style=flat-square&logo=gradle&logoColor=white)](https://gradle.org/)&nbsp;
[![JDK 17](https://img.shields.io/badge/JDK-17-orange?style=flat-square)](https://openjdk.org/projects/jdk/17/)

## Возможности

`DocHint` — мобильное приложение для **Android** и **iOS**, которое распознаёт и разбирает **квитанции ЖКУ** (платёжные документы за коммунальные услуги): извлекает текст из фото, PDF, DOCX и TXT, структурирует данные и сохраняет их локально.

Приложение поддерживает 4 основных экрана:

- **AI Сканер** — OCR + разбор через LLM (OpenRouter, `gpt-4o-mini`)
- **Оффлайн** — локальный OCR и rule-based парсер без интернета
- **Документы** — список сохранённых квитанций, импорт по коду, шаринг
- **Настройки** — хранение API-ключа OpenRouter

### Ключевые технологии (кроме бейджей выше)

- **Compose Multiplatform** — UI на Android (`:composeApp`); на iOS — **SwiftUI** + общий KMP-модуль `:shared`.
- **Clean Architecture** — модули `:domain`, `:data`, `:shared`, `:composeApp`, `:androidApp`, `:iosApp`.
- **SQLDelight** — локальное хранилище распознанных квитанций (`HousingResult`).
- **Paddle OCR (ONNX Runtime)** — офлайн-распознавание текста на Android; на iOS — **Vision** и **PDFKit**.
- **HousingBillParser** — rule-based парсер полей квитанции (УК, ЛС, услуги, суммы).
- **Ktor Client** — HTTP к OpenRouter и к backend-шарингу документов.

## Архитектура модулей

| Модуль | Назначение |
|--------|------------|
| `:domain` | Модели (`HousingPaymentDocument`), use cases, контракты репозиториев |
| `:data` | SQLDelight, парсеры, OpenRouter-клиент, share API, Android OCR |
| `:shared` | KMP framework `Shared` для iOS (Koin, контроллеры экранов) |
| `:composeApp` | Compose UI, ViewModel, навигация (Android) |
| `:androidApp` | Android application entry point |
| `:iosApp` | SwiftUI-оболочка, нативный OCR и OpenRouter на Swift |

## Сценарии распознавания

### AI Сканер

`выбор файла (IMAGE / PNG / PDF) -> OCR -> очистка текста -> OpenRouter (JSON) -> проверка дубликата -> сохранение`

API-ключ OpenRouter задаётся в **Настройках** и хранится:
- Android — `EncryptedSharedPreferences`
- iOS — Keychain

### Оффлайн сканер

`выбор файла (IMAGE / PNG / PDF / TXT / DOCX) -> извлечение текста -> HousingBillParser -> проверка дубликата -> сохранение`

На Android для PDF: нативный текст (PDFBox) + OCR по страницам; выбирается лучший вариант.  
На iOS текст извлекается нативно (Vision / PDFKit), парсинг и сохранение — через KMP `OfflineScannerController`.

## Хранение данных

Квитанции хранятся локально в **SQLDelight**, таблица **`HousingResult`** (см. `data/src/commonMain/sqldelight/.../HousingResult.sq`):

| Поле | Назначение |
|------|------------|
| `documentType` / `documentDate` | Тип и период документа |
| `source` | `ai`, `offline`, `shared` или `unknown` |
| `textFingerprint` | отпечаток текста для защиты от дубликатов |
| `payload` | JSON профиля квитанции (`HousingPaymentDocument`) |
| `createdAt` | метка времени сохранения |

Вкладки на экране **Документы**:
- **Мои** — локально сохранённые квитанции
- **Полученные** — импортированные по коду с backend

## Шаринг документов (share API)

Выбранные квитанции отправляются на backend:

- `POST /share` — создать одноразовый код
- `GET /share/{code}` — получить пакет документов

Формат обмена — `ShareEnvelope` с `messageType: housing_document_export` и `kind: housing_bill`.

## Конфигурация

### OpenRouter (AI Сканер)

- Ключ задаётся в приложении на экране **Настройки**.
- Модель по умолчанию: `gpt-4o-mini` (`OpenRouterClient.MODEL_TEXT`).

### Backend для шаринга

- **Android** — базовый URL в `ShareNetworkConfig.android.kt` (для локальной разработки можно заменить на `http://<LAN-IP>:8081`).
- **iOS** — переменная окружения `DOCHINT_SHARE_BASE_URL` (в Xcode Scheme → Run → Environment Variables); на симуляторе backend обычно доступен как `http://localhost:8081` на Mac.

### Запуск локально

#### Android

```bash
./gradlew :androidApp:assembleDebug
