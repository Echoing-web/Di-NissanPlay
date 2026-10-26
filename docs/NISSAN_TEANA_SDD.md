# 天籁适配设计说明

## 目标

为中国市场 2021 款天籁 2.0T 建立独立的车机识别入口，并提供不改变车机状态的只读诊断报告数据变换，供后续方控、网络和显示适配按需接入。

## 设计

- `NissanHeadUnit` 位于 `shared` 网络层，与现有 `GeelyHeadUnit` 的职责保持一致。
- 识别输入来自 `Build.PRODUCT`、`Build.DEVICE`、`Build.MODEL`、`Build.MANUFACTURER` 和
  `Build.BRAND`。
- `matchesIdentifiers` 是纯匹配函数，单元测试不依赖全局 Android 状态。
- `detected` 使用访问时计算，保证诊断或测试修改 `Build` 后立即生效。
- `KNOWN_UNIT_IDENTIFIERS` 在实车采集前为空，当前只匹配明确包含 `nissan` 的字段。
- `NissanDiagnosticsReport` 接收 `NissanDiagnosticsInput`，只在 `detected == true` 时生成报告；
  缺失或空白字段渲染为 `unknown`。
- 报告按固定顺序渲染 15 行 `key=value`，记录 `ro.product.*`、`ro.build.version.release`、
  `ro.build.version.sdk`、`wm size`、`wm density`、`dumpsys display`、来源和可选平台提示。
- 报告是纯数据变换，不包含时间戳，不连接车机、不执行命令、不写文件；平台提示只作记录，
  不参与 Nissan 识别，也不改变 `KNOWN_UNIT_IDENTIFIERS`。

## 文本解析设计（NissanDiagnosticsTextParser）

- `NissanDiagnosticsTextParser.parse(text, source, detected)` 把纯文本诊断记录（实车采集
  或车主转述的粘贴转录）变换为 `NissanDiagnosticsInput`，供报告层消费。
- `source` 与 `detected` 由调用方提供并原样透传；解析器不做识别、不推断 `detected`、
  不产生 `NissanPlatformHint`。
- 解析器是纯文本变换：不执行命令、不做 I/O、不依赖 Android framework；缺失或空白
  字段保持 `null`，不回填任何传闻默认值。
- 输入契约：行分隔兼容 `\n`、`\r\n` 与 `\r`；七个 `ro.*` 属性行支持 getprop 的
  `[key]: [value]` 与裸 `key=value` 两种写法，任意位置（含任何分区内与首个标记之前）
  都会解析，值去除首尾空白后保留、空白值视为缺失，重复键按后出现者覆盖；分区标记为
  行首（忽略首尾空白、区分大小写）的 `wm size:`、`wm density:`、`dumpsys display:`，
  标记后的同行剩余内容（去首尾空白）视为分区首行；size 分区只保留
  `Physical size:` / `Override size:` 行，density 分区只保留 `Physical density:` /
  `Override density:` 行（保留行同样去首尾空白），维持原始顺序，同名分区跨块累计；
  `dumpsys display` 只在显式标记之后采集，到下一个 wm 标记或输入结束为止，采集文本
  保留内部行与缩进、仅去首尾空白。
- 调用方接线规则（由 2026-10-26 调研导出，见 `NISSAN_TEANA_RESEARCH_20261026.md`
  第二节）：采集用全量 `getprop`（多参数与单键形式都不可用）；粘贴转录给每段加分区
  标签行，未加标签的原始 dumpsys 文本（如以 `DISPLAY MANAGER (dumpsys display)`
  开头的裸输出）不会被视为采集区；wm 分区内的 `Security exception` /
  `Remote exception` / `Error:` 行按噪声丢弃，dumpsys 采集区内的
  `Permission Denial` 等失败信号行原样保留为证据，由调用方检测并提示重采。

## 转录组装设计（NissanDiagnosticsTranscript）

- 调用方侧纯文本组件：把各命令的采集输出组装为带分区标签、可被
  `NissanDiagnosticsTextParser` 直接解析的转录，并检测采集失败信号行供调用方提示
  重采。Android 侧采集流程是独立切片，只组合本组件与解析器。
- 组装规则：`getprop` 段在前，随后依次 `wm size:`、`wm density:`、
  `dumpsys display:` 标签段；段落内容原样（去首尾空白）；空白段落整体跳过，
  不产生空分区；全部空白时返回空转录。
- 信号前缀（行首、区分大小写）：`Permission Denial`、`Security exception`、
  `Remote exception`、`Error:`、`Error with service`、`Can't find service`；
  检测返回首个信号行，无信号返回 null。
- 纯文本变换：不执行命令、不做 I/O、不识别车机、不产生 UI；不与解析器重复实现
  分区或过滤规则（组装只加标签，过滤与采集由解析器决定）。

## 采集编排设计（NissanDiagnosticsCapture）

- 纯数据编排：组合既有纯组件（`NissanDiagnosticsTranscript` 信号检测与组装 →
  `NissanDiagnosticsTextParser` 解析 → `NissanDiagnosticsReport` 生成），
  不新增任何文本处理规则；不执行命令、不做 I/O、不识别车机。
- `detected` 由调用方提供（实车侧取自 `NissanHeadUnit.detected`），不推断。
- 分流优先级：`detected == false` 一律拒绝（`Undetected`，即使采集干净或含信号）；
  存在采集失败信号则要求重采（`CaptureFailed`，按段落顺序收集信号行）；
  否则生成报告（`Reported`）。全部段落空白不算失败：`detected == true` 时生成
  全 unknown 报告。
- Android 侧采集 UI 与真实命令执行是后续薄壳切片：调用本编排并按 `Outcome` 分支
  展示（重采提示 / 拒绝说明 / 报告渲染）。

## 采集命令执行设计（NissanDiagnosticsCommandCapture）

- 命令执行抽象层：`NissanDiagnosticsCommandExecutor`（可注入；`execute` 返回合并
  输出或 null，超时与重试策略由实现自定）+ `NissanDiagnosticsCommands` 四条命令
  常量（AOSP 调研验证的形式：全量 `getprop`——多参数形式不可用；`wm size`、
  `wm density`、`dumpsys display`）+ `NissanDiagnosticsCommandCapture.run()` 按
  固定顺序采集四段，输出直接对应 `NissanDiagnosticsCapture.from` 的四个段落参数。
- 单条命令失败（null）不阻塞其余采集；null 段在组装时按空白段落跳过。
- 纯 JVM 测试注入桩执行器，不依赖实车或模拟器；真实执行器（Runtime.exec / shell）
  由 Android 薄壳提供，本切片不实现。
- 非目标：不在本层做信号检测、解析、报告（分别在 Transcript / Parser / Capture）。

## 真实命令执行器设计（NissanDiagnosticsShellCommandExecutor）

- 实现 `NissanDiagnosticsCommandExecutor`：白名单（仅 [NissanDiagnosticsCommands]
  四条只读命令，其余字符串一律返回 null 且不启动进程，杜绝注入）；超时（默认
  10 秒，超时销毁 destroy→destroyForcibly 并返回 null，不读取输出）；输出合并
  （stdout/stderr 后台线程并发读取，避免管道写满死锁；去首尾空白，空白返回 null）。
- 失败不抛出：启动异常、超时、无输出一律返回 null，单条失败不阻塞其余采集。
- API 24 兼容（2026-10-26 轮次 15 lint 修复）：`Process.waitFor(long, TimeUnit)`、
  `isAlive`、`destroyForcibly` 均为 API 26+，而项目 minSdk=24——超时等待改为
  `exitValue()` 轮询（10ms 间隔，中断即返回当前状态），`destroyForcibly` 仅在
  `SDK_INT >= O` 守卫内调用（旧平台 `destroy()` 即强杀语义）。修复后日产文件
  在 `:shared:lintDebug` 零命中（模块剩余 33 个 error 为既有代码存量债务，
  不在本 loop 范围）。
- `ProcessLauncher` / `LaunchedProcess` 为可注入进程抽象：真实实现用
  `sh -c <白名单命令>`；纯 JVM 测试注入桩进程，不启动真实进程。
- 真实进程行为验证分层（2026-10-26 轮次 13 模拟器实证）：白名单、超时、输出合并
  在纯 JVM 已验证；Android 15 模拟器（google_apis AVD，`ro.product.manufacturer=NISSAN`）
  实机进程内执行已验证 `sh` 路径与运行时语义——四条命令真实执行，`getprop` 全量
  输出正常采集；`wm size`/`wm density` 对 **app uid** 报 `Can't find service:
  window`（非信号前缀，按接线规则静默丢弃）；`dumpsys display` 报
  `Permission Denial`（信号，保留为证据）→ `CaptureFailed` 分支真实触发
  （截图 `docs/screenshots/nissan_e2e_capture_failed_real.png`）。注意：只读命令
  "对 shell 可读"的假设仅在 shell/root uid 成立；**app uid 下 wm/dumpsys 受限**，
  实车行为取决于车机对 DiPlay 的签名/权限配置——实车验证仍待做，但预期路径
  （失败信号采集与重采提示）已被模拟器完整走通。

## Outcome UI 分支设计（已实现，2026-10-26 轮次 12；轮次 13 补门控用例与实证）

- 薄壳 UI 职责：触发采集 → `NissanDiagnosticsCommandCapture(executor).run()` →
  `NissanDiagnosticsCapture.from(output..., source, NissanHeadUnit.detected)` →
  按 Outcome 三分支展示：`CaptureFailed` → 重采提示（列出信号行）；
  `Undetected` → 拒绝说明（不引导重采）；`Reported` → `report.render()` 逐行
  展示/导出。
- 实现约束（AGENTS.md）兑现：Diagnostics 类别（DIAGNOSTICS 卡片内门控按钮，不动
  Settings IA）；5 个新字符串 × 9 locale（`SettingsTranslationsTest` 强制并通过）；
  紧凑/完整大字号 + RTL 截图（`docs/screenshots/nissan_*.png`，几何验证）；
  `NissanDiagnosticsShellCommandExecutor` 在车机进程内执行（app uid 权限现实
  见上节）。测试 `NissanDiagnosticsUiTest` 4 用例（三分支 + 门控不显示）。

## 非目标

- 不在本切片中猜测具体固件版本、分辨率或 ADB 能力。
- 不在报告中生成时间戳或使用 `androidVersion` 别名；Android 版本字段保持原始键名。
- 不在识别未验证前启用方控、热点、仪表或启动器行为。
- 不修改 DiLink、Geely 或其他车机的既有路径。
- 文本解析不识别平台串、不猜测计划外的命令输出（如 `dumpsys window`、`getevent`），
  也不宽容无空格的 getprop 手写格式；保持与实机输出一致的严格契约。

## 证据与升级条件

实车通过 ADB 采集 `ro.product.*`、Android 版本、屏幕尺寸和网络能力后，才能扩充识别列表或
进入下一条适配切片。每条新能力必须先补 BDD 场景与 TDD 测试，再接入运行代码。

诊断报告当前只验证输入记录和稳定渲染，不把车主报告或公开资料提升为实车事实。

`NissanDiagnosticsTextParserTest.kt` 的定向单元测试通过；解析器保持纯文本变换，
场景到测试的映射见 BDD 文档验收证据。解析器在独立验收通过并补齐调用方切片前，
不接入运行代码。

## AnwPhoneLink 蓝牙桥接设计（规划，2026-10-26 轮次 23；依据 RESEARCH「日产车机蓝牙连接问题调研」）

- 背景：NTDA3 家族（逍客/轩逸/奇骏/天籁）的 Android 系统级蓝牙不可用，蓝牙在
  MCU 后、经厂商服务（`com.anwsdk.service`/`com.hsae.bluetoothservice`）暴露；
  上游无线 CarPlay 依赖原生 BT 的 iAP2 发现/认证，原生路径必然失败。
- 检测门（顺序）：①原生 adapter 可用 → 原生路径（不引入桥接）；②原生
  off/缺失 且 AnwPhoneLink 厂商服务可达（包名存在 ≠ ABI 兼容，须探测 binder
  descriptor `com.anwsdk.service.IAnwPhoneLink`）→ 桥接路径；③两者皆无 →
  明确报错「无线 CarPlay 需要蓝牙，请用有线 USB」（fail-fast，不空等超时——
  Legacy #79 已验证的用户体验要求）。
- 适配层：厂商 AIDL 字节流 ↔ 上游阻塞双工字节流等价接口，上层 iAP2/认证/
  AirPlay 全复用（#880 验证此边界可行）；有界 FIFO 缓冲、保持部分读顺序、
  零字节写不算成功、异步连接成功须轮询 connection info 确认目标地址与状态。
- ABI 防御：Anw 事务号非公共 ABI（#880 明示「checked against this unit's
  service bytecode」），必须按机型校验并允许禁用；一次桥接会话只占用一个
  未用的厂厂 Socket 子系统，失败/关闭时断开 socket、反注册回调、去初始化，
  不改任何厂厂 APK/固件/配对记录。
- 实现与验证路线：轮次 24 起分析 winkinwong/DiPlay-Legacy-Android-T32 的实机
  验证实现并与 #880 outline 对勘 → 产出移植规格 → TDD（检测门三分支 + 字节流
  适配等价性，全部纯 JVM 桩 Anw 服务）→ 实车验证（真机 ABI 不可桩测）。
- 非目标：不修改车机固件或厂商应用；不把事务号当作稳定 ABI 写死在运行时
  常量外的任何假设；不在实车采集确认前声称对具体年款的支持。

### AnwPhoneLink 桥接：T32/#880 对勘补强（2026-10-26 轮次 24）

- 检测门取 T32 形态（服务 action 解析 `queryIntentServices`，包名+action 双匹配），
  并入 #880 的连接异步确认（轮询 connection info 校验地址与状态）与配对列表
  NAMES-first/ADDRESSES-second 解析顺序防御。
- 厂商调用取 AIDL 命名方法（IAnwPhoneLink 的 ANWBT_Socket*/SPP* 方法），不用
  裸 transact 事务号——binder stub 自带接口版本容错（T32 实机路线）。
- 适配层落位：`CarPlayBluetooth` 替身 + `BlockingDuplexByteStream` 实现（上游
  两个既有接口，上游协议栈零改动）；接入点为 `CarPlayBluetoothManager`
  混合门（原生优先→厂商回落）+ `selectVendorWirelessTarget`（手动地址→已连接
  →唯一配对 iPhone，多 iPhone 抛错镜像原生）。
- 实现前先按 BDD 四场景补 TDD（全部纯 JVM 桩 AIDL），规格细节与代码证据见
  RESEARCH「T32 Anw 桥接实现分析与移植规格草案」节。

## 低负载与资源约束设计（2026-10-26 轮次 29，用户目标：卡顿/延迟/内存泄漏防护）

目标平台约束：NTDA3 家族 i.MX6 低配（单/双核 A9、1GB 级内存、Android 4.4–10）
——设计预算：**不因诊断/桥接功能引入常驻服务、常驻线程或常驻监听器**。

### 场景组与守卫（功能 + 集成测试矩阵）

| 场景组 | 守卫 | 测试 |
| --- | --- | --- |
| 诊断入口防重入 | 一次点击最多一组子进程（四条命令 ×10s 上限），重复点击忽略 | `repeatedTapsRunTheCaptureOnlyOnce` |
| 诊断对话框生命周期 | 采集期间 Activity 销毁则不弹窗（防 WindowManager 泄漏/崩溃） | `outcomeAfterActivityDestroyedIsDropped` |
| 桥接流监听器释放 | close 后厂商回调不再入队（引用不滞留） | `closedStreamReleasesItsVendorListener` |
| 桥接流超时语义 | recv 超时 null / 对端结束空数组 / 单缓冲消费 | Bridge 既有 6 用例 |
| 执行器进程清理 | 超时销毁（destroy）且不读输出；启动失败/空白不残留 | Executor 桩既有 4 用例 |
| 探针惰性与降级 | 混合门探针崩溃降 None；vendorBridge lazy 每控制器一次 | Planner 5 用例 |
| 解析吞吐 | 1.4MB 输入 0.041s（≈34MB/s 量级），实车转录（≤200 行）远低于预算 | 冒烟既有 |

### 资源预算条目（后续切片守卫基线）

- 子进程：同一时刻 ≤4（仅用户点击窗口内），串行执行，每个 ≤10s。
- 线程：采集期间 ≤2 短命线程（执行 + 读流守护线程池复用 JVM 默认）；桥接
  会话期间 ≤1 读回调 binder 线程（厂商侧）。无常驻。
- 内存：诊断报告 ≤1.4MB 峰值（冒烟钉住）；桥接 FIFO 默认无上限——**后续
  切片若有真实厂商流背压证据再设界**（当前实测形态：iAP2 短帧）。
- 延迟：检测探针每次无线启动一次（lazy 缓存于控制器生命周期）；诊断按钮
  点击到对话框 = 子进程实际耗时（无轮询空转）。

## 代际决策门（轮次 33，P0-2）

- 决策门（承接「证据与升级条件」节，动机=PRD 断点 #1）：实车 ADB 采集的
  `ro.build.version.sdk` 若为 **19**（i.MX6 / Android 4.4 世代），主线 DiPlay
  （minSdk=24）**不可安装**，适配路线转 DiPlay-Legacy-Android fork——PRD R8
  显示该世代需要的补丁面（VPN 权限对话框缺失、NCM NTB 超时、USB 弹窗死锁）
  主线完全没有；且 PRD R1 证明同家族固件可把版本显示造假为 Android 10/11/12，
  故 `ro.build.version.release` 不能作代际依据。
- `ro.build.version.sdk` 是**代际唯一仲裁字段**：只来自实车采集输入，缺失渲染
  `unknown`，禁止被任何公开资料值（README、车主转述、论坛报告）覆盖——沿用
  BDD「缺失诊断值使用 unknown」契约；该行为已由既有样本测试
  `NissanTeanaRealWorldSampleTest` 钉住（4.4.3/sdk=19 与 Android 9/sdk=28
  双平台样本，见 RESEARCH「真机拆解样本数据集与样本模拟测试」节）。
- 升级条件：仅当实车采集的 sdk ≥ 24 时回到主线适配路线；在此之前不为
  API 19 世代在主线新增任何运行时兼容代码（由 Legacy fork 路线承载）。
