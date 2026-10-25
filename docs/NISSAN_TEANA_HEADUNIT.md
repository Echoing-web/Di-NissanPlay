# Nissan Teana (2021 2.0T) head unit — research notes

Target car: China-market 2021 Nissan Teana 2.0T (seventh-generation Altima, VC-Turbo).
Everything below comes from public owner reports and tutorials
(汽车之家, Bilibili, 太平洋汽车, tool-vendor guides), cross-checked where possible.
**Nothing has been measured on this project's own car yet**; items marked
`TO CONFIRM` need an on-car check before code may depend on them.

## System

- The stock head unit runs 超智联 (Nissan Connect, "智联导航" firmware family), a closed
  Android build. 2019–2021 units are commonly reported as Android 9-era; 2023 units are
  reported as Android 10. `TO CONFIRM`: the exact Android and firmware version of the
  2021 Teana 2.0T unit (设置 → 版本信息, and `adb shell getprop ro.build.version.*`).
- Screen: 8-inch centre display, reported 1280x720. `TO CONFIRM` via
  `adb shell wm size`.
- No BYD DiLink services exist; every DiLink-gated route in DiPlay stays off. The
  Geely-style lesson applies: vendor integrations hang off a head-unit check
  (`NissanHeadUnit`) and nothing changes on other units.

## Installing third-party APKs (owner-reported methods)

1. **Engineering mode + ADB (recommended for this port).** In 系统设置 → 版本信息,
   long-press the three lines bottom-to-top (2 s each) to open the engineering/diagnostic
   menu (the exact gesture differs slightly by model year), enable ADB. Wireless ADB on
   port 5555 is reported to work on 2019–2023 units; `adb connect <car-ip>:5555` then
   `adb install` is the standard route the community uses.
2. **U-disk installer.** FAT32 U-disk with a market APK (当贝市场极速版 / 沙发管家车载版),
   opened from the stock file manager after allowing unknown sources.
3. **Hotspot push.** Turn on the head unit's own hotspot, join it from the phone, push the
   APK with 悟空遥控器.

Root: public videos describe root for the 2021 units (轩逸/天籁/逍客/奇骏). This port does
not require root; ADB install is enough. Root may affect the warranty and is out of scope.

## What this means for DiPlay

- DiPlay installs as a normal APK (Android 7.0+ / API 24+); the Android 9 Wi-Fi Direct
  legacy path and the "driver turns the car hotspot on in car settings" behaviour apply.
- Connection transport options on this unit, in the order worth trying:
  1. USB (data port required) — `TO CONFIRM` the Teana's USB port does data, not only charging.
  2. Existing Wi-Fi / same LAN (docs/EXISTING_WIFI.md) — phone and head unit join the same
     external router (e.g. phone hotspot joined by the head unit).
  3. Wi-Fi Direct — subject to the Android 9 limits in docs/ANDROID9_WIFI_DIRECT.md.
  4. Car hotspot as the CarPlay network — the head unit's hotspot is 2.4 GHz on most of
     these units; usable but more likely to stutter.
- Bluetooth caveat from upstream docs: wireless CarPlay cannot start on a unit whose maker
  keeps calls/music on its own Bluetooth module instead of Android's. The 超智联 units pair
  phones in their own Bluetooth UI, so this risk is real; `TO CONFIRM` whether Android's
  Bluetooth stack sees the pairing. USB sidesteps it.

## On-car checklist (fills the TO CONFIRM items)

Run over `adb connect <car-ip>:5555` and record in the diagnostic report:

```
adb shell getprop ro.product.device ro.product.model ro.product.manufacturer ro.product.brand
adb shell getprop ro.build.version.release ro.build.version.sdk
adb shell wm size && adb shell wm density
adb shell dumpsys display | findstr -i displayId
adb shell getprop | findstr -i "nissan model hardware"
```

The `ro.product.*` values extend `NissanHeadUnit.KNOWN_UNIT_IDENTIFIERS`; the screen size
picks the resolution preset in Settings → Display.

## Sources

Owner-reported, not measured by this project:

- 日产智联屏(19-21款)安装APP方法 — club.autohome.com.cn/bbs/thread-c-656-94285692-1.html
- 21款日产智联导航获取Root权限 — bilibili.com/video/BV1nn4y1f7G6
- 日产无线ADB工具（19-23款，安卓9-10，5555端口） — bilibili.com/video/BV1Q5EFzmEoZ
- 23款天籁、轩逸安卓10系统升级安装三方软件 — bilibili.com/video/BV1wr421c7ox
- U盘安装三方软件 — bilibili.com/video/BV1hs421N7rg
- 悟空遥控器热点推送 — club.m.autohome.com.cn/bbs/thread/46db19c85f6bb05b/99322437-1.html
- 第三方应用安装说明 — pcauto.com.cn/cxxj/3236/32365576.html
