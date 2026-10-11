# M Rupee — Loan Application (Web + Android APK)

A single-page loan app that reads customer/loan records from a **published Google
Sheets CSV link** and shows the repayment flow, plus a native **Android APK**
wrapper around the exact same web app.

- **Live web app (GitHub Pages):** https://stevedfd123.github.io/loan-application-/
- **Published data source (CSV):**
  https://docs.google.com/spreadsheets/d/e/2PACX-1vSqEJvs5Tnz2x1oCo5xOURlss-qdpBeF8Ew2-7wllFw17LU_jaxJ0YF-Gw8ncqUpAru6LfK25HlL__U/pub?output=csv

## Data flow

```
Google Sheet ──(published as CSV)──▶ index.html fetches CSV_URL ──▶ login by mobile ──▶ repayment screens
                                           │
                                           └── copied into the APK at build time (android/app/src/main/assets/www/)
```

The CSV is parsed in the browser. The first column that holds a mobile number is
used as the lookup key (last 10 digits), and these columns are mapped onto the
screens:

| CSV column | Used for |
|---|---|
| `Mobile` | login lookup key |
| `Name` | Order Details → Name |
| `Loan Amount` | Order Details → Loan Amount |
| `Total repayment amount` | My Account + Order Details + Payment amount |
| `Late Fee` | Order Details → Late Fee |
| `Due Date` | My Account + Order Details → Due Date |
| `Days overdue` | Overdue badge / Overdue Days |
| `Application date` | Order Details → Application Date |
| `tenure` | Order Details → Tenure |
| `VPA/UPI` | Payment → VPA/UPI (the payee address) |

## Web app

`index.html` is the entire app — no build step, no dependencies. Publish it and
it works. To point it at a different sheet, change `CSV_URL` near the top of the
`<script>` block.

## Android APK

`android/` is a minimal Gradle project that loads `index.html` in a full-screen
WebView, so the HTML/CSS/JS remains the single source of truth. On every build a
Gradle task copies the repo's `index.html` into the APK's assets, so the APK and
the website can never drift apart.

The native layer also intercepts the CSV request and downloads it directly, so
the sheet loads regardless of CORS headers (a `file://` page has a `null` origin).

### Build the APK

The APK is built automatically by GitHub Actions on every push to this branch:

```
.github/workflows/build-apk.yml  →  gradle -p android assembleDebug assembleRelease
```

Download it from the **Artifacts** section of the run
(`loan-app-release.apk`, `loan-app-debug.apk`).

To build it yourself:

```bash
cd android
gradle assembleDebug          # or: gradle assembleRelease
# → app/build/outputs/apk/debug/app-debug.apk
```

Requires a JDK 17 and an Android SDK (platform 34, build-tools 34).

### Install

Copy the `.apk` to the phone, tap it, and allow "install unknown apps".
The release build is signed with the debug keystore so it installs immediately —
replace `signingConfigs.release` in `android/app/build.gradle.kts` with your own
keystore before distributing publicly.

## Notes

- The login screen generates a **random 4-digit OTP and shows it on screen**
  (see `generatedOTP` in `index.html`) — there is no SMS backend. Swap that for a
  real OTP service before real use.
- `Days overdue > 0` switches the status badge to **Overdue**.
