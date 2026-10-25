# Nissan Teana port plan (2021 2.0T)

Goal: run this fork's CarPlay receiver on the 2021 Teana 2.0T stock head unit (超智联,
Android 9/10, 8-inch 1280x720, APK install over ADB port 5555 — see
[NISSAN_TEANA_HEADUNIT.md](NISSAN_TEANA_HEADUNIT.md)), and put a MikuCarLauncher-style
home under it. Facts about the head unit are owner-reported until the Phase 1 checklist
runs on the car.

## Architecture

```
┌─────────────────────────────────────────────┐
│ Teana head unit (Android 9/10, 1280x720)    │
│                                             │
│  Launcher app  ──widget/map card──►  DiPlay │
│  (home screen)                        CarPlay│
│                                     receiver │
└─────────────────────────────────────────────┘
        ▲ ADB install (port 5555)      ▲ Wi-Fi/USB
        │                             │ CarPlay session
   workstation                        iPhone
```

- **DiPlay (this repo)** stays the framework: connection, protocol, rendering, settings.
  Car-specific behaviour hangs off `NissanHeadUnit.detected` exactly as the Geely work
  hangs off `GeelyHeadUnit`; no other head unit changes.
- **Launcher** is a separate concern. DiPlay already exposes launcher integration
  ([LAUNCHER_INTEGRATION.md](LAUNCHER_INTEGRATION.md)): navigation widget, map card,
  embedded live map (SurfacePackage protocol), home-screen bubble. The Teana is Android
  9/10, so the **embedded map (needs Android 11+) is unavailable**; the widget and the
  map card are the integration paths that work.

## Phases

### Phase 0 — workstation (no car needed)
- [x] Fork cloned, `upstream` remote added, `develop`/`feat/*`/`hotfix` branches pushed
      ([BRANCHING.md](BRANCHING.md)).
- [x] `NissanHeadUnit` gate + test + research docs committed on
      `feat/nissan-teana-adaptation`.
- [ ] Install JDK 17+ and Android SDK (platform-tools, android-37, build-tools 36) so
      `gradlew :shared:testDebugUnitTest :common:testDebugUnitTest :mobile:assembleDebug`
      runs locally; until then CI is the only verifier.

### Phase 1 — on-car reconnaissance (parked, engine on)
- [ ] Enable ADB via the engineering menu; `adb connect <car-ip>:5555`.
- [ ] Run the getprop/wm-size checklist in NISSAN_TEANA_HEADUNIT.md; extend
      `NissanHeadUnit.KNOWN_UNIT_IDENTIFIERS` with the real values.
- [ ] Confirm Android version, panel size/density, USB data port, whether Android's
      Bluetooth stack sees the phone (upstream's maker-Bluetooth failure mode).
- [ ] Install the unmodified upstream APK; export a DiPlay diagnostic report.

### Phase 2 — stock DiPlay on the car
- [ ] Wired USB session first (sidesteps every Wi-Fi/Bluetooth unknown).
- [ ] Then existing-Wi-Fi (phone hotspot joined by the head unit), then Wi-Fi Direct
      (Android 9 legacy path) and the car hotspot.
- [ ] Pick the display preset for 1280x720; note stutter/fps findings in
      docs/VALIDATION-style notes.

### Phase 3 — Nissan-gated adaptations (one feat branch each)
- `feat/nissan-hotspot-guide`: Android 9/10 refuses app hotspot requests on non-BYD
  units; detect and show where the driver turns the car hotspot on.
- `feat/nissan-wheel-keys`: only if Phase 1 shows the wheel keys bypass standard
  `KEYCODE_MEDIA_*`; follow the Geely subscription pattern.
- `feat/nissan-autostart`: boot start if the firmware allows it.
- Bluetooth pairing reality check before any wireless work.

### Phase 4 — launcher (MikuCarLauncher concepts on the Teana)
The reference launchers (MikuCarLauncher 1.0–3.0) are Audi-A4L 32:9 Java launchers:
Live2D wallpaper, sidebar, weather, vehicle view, 高德 floating card via
`com.autonavi.plus.showmap` broadcasts. Reuse direction:
- [ ] Start with **map card + navigation widget** on a re-themed launcher — no protocol
      work needed, works on Android 9/10.
- [ ] Re-theme a launcher copy for 16:9 1280x720 (the A4L 32:9 art does not fit);
      keep the 高德 floating card (高德车机版 supports it on this unit per owner reports).
- [ ] Decide later whether the launcher lives as a new module in this fork
      (`:nissan-home`, mirroring `samples/home`) or its own repo.

### Phase 5 — hardening
- [ ] PR feat → develop runs CI (`android.yml` covers shared/common unit tests, lint,
      assembleDebug); merges to `main` tag `nissan-v0.x` releases.
- [ ] Hotfixes follow [BRANCHING.md](BRANCHING.md).

## Known risks (carried from upstream docs + research)

- The bundled experimental accessory identity is not Apple-certified; a future iOS may
  reject it (upstream README).
- Wireless CarPlay fails if the maker's own Bluetooth module owns calls/music; the
  超智联 pairs phones in its own UI, so this must be tested (USB always works around it).
- Android 9 Wi-Fi Direct legacy limits (docs/ANDROID9_WIFI_DIRECT.md).
- Charging-only USB ports cannot carry the wired session; the Teana port is unverified.
- 1280x720 is owner-reported, not measured; `wm size` in Phase 1 settles it.
