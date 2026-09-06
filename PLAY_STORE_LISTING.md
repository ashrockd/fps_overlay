# Play Store publishing checklist

Everything in this repo (code, icons, signing pipeline, privacy policy) is
ready for a Play Store submission. The rest is manual work inside the
[Play Console](https://play.google.com/console) — a Google account and a
one-time $25 registration fee are required, and I can't do these steps for
you (they need your account/payment).

## What's already done here
- [x] `compileSdk`/`targetSdk` 35 (current Play Store requirement)
- [x] Adaptive launcher icon (API 26+) + legacy/round fallback icons (API 24-25)
- [x] R8 minify + resource shrinking enabled for release builds
- [x] Release signing wired to GitHub Actions secrets (see `README.md`)
- [x] CI builds a signed `.aab` (Android App Bundle) — the format Play
      Store requires for new apps
- [x] No network access, no ads/analytics SDKs. Uses `SYSTEM_ALERT_WINDOW`
      (draw over other apps) + a foreground service so the overlay stays
      visible while using other apps.
- [x] Privacy policy page in `docs/privacy-policy.html`
- [x] Store icon (512x512) and feature graphic (1024x500) in `store_assets/`

## What you still need to do in Play Console
1. **Create a developer account** (one-time $25 fee) if you don't have one.
2. **Create the app** → choose "App" (not game), free, and your country.
3. **Upload the release `.aab`** downloaded from the GitHub Actions artifact
   (`fps-overlay-release-aab`) to a Production (or Internal Testing) release.
4. **Store listing**:
   - App name: `FPS Overlay`
   - Short description (suggested): "Shows your screen's live frame rate in a draggable, customizable overlay."
   - Full description (suggested):
     > FPS Overlay is a bare-minimum utility that displays your device's
     > current screen frame rate in a small, draggable label. Customize
     > the font, font size, and color to your liking. No permissions,
     > no ads, no data collection — just a frame-rate readout.
   - Upload `store_assets/play_store_icon_512.png` as the app icon.
   - Upload `store_assets/play_store_feature_graphic_1024x500.png` as the
     feature graphic.
   - You'll still need 2+ **phone screenshots** — run the app on a device
     or emulator and capture a couple.
5. **Privacy policy URL**: once GitHub Pages is enabled for this repo
   (Settings → Pages → Source: `main` branch, `/docs` folder), the policy
   will be at `https://<your-github-username>.github.io/<repo-name>/privacy-policy.html`.
   Paste that URL into the Play Console "App content" → "Privacy policy" field.
6. **Data safety form**: answer "No" to data collection/sharing — this app
   collects nothing (see the privacy policy for the exact wording).
6a. **Permissions declaration form / "Draw over other apps" declaration**:
   Play Console has a separate policy questionnaire for apps requesting
   `SYSTEM_ALERT_WINDOW`. When it asks for a justification, use something
   like: "Core functionality — the app's sole purpose is displaying a
   live FPS counter on top of other apps and games. Without this
   permission the overlay could only be shown while this app itself is in
   the foreground, defeating the app's purpose."
7. **Content rating questionnaire**: answer honestly; this app has no
   objectionable content and should get the lowest rating tier (e.g. "Everyone").
8. **Target audience**: not designed for children.
9. **App access**: no login required, full functionality is available
   immediately — declare "all functionality available without restrictions".
10. Submit for review.

## Note on the debug APK
The `fps-overlay-debug-apk` artifact is signed with the Android debug key
and is meant for sideloading/testing on your own device only. Play Store
requires the signed release `.aab` from `fps-overlay-release-aab`.
