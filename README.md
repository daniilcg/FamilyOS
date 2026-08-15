# FamilyOS

Production-ready Android family operating system — shared shopping, tasks, calendar, budget, encrypted documents, notes, chat, notifications, Family AI, and Premium billing.

## Requirements

- Android Studio Ladybug / Narwhal (or newer) with AGP 8.9+
- JDK 17
- Android SDK 35, minSdk 29 (Android 10+)
- Firebase project (Auth, Firestore, Storage, Cloud Messaging)
- Google Play Console app with Billing products (for Premium)

## Open in Android Studio

1. Clone / open `d:\Projects\Develop\FamilyOS`
2. **File → Open** the project root (the folder that contains `settings.gradle.kts`)
3. Let Gradle sync (Version Catalog + `buildSrc`)
4. Add Firebase + API keys (below)
5. Run the `app` configuration on a device / emulator with Google Play services

## Firebase setup

1. Create a Firebase project and add an Android app with application id `com.familyos.app`
2. Download `google-services.json`
3. Place it at:

```text
app/google-services.json
```

4. Enable:
   - Authentication → Email/Password + Google
   - Cloud Firestore
   - Firebase Storage
   - Cloud Messaging
5. Deploy rules from this repo:

```bash
firebase deploy --only firestore:rules,storage
```

Rules files:

- `firestore/firestore.rules`
- `firestore/storage.rules`

## Local secrets (`local.properties`)

Create / edit `local.properties` in the project root (never commit secrets):

```properties
sdk.dir=C\:\\Users\\YOU\\AppData\\Local\\Android\\Sdk

# Family AI providers (read into feature_ai BuildConfig)
AI_OPENAI_KEY=sk-...
AI_GEMINI_KEY=AIza...
AI_OPENROUTER_KEY=sk-or-...

# Optional Google Sign-In web client id for app module
GOOGLE_WEB_CLIENT_ID=....apps.googleusercontent.com
```

`feature_ai` maps these into:

- `BuildConfig.AI_OPENAI_KEY`
- `BuildConfig.AI_GEMINI_KEY`
- `BuildConfig.AI_OPENROUTER_KEY`

Keys can also be overridden at runtime via DataStore (`AiKeyStore`). The active provider is switchable in settings / Family AI (`openai` | `gemini` | `openrouter`).

## Google Play Billing products

In Play Console → Monetize → Subscriptions create:

| Product ID | Type | Purpose |
|---|---|---|
| `familyos_premium_monthly` | Subscription | Monthly Premium |
| `familyos_premium_yearly` | Subscription | Yearly Premium |

Wire the app with a licensed tester account. The Billing Library 7 client lives in `feature_billing` (`BillingRepositoryImpl`).

### Entitlements

| Tier | Members | Families | Storage | AI | Analytics | PDF/Excel export |
|---|---|---|---|---|---|---|
| FREE | 5 | 1 | 2 GB | No | Basic | No |
| PREMIUM | Unlimited | Unlimited | 50 GB | Yes | Advanced | Yes |

Gatekeeping is centralized in `PremiumAccessControl`.

## Module map

| Module | Package | Responsibility |
|---|---|---|
| `app` | `com.familyos.app` | Application shell, navigation host, Hilt |
| `core` | `com.familyos.core` | Dispatchers, logging, extensions |
| `core_domain` | `com.familyos.core.domain` | Models, repositories, use cases |
| `core_data` | `com.familyos.core.data` | Room, Firestore, cipher, sync |
| `core_ui` | `com.familyos.core.ui` | Theme + shared Compose UI |
| `feature_documents` | `com.familyos.feature.documents` | AES-256 vault, PIN/biometric |
| `feature_notes` | `com.familyos.feature.notes` | Notes, checklists, tags, archive |
| `feature_chat` | `com.familyos.feature.chat` | Family chat, voice, receipts |
| `feature_notifications` | `com.familyos.feature.notifications` | In-app notification center |
| `feature_ai` | `com.familyos.feature.ai` | Family AI providers + prompts |
| `feature_billing` | `com.familyos.feature.billing` | Play Billing 7 + exports |

## Family AI quick prompts

- Shopping from recipe: `Borscht for 6`
- Task set from goal: `Prepare birthday party`
- Budget plan: `Family of 4, monthly budget 1200 EUR`
- Trip checklist: `Weekend trip for 5 days`

Structured JSON responses are parsed into domain actions and applied via `ApplyAiShoppingListUseCase` / `ApplyAiTaskSetUseCase`.

## Documents vault

Supported uploads: **PDF, DOCX, JPG, PNG, WEBP**  
Types: Passport, Insurance, Warranty, Contract, Certificate, Medical, Other  
Encryption: **AES-256-GCM** via `DocumentCipher` / `AesDocumentCipher` (Android Keystore)  
Access: PIN + Biometric lock screens before list/detail/import

## Architecture

Kotlin · Jetpack Compose · Material 3 · MVVM · Clean Architecture · Hilt · Coroutines/Flow · Room · Firestore · DataStore · Paging 3 · WorkManager · Coil · Kotlin Serialization · OkHttp

## License

Proprietary — FamilyOS.
