# FPS Overlay

A bare-minimum Android app that shows the device's current screen frame
rate (measured via `Choreographer` vsync callbacks) in a draggable,
customizable label at the top-left of the app's window. Font size, font,
and color are all user-configurable via the settings (gear) button.

No permissions, no network access, no ads, no analytics.

## Building locally

This project has no Gradle wrapper committed (see below); use a system
Gradle install (8.7+) with JDK 17:

```bash
gradle assembleDebug
```

The debug APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

## CI builds (GitHub Actions)

Every push to `main` (or manual "Run workflow") triggers
[`.github/workflows/build.yml`](.github/workflows/build.yml), which:

1. Builds a **debug APK** (always) → uploaded as the `fps-overlay-debug-apk`
   artifact. Sideload this on your own device for quick testing.
2. Builds a **signed release AAB** (only if the signing secrets below are
   configured) → uploaded as the `fps-overlay-release-aab` artifact. This
   is the file to upload to Google Play.

Download artifacts from the workflow run's **Summary** page under
**Artifacts**, or via `gh run download`.

### Release signing secrets

The release build reads signing config from environment variables, which
CI supplies from repository secrets (nothing is ever committed to git):

| Secret | Contents |
|---|---|
| `ANDROID_KEYSTORE_BASE64` | base64-encoded `.jks` keystore file |
| `ANDROID_KEYSTORE_PASSWORD` | keystore password |
| `ANDROID_KEY_ALIAS` | key alias inside the keystore |
| `ANDROID_KEY_PASSWORD` | key password |

**Keep your own copy of the keystore file and passwords somewhere safe
(password manager). GitHub secrets are write-only — if you lose your own
copy, it cannot be recovered from GitHub, and you will never be able to
publish an update to the same Play Store listing again.**

## Play Store publishing

See [`PLAY_STORE_LISTING.md`](PLAY_STORE_LISTING.md) for the full
publishing checklist (what's already done vs. what you still do manually
in Play Console).

## Privacy policy

Hosted from this repo via GitHub Pages: [`docs/privacy-policy.html`](docs/privacy-policy.html).
Once Pages is enabled (Settings → Pages → `main` branch, `/docs` folder),
it will be live at `https://<username>.github.io/<repo>/privacy-policy.html`.
