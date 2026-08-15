# FamilyOS

Production-ready Android family operating system — shared shopping, tasks, calendar, budget, encrypted documents, notes, chat, notifications, Family AI, and Premium billing.

**Works offline by default.** Optional Firebase for cloud sync.

## Requirements

- Android Studio Ladybug / Narwhal (or newer) with AGP 8.9+
- JDK 17
- Android SDK 35, minSdk 29 (Android 10+)
- Optional: Firebase project (Auth, Firestore, Storage, Cloud Messaging) for cloud sync
- Google Play Console app with Billing products (for Premium)

## Open in Android Studio

1. Clone / open `d:\Projects\Develop\FamilyOS`
2. **File → Open** the project root (the folder that contains `settings.gradle.kts`)
3. Let Gradle sync (Version Catalog + `buildSrc`)
4. Run the `app` configuration — email/password auth works immediately in local mode
5. (Optional) Add Firebase + API keys for cloud sync (below)

## Auth modes

| Mode | When | What works |
|---|---|---|
| **Local (default)** | Placeholder / missing Firebase config | Email/password sign-up, sign-in, logout, delete account, remember-me, auto-login, password reset — all on-device via Room + PBKDF2 |
| **Cloud** | Real `google-services.json` | Firebase Auth + Google Sign-In + Firestore sync |

Local credentials and session survive app restarts when **Remember me** is enabled. Signup auto-creates a family named `{displayName}'s Family`.

## Firebase setup (optional cloud)

Auth uses **local Room** until you replace the placeholder Firebase config. With a stub `google-services.json`, the app shows an info banner: «Локальный режим — данные на устройстве» — email/password still works.

### Steps (for cloud sync / Google Sign-In)

1. Open [Firebase Console](https://console.firebase.google.com/) → create / select a project.
2. Add an Android app with package name:
   - release: `com.familyos.app`
   - debug APK: also add `com.familyos.app.debug` (same project)
3. Download the real `google-services.json` and replace:

```text
app/google-services.json
```

4. Authentication → Sign-in method → enable:
   - **Email/Password**
   - **Google**
5. For Google Sign-In, copy the **Web client ID** (`client_type: 3`, ends with `.apps.googleusercontent.com`) from the JSON `oauth_client` section, or from Google Cloud Console → Credentials.
6. Optional: put it in `local.properties`:

```properties
GOOGLE_WEB_CLIENT_ID=123456789-xxxx.apps.googleusercontent.com
```

7. Rebuild / reinstall the app (`./gradlew :app:assembleDebug`).

SHA-1 for Google Sign-In (debug):

```bash
keytool -list -v -alias androiddebugkey -keystore %USERPROFILE%\.android\debug.keystore -storepass android -keypass android
```

Add that SHA-1 in Firebase → Project settings → Your Android app.

### Quick check

After a real JSON is in place, `BuildConfig.FIREBASE_CONFIGURED` becomes `true`, local-mode banner disappears, and HybridAuth prefers Firebase.

Also enable in the same Firebase project:

- Cloud Firestore
- Firebase Storage
- Cloud Messaging

Deploy security rules:

```bash
firebase deploy --only firestore:rules,storage
```

Rules files: `firestore/firestore.rules`, `firestore/storage.rules`.


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
