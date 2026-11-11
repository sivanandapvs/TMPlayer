# Veylora development handoff

## Scope by platform

- Android phone and TV: use `com.veylora`, retain private app data isolation, and expose Liquid AMOLED separately from Pure Black. TV focus and playback require device checks.
- Desktop: share the new appearance choice and glass surface token. Package identity does not apply.
- Web: Android package identity and Compose appearance do not apply. Telegram request changes must preserve the existing shared core API.

## Progress

- Confirmed the target branch started clean and read the repository build, architecture, and Telegram terms instructions.
- Changed the Android application ID to `com.veylora`, disabled ABI splits, and set the release ABI default to all four ABIs with matching native libraries. Existing provider authorities use `${applicationId}`.
- Added a distinct Liquid AMOLED choice and shared floating surface styling. The existing Pure Black setting and its styling remain separate.
- Rebuilt `:app:assembleDebug` after review. The APK is at `Build-Output/Veylora.apk`. Verified package, label, SDK levels, four ABIs, and debug signature with Android build tools.
- `PureBlackTest` passes. The full core JVM suite reported five failures in scale timing and Windows cache directory tests; these require investigation.
- A review found that the phone's fourth theme segment needed a short display label, while its description and TV label retain the full name. The desktop Home renderer also needed TorBox and Unified branches; `:desktop:compileKotlin` now passes.

## Remaining work

- Implement and test central TDLib request coordination, flood wait handling, search deduplication, and shared stream, warmup, and download scheduling. Preserve cancellation, pagination, and playback priority.
- Finish Liquid AMOLED coverage across navigation, search, cards, dialogs, sheets, and TV; add translated strings and appearance tests.
- Audit app icon and other visible branding, all provider authorities, deep links, storage paths, and native dependencies per ABI.
- Run Android unit tests, desktop tests, TV focus and screenshot tests, and phone and TV playback tests. Confirm installation alongside TMPlayer on a device.
- Rebuild and recopy the APK after every code change. The current APK reflects only the changes listed above.
