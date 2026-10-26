# 日产诊断链路独立验收材料（NissanDiagnostics）

- 日期：2026-10-26
- 分支：`feat/nissan-teana-adaptation`
- 性质：本文件是给独立验收方的材料清单，不是变更的一部分。所有代码均未跟踪、
  未 `git add`、未 commit、未 push，验收方可在工作区直接核对。

## 一、验收范围

本次交付（2026-10-26 日产诊断 loop，轮次 1–13）包含三层：

1. **纯 JVM 解析与编排链路**（`shared` 模块，`com.shilapi.xcertplay.network` 包）：
   文本解析 → 转录组装 → 采集编排 → 报告渲染，以及命令执行抽象与真实执行器。
2. **UI 集成**（`common` 模块）：Diagnostics 卡片内 Nissan 门控按钮 + 三分支
   对话框（重采提示 / 拒绝说明 / 报告渲染）。
3. **文档**：SDD / BDD / 调研纪要 / 交接文档 / 工作日志（见第五节索引）。

## 二、交付物清单（文件 → 用例）

### shared 模块（7 个实现 + 6 个测试文件，全部未跟踪）

| 文件 | 职责 | 用例数 |
| --- | --- | --- |
| `NissanDiagnosticsTextParser.kt` | 纯文本 → `NissanDiagnosticsInput`（7 属性 + wm 两分区 + dumpsys 采集区） | 24 |
| `NissanDiagnosticsTranscript.kt` | 调用方侧转录组装（getprop 段 + 三分区标签）+ 六前缀失败信号检测 | 5 |
| `NissanDiagnosticsCapture.kt` | 采集编排：信号检测→组装→解析→报告，sealed `Outcome` 三分流 | 4 |
| `NissanDiagnosticsCommandCapture.kt` | 命令执行抽象（可注入 fun interface）+ 四条命令常量 + 采集运行器 | 4 |
| `NissanDiagnosticsShellCommandExecutor.kt` | 真实执行器：白名单 + 10s 超时销毁 + 并发读流合并 | 4 |
| `NissanDiagnosticsReport.kt` | 报告层（15 行 key=value，缺失渲染 unknown）——此前会话产出 | 6 |
| `NissanHeadUnit.kt` | 识别门控——**他人未提交修改，非本次交付**，验收时按既有约束核对（`KNOWN_UNIT_IDENTIFIERS` 为空） | 既有 |

诊断链路合计 47 个纯 JVM 用例；`shared` 模块整体全量回归数字见第四节。

### common 模块（修改 2 处 + 新增 1 测试）

| 变更 | 内容 |
| --- | --- |
| `DiPlayActivity.kt`（+64 行） | 5 个 network import；DIAGNOSTICS 卡片内 `NissanHeadUnit.detected` 门控按钮；`nissanDiagnosticsRunner` 可注入字段（默认真实链路，测试注入桩）；后台线程执行 + `runOnUiThread` 三分支对话框 + 等宽报告对话框 |
| `values*/strings.xml`（9 目录） | 5 个新字符串 × 9 locale（`settings_nissan_diagnostics`、`_recapture_title`、`_recapture_message(%1$s)`、`_refusal_message`、`_report_title`），已核查 9 目录全部存在 |
| `NissanDiagnosticsUiTest.kt`（新增） | 4 个 Robolectric 用例（重采提示含信号 / 拒绝说明无重采 / 15 行报告渲染 / 门控不显示——轮次 14 钉子） |

### AnwPhoneLink 蓝牙桥接（2026-10-26 轮次 23–27b，新包 `shared/.../bluetooth/`）

| 文件 | 职责 | 用例 |
| --- | --- | --- |
| `CarPlayBluetooth.kt` | 最小蓝牙后端契约（无 Android 类型，纯 JVM）+ 设备数据类 | — |
| `AnwVendorChannel.kt` | 厂商通道抽象（隔离 AIDL binder）+ iAP2 UUID→GUID 字节序 | 见 Wiring |
| `HsaeRfcommStream.kt` | 上游 `BlockingDuplexByteStream` 适配（FIFO 保序/故障传播/清理回落） | Bridge 6 |
| `AnwPhoneLinkBluetoothAdapter.kt` | `CarPlayBluetooth` 替身 + `CarPlayBluetoothManager` 混合门（原生优先）+ `selectVendorWirelessTarget` | Bridge 6 |
| `AnwPhoneLinkWiring.kt` | 装配层（`AnwServiceProbe` 检测门组装） | Wiring 5 |
| `AndroidAnwServiceProbe.kt` | 生产探针薄层（`queryIntentServices` 双匹配） | — |
| `WirelessBluetoothPlanner.kt` | 混合门纯函数规划（Native/Vendor/None） | Planner 5 |
| `com/anwsdk/service/` 三 AIDL 文件 | 厂商接口定义（来源 T32 fork，事务号与 #880 实机互证） | — |
| `CarPlayController.kt`（三处窄改） | 预检放行 / 决策两型 / 流建立两型——**原生分支零变化** | 全量回归守卫 |

测试：`AnwBluetoothBridgeTest`（6）+ `AnwPhoneLinkWiringTest`（5）+
`WirelessBluetoothPlannerTest`（5）= 16 用例全绿。

## 三、复验命令（Windows，pwsh 7）

环境前置（项目路径含中文，必须走 `X:` 映射；`X:` 已由 `subst` 指向仓库根）：

```powershell
$env:JAVA_HOME='D:\dev-env\jdk-21.0.12.1+1'
$env:ANDROID_HOME='D:\dev-env\android-sdk'
Set-Location X:\
& 'D:\dev-env\gradle-9.5.0\bin\gradle.bat' '-Dorg.gradle.java.installations.paths=D:\dev-env\jdk-21.0.12.1+1,D:\dev-env\jdk-25.0.4.1+1' <任务>
```

任务一览（`--tests` 参数不加引号）：

| 验什么 | 任务 |
| --- | --- |
| 诊断链路全部用例 | `:shared:testDebugUnitTest --tests com.shilapi.xcertplay.network.NissanDiagnostics*`（47 用例） |
| Anw 蓝牙桥接全部用例 | `:shared:testDebugUnitTest --tests com.shilapi.xcertplay.bluetooth.*`（16 用例） |
| 真机拆解样本测试 | `:shared:testDebugUnitTest --tests com.shilapi.xcertplay.network.NissanTeanaRealWorldSampleTest`（3 用例） |
| 诊断生命周期守卫 | `:common:testDebugUnitTest --tests com.shilapi.xcertplay.NissanDiagnosticsLifecycleTest`（2 用例：防重入/销毁不弹窗） |
| 粘贴转录入口（轮次 34） | `:common:testDebugUnitTest --tests com.shilapi.xcertplay.NissanDiagnosticsPasteTest`（12 执行：完整转录/裸 dumpsys/空白/拒绝/门控/输入框，×SDK 28/34） |
| UI 三分支 | `:common:testDebugUnitTest --tests com.shilapi.xcertplay.NissanDiagnosticsUiTest`（3 用例） |
| 字符串与布局守卫 | `:common:testDebugUnitTest --tests *SettingsTranslationsTest --tests *SettingsLayoutPolicyTest` |
| shared 全量回归 | `:shared:testDebugUnitTest` |
| common 全量回归 | `:common:testDebugUnitTest`（见第四节已知失败） |

### 预期结果

- shared 全量：全绿（数字见第四节）。
- common 全量：除「本机环境失败」小节列出的用例外全绿。
- Robolectric 首跑需编译，耗时数分钟属正常。

## 四、验证证据（2026-10-26 实测）

- shared 全量回归：**265 个测试类 / 1766 用例 / 0 失败**（2026-10-26 轮次 13 实跑复验，
  JUnit XML 汇总）。
- common 全量回归：**208 个测试类 / 1675 用例 / 5 失败**——失败恰好全部为下方
  「本机环境失败」列出的 5 个已知用例（4×`UsbPermissionSetupTest`、
  1×`CarPlayHostClusterStartupTest`），除已知项外全绿，无新增回归。
- 定向守卫（轮次 12 实测）：`SettingsTranslationsTest` / `JapaneseTranslationsTest` /
  `TraditionalChineseTranslationsTest` / `SettingsLayoutPolicyTest` 全过。
- **AGENTS.md 完整 CI 命令（轮次 15 实测）**：`:mobile|home|maphost:lintDebug` +
  `assembleDebug` + `:home:testDebugUnitTest` **BUILD SUCCESSFUL**（含轮次 12 的
  Activity 改动与轮次 15 的 executor 修复）。
- **`:shared:lintDebug`（轮次 15 新增检查）**：首跑揪出日产执行器 3 个 NewApi
  缺陷（API 26+ 调用、minSdk=24）→ 已修复（`exitValue()` 轮询 + `SDK_INT` 守卫）
  → 复验**日产文件 lint 零命中**，模块剩余 33 errors 为既有代码存量债务
  （CI 命令不含 shared lint 的原因），与本交付无关。
- TDD Red 证据（编译失败日志，`%TEMP%` 下）：`nissan_parser_red.log`、
  `nissan_opt_red.log`、`nissan_transcript_red.log`、`nissan_capture_red.log`、
  `nissan_cmdcap_red.log`、`nissan_exec_red.log`、`nissan_ui_red.log`。
- 每轮 Red→Green 过程与数字见 `NISSAN_TEANA_PARSER_WORKLOG.md` 轮次 1–13。

### 本机环境失败（与日产组件无关，对照实验已证明）

- `common` `UsbPermissionSetupTest` 4 用例：依赖 POSIX `sh`，Windows 缺失
  （CreateProcess error=2）。
- `common` `CarPlayHostClusterStartupTest` 1 用例：Mockito 偶发（flaky），基线重跑
  通过。根因调查（2026-10-26 轮次 17，只读）：`times(2)` 的 setSurface 偶发只到
  1 次，嫌疑为 PAUSED looper 的 `idle()` 只排当前队列 +
  `clusterOutputReady()` 条件分支时序（详见 worklog 轮次 17）；属既有上游测试
  时序敏感，与本交付无关。

## 五、文档索引

| 文档 | 内容 |
| --- | --- |
| `NISSAN_TEANA_SDD.md` | 设计：解析器契约、调用方接线规则、转录组装、采集编排、命令执行、真实执行器、Outcome UI 分支 |
| `NISSAN_TEANA_BDD.md` | 19 个已验证场景 + 3 个原规划场景（已由轮次 12 兑现）+ 场景↔测试映射全表 |
| `NISSAN_TEANA_RESEARCH_20261026.md` | AOSP 8.1→14 源码验证 + 日产社区调研（附来源 URL）、调用方接线规则 |
| `NISSAN_TEANA_PARSER_HANDOFF.md` | 交接文档（输入契约、构建环境、后续清单） |
| `NISSAN_TEANA_PARSER_WORKLOG.md` | 轮次 1–16 日志 + 操作计数表 |
| `NISSAN_DIAGNOSTICS_USER_GUIDE.md` | 日产诊断使用指南（验收方/车主视角） |

## 六、红线核查表（验收方逐条核对）

| # | 约束 | 状态 | 证据 |
| --- | --- | --- | --- |
| 1 | 不 `git add` / commit / push | 满足 | `git status` 全部为未跟踪/工作区修改 |
| 2 | 不动他人未提交的六个文件（README.zh-CN.md、BRANCHING.md、NISSAN_PORT_PLAN.md、NISSAN_TEANA_HEADUNIT.md、NissanHeadUnit.kt 及其测试） | 满足 | 修改时间早于本次 loop；本次会话零触碰 |
| 3 | 传闻值（1280x720、NTDA3、P1xx）不进运行时代码 | 满足 | `findstr /s /i "NTDA3 1280x720 P1" shared\src\main common\src\main` 仅文档命中 |
| 4 | AGENTS.md：Nissan 设置属 Diagnostics 类别 | 满足 | 按钮在 DIAGNOSTICS 卡片内，未新增 SettingsSection |
| 5 | AGENTS.md：新字符串进 values + 全部 values-xx | 满足 | 5 键 × 9 目录核查通过；`SettingsTranslationsTest` 过 |
| 6 | AGENTS.md：布局截图（紧凑/完整 + 大字号，RTL 酌情） | 满足 | `docs/screenshots/`：`nissan_full_zh_font150.png` / `nissan_compact_zh_font150.png`（简中，540x1140 窄窗）/ `nissan_rtl_ar_font150.png`（阿语 RTL），另附门控基线 `baseline_*`×2；几何验证见第七节 |
| 7 | 按钮不触发重连类行为，无 `markReconnectNeeded` 义务 | 满足 | 诊断按钮只读采集，不改变连接设置 |
| 8 | `KNOWN_UNIT_IDENTIFIERS` 保持空（实车采集前不识别具体机型） | 满足 | `NissanHeadUnit.kt:20`（他人文件，未触碰） |

## 七、已知限制与待办

1. 真实执行器（`NissanDiagnosticsShellCommandExecutor`）：白名单/超时/读流逻辑
   有桩进程测试；Android 15 模拟器已实证真实 `sh` 链路（四命令执行、getprop
   全量采集、wm 静默丢弃、dumpsys 信号保留、CaptureFailed 对话框——截图
   `nissan_e2e_capture_failed_real.png`，轮次 14）。**注意 app uid 下
   wm/dumpsys 受限**（"对 shell 可读"仅 shell uid 成立），实车行为取决于车机
   权限配置——实车验证仍待做，预期路径已被模拟器走通。
2. 上游 hotfix 交叉验证（#1084/#1085，轮次 14）：本地 main 已含修复；
   LauncherIntentTest 14 用例 Green；hotfix 基线 common 1672 用例仅 4 个已知
   环境失败；模拟器 boot 广播正路径实证。worktree
   `%TEMP%\DiPlay-hotfix1084`（分支 `hotfix-1084-boot-return-carplay`）保留。
2. 布局截图：**已完成（2026-10-26 轮次 13）**。方法：sdkmanager 安装 emulator +
   `system-images;android-35;google_apis;x86_64`（用户授权自行安装），AVD
   `diplay_nissan_gapi` 经 `adb root → disable-verity → remount` 后把
   `/product/etc/build.prop` 与 `/system/build.prop` 的 manufacturer 行改为
   `NISSAN`，使 `NissanHeadUnit.detected=true`（该改动仅在 AVD 磁盘 overlay 内，
   不影响仓库任何代码；诊断值不回填运行时的红线不受影响——模拟器属性注入是
   环境配置，不是代码默认值）。截图统一 `font_scale 1.5`：
   - `nissan_full_zh_font150.png`（简中，1080x2340）：日产诊断按钮
     bounds=[99,1420][981,1585]，高 165px 与卡内「保存诊断报告」
     [99,826][981,991] 等全部按钮一致，x 对齐 [99,981]、行间距 33px 一致。
   - `nissan_compact_zh_font150.png`（简中窄窗 540x1140）：按钮
     [99,685][441,850] 完整可见，高 165px 与相邻按钮一致，无裁剪。
   - `nissan_rtl_ar_font150.png`（阿语 RTL）：按钮 bounds=[99,527][981,692]
     高 165px 一致；类别图标与文本右对齐镜像正常。
   - 门控基线 `baseline_full_font150.png` / `baseline_compact_font150.png`
     （manufacturer=unknown 的 default 镜像）：诊断卡**无**日产按钮——门控
     拒绝路径的实证。
3. `NISSAN_TEANA_HEADUNIT.md` §实车采集清单 的 getprop 多参数命令待文档所有者修正。
4. （可选）JMH 手动基准；现有 1.4MB 大输入结构化冒烟已覆盖数量级。
5. 实车采集完成后扩充 `KNOWN_UNIT_IDENTIFIERS`（handoff 第七节第 6 项）。
7. 低负载守卫（轮次 29）：诊断防重入、销毁不弹窗、桥接监听器释放已落地并钉住；资源预算矩阵见 SDD「低负载与资源约束设计」（FIFO 背压待实车厂商流证据）。真机规格模拟（轮次 30）：1280x720+d160 布局验证通过，样本值仅测试零回填。
6. AnwPhoneLink 蓝牙桥接的真机路径（服务绑定→`ANWBT_SocketConnect`→iAP2 交换）
   待实车验证——纯 JVM 已覆盖决策门与字节流语义，控制器原生分支由全量回归
   守卫（shared 268 类/1782 用例/0 失败）。上游检查点（2026-10-26 轮次 28）：
   #1071 仍 OPEN、0.2.22 未发布、上游 main 仍 9d896330——无同步动作。
