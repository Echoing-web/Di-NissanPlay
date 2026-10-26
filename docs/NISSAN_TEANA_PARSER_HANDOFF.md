# 日产诊断文本解析器交接文档（NissanDiagnosticsTextParser）

- 日期：2026-10-26
- 分支：`feat/nissan-teana-adaptation`
- 涉及文件（均未提交、未推送）：
  - `shared/src/main/java/com/shilapi/xcertplay/network/NissanDiagnosticsTextParser.kt`（实现）
  - `shared/src/test/java/com/shilapi/xcertplay/network/NissanDiagnosticsTextParserTest.kt`（24 个纯 JVM 用例）
  - `shared/src/main/java/com/shilapi/xcertplay/network/NissanDiagnosticsTranscript.kt`
    （调用方侧转录组装 + 失败信号检测，纯文本组件）
  - `shared/src/test/java/com/shilapi/xcertplay/network/NissanDiagnosticsTranscriptTest.kt`
    （5 个纯 JVM 用例，含组装→解析往返契约）
  - `shared/src/main/java/com/shilapi/xcertplay/network/NissanDiagnosticsCapture.kt`
    （采集编排：信号检测→组装→解析→报告，sealed Outcome 三分支）
  - `shared/src/test/java/com/shilapi/xcertplay/network/NissanDiagnosticsCaptureTest.kt`
    （4 个纯 JVM 用例，含分流优先级钉子）
  - `shared/src/main/java/com/shilapi/xcertplay/network/NissanDiagnosticsCommandCapture.kt`
    （命令执行抽象层：可注入执行器接口 + 四条调研验证的命令常量 + 采集运行器）
  - `shared/src/test/java/com/shilapi/xcertplay/network/NissanDiagnosticsCommandCaptureTest.kt`
    （4 个纯 JVM 桩执行器用例，含执行器→编排全链路）
  - `shared/src/main/java/com/shilapi/xcertplay/network/NissanDiagnosticsShellCommandExecutor.kt`
    （真实执行器：白名单+超时销毁+并发读流合并，可注入进程抽象）
  - `shared/src/test/java/com/shilapi/xcertplay/network/NissanDiagnosticsShellCommandExecutorTest.kt`
    （4 个纯 JVM 桩进程用例；真实 `sh` 行为待实车验证）
- 状态：实现完成、测试全绿、**尚无任何调用方**（刻意不接线，等待独立验收后再接入）。

## 一、它是什么、不是什么

`NissanDiagnosticsTextParser.parse(text, source, detected)` 把一段纯文本诊断记录
变换为 `NissanDiagnosticsInput`，供现有 `NissanDiagnosticsReport.from` 渲染。

它是、且只是**纯文本变换**：

- 不执行 shell、ADB 或任何命令；不做任何 I/O；不依赖 Android framework（纯 JVM 可测）。
- 不做识别：`detected` 与 `source` 由调用方提供并**原样透传**，解析器绝不推断、
  绝不改写；`platformHint` 恒为 null（解析器不产生平台标注）。
- 不回填任何默认值：缺失或空白字段保持 null，由报告层统一输出 `unknown`。
  运行时代码中不出现 `NTDA3`、`P1xx`、`1280x720` 等公开资料字符串常量。
- 与 `NissanHeadUnit` / `KNOWN_UNIT_IDENTIFIERS` 零依赖；后者在实车采集前保持空列表
  （见 `NISSAN_TEANA_SDD.md`、`NISSAN_TEANA_BDD.md` 的既有约束，本组件不触碰）。

## 二、数据流与 API

```
纯文本记录（实车采集的粘贴/转录文本）
    │ NissanDiagnosticsTextParser.parse(text, source, detected)
    ▼
NissanDiagnosticsInput（缺省字段为 null）
    │ NissanDiagnosticsReport.from(input)   // detected=false 时返回 null，拒绝生成
    ▼
固定 15 行 key=value 报告（缺失字段渲染 unknown）
```

## 三、输入文本契约（以下行为全部有测试钉住）

1. **行分隔**：兼容 `\n`、`\r\n`、`\r`。
2. **属性行**：以下 7 键支持 getprop 的 `[key]: [value]` 与裸 `key=value` 两种写法：
   `ro.product.product`、`ro.product.device`、`ro.product.model`、
   `ro.product.manufacturer`、`ro.product.brand`、`ro.build.version.release`、
   `ro.build.version.sdk`。
   - 任意位置（包括任何分区内与首个标记之前）的属性行都会被解析；重复键按后出现者
     覆盖。
   - 值去除首尾空白后保留（内部可含空格、`=`、`]`、引号、命令文本——注入内容只是
     数据）；空白值视为缺失（不记录）；键前缀精确匹配（`ro.product.modelX=1` 不会
     命中 model）。
3. **分区标记**：行首（忽略首尾空白、区分大小写）的整段
   `wm size:`、`wm density:`、`dumpsys display:`；标记之后的**同行剩余内容**（去首尾
   空白）视为该分区首行（如 `wm size: Physical size: 800x1280`）。`WM SIZE:`、嵌在
   句中的子串都不算标记。
4. **wm size 分区**：只保留 `Physical size:` / `Override size:` 行；**wm density 分区**：
   只保留 `Physical density:` / `Override density:` 行（保留行同样去首尾空白）。
   均维持原始顺序；跨分区的行（如 density 行出现在 size 分区）与前缀撞车行
   （如 `Physicality:`）不采集；无标记时出现的 Physical 行不采集。
5. **dumpsys display**：只在显式标记之后开始采集；到下一个 wm 标记或输入结束为止；
   没有显式标记时不得采集（即使文本酷似 dumpsys 输出）。采集文本保留内部行与缩进，
   仅去除首尾空白。
6. **同一分区多次出现**：跨块按原始顺序累计（不覆盖）。
7. **空白输入**：全部字段 null，`source`/`detected` 仍原样透传。

### 输入示例

```
[ro.product.manufacturer]: [NISSAN MOTOR CO., LTD.]
[ro.build.version.release]: [10]
[ro.build.version.sdk]: [29]
wm size:
Physical size: 800x1280
Override size: 720x1520
wm density:
Physical density: 320
dumpsys display:
DISPLAY MANAGER (dumpsys display)
  mGlobalDisplayState=ON
```

## 四、与项目计划的对应

- 对应 `NISSAN_PORT_PLAN.md` **阶段 1（实车勘测）**的诊断采集环节；实车采集命令以
  `NISSAN_TEANA_HEADUNIT.md` §实车采集清单 为准（`getprop ro.product.*`、
  `wm size && wm density`、`dumpsys display` 摘要等），解析器消费其粘贴文本。
- `NISSAN_TEANA_BDD.md` / `NISSAN_TEANA_SDD.md` 写在解析器存在之前，只覆盖了
  `NissanDiagnosticsInput → NissanDiagnosticsReport` 一段。**接线前缺口**：按
  `BRANCHING.md` 的 SDD→BDD→TDD 循环，需为"纯文本 → NissanDiagnosticsInput"这一步
  补 SDD 目标/输入/输出/非目标/验收证据与 BDD 场景。
- 术语对齐：来源只用 `on-car` / `owner-reported`；平台代号只通过
  `NissanPlatformHint` 由调用方另行标注，解析器不消费、不识别平台串。

## 五、测试与验证证据

- 定向测试：`NissanDiagnosticsTextParserTest`，24 个用例（JUnit4，无 Robolectric），
  覆盖第三节全部行为 + 命令注入文本仅作数据 + detected 透传（detected=false 时
  `NissanDiagnosticsReport.from` 拒绝生成）+ 重复解析结果一致（纯函数）+
  调研验证的不变量（标记文本嵌在行中不构成标记；属性值含 `]: [` 不歧义、括号未闭合
  的畸形行不误解析；值去首尾空白）+ 约 1.4MB 大输入结构化冒烟（性能调研结论：
  保留 `lineSequence()` 与 `startsWith` 前缀链，不引入 Regex 与 Parameterized）+
  调用方接线规则（全量 getprop 只提取七属性且无关值不流入报告；wm 失败信号行丢弃、
  dumpsys 失败信号行保留为证据）。
- 回归：`:shared:testDebugUnitTest` 全量 261 个测试类、1749 个用例、0 失败
  （2026-10-26，本机）。
- 调研交叉验证：`NISSAN_TEANA_RESEARCH_20261026.md`——解析器的三条核心假设
  （getprop 格式、wm 只出 Physical/Override 行、`dumpsys display:` 行首标记无碰撞）
  已经 AOSP 8.1→14 源码逐版本验证成立；并发现 HEADUNIT.md §实车采集清单 的
  getprop 多参数命令是坏的（getprop 最多 2 个参数），调用方接线规则见该文档第二节。
- TDD 纪律：
  - 初版：先写测试（Red：13 处 `Unresolved reference`，`BUILD FAILED`）→ 最小实现
    → Green（10/10）。
  - 优化轮：先加 8 个测试（Red：恰 3 个新行为用例失败）→ 实现 → Green（18/18）。
- AGENTS.md 的 CI 全量命令已运行：三个模块的 `lintDebug`、`assembleDebug` 与
  `:home:testDebugUnitTest` 通过。`:common:testDebugUnitTest` 有 5 个与本组件无关的
  本机环境失败（见第六节）。

## 六、本机构建环境（Windows，交接必读）

- 仓库路径 `D:\车机改造DIY\DiPlay`；`D:\dev-env\build-diplay.bat` 是既定构建入口
  （JDK21 启动 + JDK25 工具链 + `X:` ASCII 盘符映射）。注意：该 .bat 内中文路径在
  部分代码页下报"命令语法不正确"，可直接展开为等价内联命令：

  ```
  set JAVA_HOME=D:\dev-env\jdk-21.0.12.1+1
  set ANDROID_HOME=D:\dev-env\android-sdk
  cd /d X:\
  D:\dev-env\gradle-9.5.0\bin\gradle.bat "-Dorg.gradle.java.installations.paths=D:\dev-env\jdk-21.0.12.1+1,D:\dev-env\jdk-25.0.4.1+1" <任务>
  ```

- 定向测试命令：
  `:shared:testDebugUnitTest --tests com.shilapi.xcertplay.network.NissanDiagnosticsTextParserTest`
- 本机已知失败（与日产组件无关，已做对照实验证明）：`common` 模块
  `UsbPermissionSetupTest` 4 个用例依赖 POSIX `sh`（Windows 缺失，CreateProcess
  error=2）；`CarPlayHostClusterStartupTest` 1 个 Mockito 用例偶发失败。
- Shell 是 cmd；PowerShell 命令用 `powershell -NoProfile -Command` 调用。
- 模拟器环境（2026-10-26 轮次 13 自装）：emulator + API35 两镜像（default /
  google_apis x86_64）已在 `D:\dev-env\android-sdk`；AVD `diplay_nissan_gapi`
  已注入 `ro.product.manufacturer=NISSAN`（经 root→disable-verity→remount 改
  product/system build.prop，仅 AVD overlay 内）。模拟器截图要点：MCP android
  插件进程缺 `ANDROID_HOME` 不可用，用 raw adb（`D:\dev-env\android-sdk\
  platform-tools\adb.exe`）；`-prop` 只接受 `qemu.*`；`/data/local.prop` 在
  Android 15 失效；app 语言切换走 Settings→Language（RTL 下 APPLY 按钮镜像到
  左侧）；mobile debug 包名 `com.shihab.diplay.hudtest`，Settings 页用
  `am start -n com.shilab.diplay.hudtest/com.shilapi.xcertplay.DiPlayActivity
  --es page settings`；首次启动有 ImmersiveModeConfirmation 对话框需点 Got it。

## 七、后续工作清单（按序）

1. **（轮次 26 已完成，2026-10-26）Anw 装配层 + AIDL 移植**：`AnwPhoneLinkWiring`（探针检测门组装，`AnwPhoneLinkWiringTest` 5/5）+ `com.anwsdk.service` 三 AIDL 文件（事务号与 #880 互证，.class 产物核对）；全量 shared 267 类/1777 用例/0 失败，lint 零命中。**已完成端到端接线（轮次 27b，2026-10-26）**：`WirelessBluetoothPlanner`（5/5 Green）+ `AndroidAnwServiceProbe` 生产探针 + CarPlayController 三处窄改（预检放行/决策两型/流建立两型，原生分支零变化，全量 shared 268 类/1782 用例/0 失败守卫）。**剩余**：实车验证（服务绑定→SocketConnect→iAP2 交换真机路径）。此前轮次 25 已交付：新包
   `shared/.../bluetooth/` 四文件（CarPlayBluetooth 契约 / AnwVendorChannel
   厂商通道抽象 + UUID→GUID 字节序 / HsaeRfcommStream 字节流适配 /
   AnwPhoneLinkBluetoothAdapter + CarPlayBluetoothManager 混合门 +
   selectVendorWirelessTarget），`AnwBluetoothBridgeTest` 6/6 Green（Red→Green
   留痕），全量 shared 266 类/1772 用例/0 失败，lint 新包零命中。**剩余**：
   轮次 26 Android 装配层（AIDL 定义 + 生产 resolver/channelFactory +
   CarPlayController 接入）→ 实车验证。
2. 独立验收当前代码与文档（原任务约定：验收前不改其他文件、不接线）。
2. ~~为解析器补 SDD/BDD 条目~~ **已完成**（2026-10-26：`NISSAN_TEANA_SDD.md` 新增
   文本解析设计/非目标/证据，`NISSAN_TEANA_BDD.md` 新增 4 场景 + 场景↔测试映射）。
3. HEADUNIT.md 所有者修正 §实车采集清单 的 getprop 多参数命令
   （见 `NISSAN_TEANA_RESEARCH_20261026.md` 第二节）。
4. ~~Kotlin 解析性能实践调研 + 性能冒烟测试切片~~ **已完成（2026-10-26 轮次 5；
   轮次 14 补手动基准数字）**：调研结论全部采纳（保留 `lineSequence()` 与
   `startsWith` 前缀链，不引入 Regex/Parameterized），`大输入结构化冒烟测试`
   （约 1.4MB 合成转录 + 10 秒数量级上限）已入 `NissanDiagnosticsTextParserTest`；
   轮次 14 JUnit 计时实测该用例 **0.041s**（≈34MB/s 量级），维持不引入 JMH 的决策。
5. ~~实现 Outcome UI 分支~~ **已完成（2026-10-26 轮次 12）**：Diagnostics 卡片内
   Nissan 门控按钮（`NissanHeadUnit.detected`，不动 Settings IA）→ 后台线程执行
   `NissanDiagnosticsCommandCapture(NissanDiagnosticsShellCommandExecutor()).run()`
   → `NissanDiagnosticsCapture.from(..., NissanHeadUnit.detected)` → 三分支对话框
   （CaptureFailed 重采提示含信号行 / Undetected 拒绝说明 / Reported 15 行等宽可选
   报告）。测试 `NissanDiagnosticsUiTest` 3 用例（Robolectric 桩注入）+ 5 个新
   字符串 × 9 locale；BDD 规划场景已由实现兑现。~~大字号截图待办~~ **已完成
   （轮次 13，简中为主）**：`docs/screenshots/nissan_full_zh_font150.png` /
   `nissan_compact_zh_font150.png` / `nissan_rtl_ar_font150.png` + 门控基线 ×2，
   几何验证（同高/对齐/无裁剪）见 `NISSAN_TEANA_ACCEPTANCE.md` 第七节。遗留：
   实车验证真实执行器行为。
6. 实车采集完成后：按 `NISSAN_TEANA_HEADUNIT.md` §实车采集清单 的规则，把确认过的
   `ro.product.*` 值扩充进 `NissanHeadUnit.KNOWN_UNIT_IDENTIFIERS`（不是解析器的职责）。
   注意调研发现：NTDA3 世代存在 Android 4.4.3（i.MX6，有实车日志）与 Android 9
   （ARM64）两代平台并存，目标车 getprop 是唯一仲裁。
7. **上游 issue hotfix 交叉验证（2026-10-26 轮次 14 完成，用户指示）**：上游
   `shihabal3amri/DiPlay` #1084（开机自启卡已连接界面，多人复现真实 bug）的修复
   #1085 已在本地 main（9d896330 = upstream main HEAD）中；交叉验证与回归测试
   结论——日产分支对 BootReceiver/LauncherIntentTest 零差异（正交）；
   `LauncherIntentTest` 14 用例 Green；hotfix 基线 common 全量 1672 用例仅 4 个
   已知环境失败；模拟器 boot 广播正路径实证（Android 15，root 广播→新进程→
   DiPlayActivity 前台）。worktree `%TEMP%\DiPlay-hotfix1084` 分支
   `hotfix-1084-boot-return-carplay` 保留。**同类 issue 覆盖性验证（轮次 15）**：
   #1095（自动连接后不自动进入 CarPlay）已被上游作者关闭为 #1084 重复；
   #986（连接后直接打开 CarPlay）经贡献者分析其启动日志与 #1084 同路径——
   **三 issue 同根因，#1085 统一覆盖，无需额外 hotfix**（#986 仍开仅因附带的
   壁纸功能请求）。后续：跟踪上游新 issue（gh CLI 可用；git fetch 对
   github.com 不通时用 `gh api tarball`）；其他本地可测 issue 候选：
   #1119/#1116（热点连接/绿屏，需 iPhone，仅代码级）、#1108/#1075（帧率，
   硬件相关）。**#1119 已定嫌疑（轮次 19）**：上游 open PR **#1071**
   （StartSession IP 载荷修复）即对症修复，与症状链精确闭环；patch 已在
   hotfix worktree 干净应用且 PR 测试全绿（12/0 + 8/0），全量回归零新增
   （shared 1714/0，common 仅 4 个已知失败），工作副本保留未提交；
   待上游合并后按 #1084 模式同步交叉验证。详见 RESEARCH 文档 #1119 两节。
   另新增 `docs/NISSAN_DIAGNOSTICS_USER_GUIDE.md`（验收方/车主视角使用指南）。

## 八、评审中未采纳的建议（记录在案）

- 属性行只在分区外解析（会丢 dumpsys 区内合法属性）→ 不采纳，维持全局解析。
- 重复分区只保留首次出现（会丢真实的第二次采集）→ 不采纳，维持跨块累计。
- dumpsys 首行缩进与内部缩进完全一致化（现约定"仅去首尾空白"已有测试钉住，
  语义清晰）→ 不采纳。
- 宽容 `[key]:[value]`（无空格）手写格式 → 不采纳，保持与 getprop 实际输出一致的
  严格格式。

## 九、新会话长期任务目标（goal 文本，直接粘贴设立）

> 上一会话（2026-10-26，13 轮、92 次操作计数）已按上下文规则封盘交接。新会话请
> 沿用以下目标文本设立 goal（模型**优先 GLM-5.3**，2026-10-26 用户更新）。
> **接管机制（用户确认的 loop 工作方法）**：新会话一旦以下述文本设立目标，即强制
> 接管 loop——旧会话的目标回传被打断，loop 在新会话继续；此后每次交接均按此机制
> 向下传递。

```
长期任务目标（DiPlay 日产天籁适配 loop，2026-10-26 起续推，轮次 30 后）：

在 D:\车机改造DIY\DiPlay 按 docs/NISSAN_TEANA_PARSER_HANDOFF.md 第七节清单续推
日产适配开发，采用 TDD+SDD+BDD 驱动（每轮 Red→Green 留痕、场景↔测试映射）。
当前状态（2026-10-26 轮次 28，操作计数 220）：①诊断链路七件套 + Outcome UI
+ 布局截图 + 端到端模拟器实证全部完成；②日产蓝牙根因已定案（NTDA3 家族系统
蓝牙不可用、BT 在 MCU 后经 com.anwsdk.service 暴露，见 RESEARCH 调研节）；
③**AnwPhoneLink 蓝牙桥接四层端到端接线完成**——纯 JVM 核心（AnwBluetoothBridgeTest
6/6）+ 装配层（AnwPhoneLinkWiringTest 5/5）+ AIDL 三文件（事务号与上游 #880
实机互证）+ CarPlayController 混合门三处窄改（原生分支零变化）；
④全量回归 shared 269 类/1786 用例/0 失败（轮次 30 实测，含拆解样本 3 用例），common 仅 4 个已知 POSIX sh 环境失败 + 1 个已归档 flaky；⑤轮次 29 低负载守卫落地（诊断防重入/销毁不弹窗/桥接监听器释放，资源预算矩阵见 SDD）与轮次 30 真机规格模拟（1280x720+d160 布局验证 + 拆解样本数据集）；
lint 新增代码零命中。验收材料 docs/NISSAN_TEANA_ACCEPTANCE.md（含桥接 16 用例
复验命令）与用户指南 NISSAN_DIAGNOSTICS_USER_GUIDE.md 已就绪。

**持续任务目标（2026-10-26 用户加入 loop）**：针对当前版本设计功能/集成测试
多场景，降低性能与设备要求、减少卡顿/延迟/内存泄漏（场景矩阵与资源预算见
SDD「低负载与资源约束设计」节；轮次 29 已落地三守卫：诊断防重入、销毁不弹窗、
桥接监听器释放——后续切片按预算矩阵扩展）。

最高价值排队项（等待外部输入解锁）：**实车输入**（2021 天籁超智联按
NISSAN_TEANA_HEADUNIT.md §实车采集清单 的 ADB 转录文本）——单项解锁三事：
诊断执行器实车验证、Anw 桥接真机路径验证（服务绑定→SocketConnect→iAP2
交换）、KNOWN_UNIT_IDENTIFIERS 扩充；上游 0.2.22 发布后 #1085/#1071 同步
交叉验证（#1071 若合并则无线上连不上的修复同基线受益）；独立验收执行。

持续要求：操作继续计数记入 docs/NISSAN_TEANA_PARSER_WORKLOG.md（已 220 次）；
TDD+SDD+BDD 留痕；构建 pwsh 7 内联 + X: 映射（交接文档第六节）；模拟器
环境已自装（AVD diplay_nissan_gapi 须 -writable-system 启动）；上下文压缩
超两次即强制交接；不提交不推送；不动他人未提交修改六文件（README.zh-CN.md、
docs/BRANCHING.md、NISSAN_PORT_PLAN.md、NISSAN_TEANA_HEADUNIT.md、
NissanHeadUnit.kt 及其测试）；诊断传闻值（1280x720、NTDA3、P1xx）绝不进
运行时代码；等待独立验收。```



