# 日产诊断功能使用指南（Nissan Diagnostics）

- 面向：独立验收方与 2021 天籁车主。
- 状态：功能已实现并通过测试（2026-10-26，见 `NISSAN_TEANA_ACCEPTANCE.md`），
  实车验证待做。

## 入口与可见性

设置 → 诊断 类别页，「保存诊断报告」卡片内。按钮**只在系统标识包含 nissan
的车机上显示**（识别逻辑见 `NissanHeadUnit`；实车采集完成前不匹配具体机型，
仅匹配明确的日产标识）。非日产设备不渲染该按钮——这是刻意的门控，不是缺陷
（Robolectric 用例 `nissanButtonHiddenWhenHeadUnitNotDetected` 与模拟器门控
基线截图双重钉住）。

## 点击后会发生什么

应用在后台线程依次执行四条**只读**命令（白名单，其余任何命令一律不执行）：

1. `getprop`（全量系统属性）
2. `wm size`（屏幕分辨率）
3. `wm density`（屏幕密度）
4. `dumpsys display`（显示服务摘要）

然后按结果展示三种对话框之一：

| 结果 | 含义 | 你该做什么 |
| --- | --- | --- |
| 诊断报告（15 行 key=value） | 采集成功 | 长按可复制；缺失字段显示 `unknown` 属正常（采集不到，绝不猜测默认值） |
| 重新采集诊断输入 | 部分命令输出包含失败信号（对话框会列出具体信号行） | 按提示用 ADB 在车机上重新采集（见下节） |
| 拒绝说明 | 系统标识不含 nissan（识别门控未通过） | 确认车机型号；如果是目标车，请采集 `getprop` 提供给开发者 |

## 在车机上用 ADB 采集（重采提示的正确做法）

命令清单以 `NISSAN_TEANA_HEADUNIT.md` §实车采集清单 为准。注意两点：

- `getprop` 一次输出全部属性（不支持一次传多个键），直接 `adb shell getprop`；
- `wm size` / `wm density` / `dumpsys display` 需要 shell 权限——**app 进程内
  执行时这些命令在部分车机上会被拒绝**（模拟器实测：wm 报 Can't find service、
  dumpsys 报 Permission Denial），这正是「重新采集」提示出现的原因；经 ADB
  shell 执行则有权限。这是预期行为，不是故障。

## 隐私与安全

- 白名单只有上述四条只读命令；注入任何其他命令（包括拼接变体）不会执行。
- 每条命令 10 秒超时，超时销毁进程。
- 报告只含系统属性与显示摘要；不含协议数据、凭据或个人信息（与既有诊断
  报告同一隐私约定）。

## 已知限制

- 真实命令执行器已通过纯 JVM 桩测试与 Android 15 模拟器端到端验证；实车
  行为取决于车机对本应用的权限配置。
- 实车采集完成后，确认过的 `ro.product.*` 值才会加入具体机型识别列表
  （`KNOWN_UNIT_IDENTIFIERS`，当前为空）。

## 桌面图标不可见时

原厂桌面读取硬编码的应用白名单（`config_with_dvr.json` / `white.txt`，PRD R7），
侧载安装的 DiPlay 可能不出现在桌面上。这是白名单机制，不是安装失败。两个进入
预案（仅为文档说明，应用本身不做任何运行时改动）：

1. 用 ADB 直接启动：
   `adb shell am start -n com.shihab.diplay.hudtest/com.shilapi.xcertplay.DiPlayActivity`
2. 安装第三方启动器：社区实测有车主用 DUDU Launcher Pro 绕过白名单；仅作
   进入手段记录，不构成推荐。

## 实车勘测增补清单

在 `NISSAN_TEANA_HEADUNIT.md` §实车采集清单 之外，实车勘测补充以下检查项：

- `ip addr show` 摘要：Android 9 世代 kernel 4.9 可能剥离 IPv6（`ip -6 route`
  为空，PRD R6）——若确认剥离，无线路径须强制 IPv4 网关，并与上游 #1071
  （StartSession 只带单地址）叠加评估风险。
- 桌面可见性首查项：侧载后先确认 DiPlay 图标是否可见（见上节），再继续其余
  检查。
- 阶段 2 验收阈值候选基准（PRD R11，车主实测）：无线 CarPlay（2.4GHz 热点）
  触摸延迟 40–60ms、HEVC 降码率约 40%；有线 USB <5ms 零丢包——仅作候选基准，
  最终阈值以实车实测为准。
