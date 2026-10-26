# 天籁适配行为规格

本文件描述日产车机识别和只读诊断报告切片。方控、热点、分辨率和启动器接入不属于本切片。

## 场景：识别日产车机

**Given** 系统标识的产品、设备、型号、制造商或品牌字段中至少一个包含 `Nissan`

**When** 应用读取 `NissanHeadUnit.detected`

**Then** 结果为 `true`，日产专用功能才允许继续判断

## 场景：拒绝非日产车机

**Given** 系统标识属于 Robolectric 默认值、比亚迪或吉利，且没有日产标识

**When** 应用读取 `NissanHeadUnit.detected`

**Then** 结果为 `false`，不得打开日产专用功能

## 场景：诊断状态变化后重新判断

**Given** 测试或诊断过程修改了 Android `Build` 字段

**When** 应用再次读取 `NissanHeadUnit.detected`

**Then** 结果基于当前字段重新计算，不使用旧的初始化快照

## 场景：生成稳定诊断报告

**Given** `detected == true`，并提供车机标识、`ro.build.version.release`、
`ro.build.version.sdk`、`wm size`、`wm density` 和 `dumpsys display` 摘要

**When** 调用 `NissanDiagnosticsReport.from(input)` 并渲染报告

**Then** 输出固定 15 行的 `key=value` 文本，字段顺序稳定；来源使用 `on-car` 或
`owner-reported`，不生成时间戳，不把 `ro.build.version.release` 改名为
`androidVersion`

## 场景：缺失诊断值使用 unknown

**Given** 任一诊断字段缺失或只包含空白

**When** 渲染诊断报告

**Then** 对应字段输出 `unknown`，不回填 `1280x720`、`NTDA3` 或 `P1xx` 等传闻值

## 场景：拒绝未识别车机的报告

**Given** `detected == false`

**When** 调用 `NissanDiagnosticsReport.from(input)`

**Then** 返回拒绝结果，不生成报告

## 场景：平台提示只作记录

**Given** 输入包含 `NissanPlatformHint`

**When** 生成诊断报告

**Then** 平台值和自身来源原样进入 `platform_hint` 与 `platform_hint_source`，
不改变 `NissanHeadUnit.detected`，也不扩充 `KNOWN_UNIT_IDENTIFIERS`

## 场景：解析实车文本记录

**Given** 一段纯文本诊断记录，包含 getprop 括号格式的 `ro.*` 属性行、`wm size:` 与
`wm density:` 分区（各含 Physical/Override 行）以及 `dumpsys display:` 分区，调用方
提供 `source` 与 `detected`

**When** 调用 `NissanDiagnosticsTextParser.parse(text, source, detected)`

**Then** 七个 `ro.*` 属性进入对应字段；wm 分区只保留本分区的 Physical/Override 行并
维持原始顺序；`dumpsys display` 只在显式标记之后采集，到下一个 wm 标记或输入结束为止

## 场景：缺失与噪声不产生字段

**Given** 记录缺失部分属性、属性值为空白、wm 分区只有噪声行，或没有显式
`dumpsys display:` 标记

**When** 解析文本记录

**Then** 缺失字段保持 `null`，噪声行被丢弃，无标记时不采集 dumpsys，绝不回填
`1280x720`、`NTDA3` 等传闻默认值

## 场景：解析不改变识别结论

**Given** 记录中满是 Nissan 字样，调用方提供 `detected == false`

**When** 解析文本记录

**Then** 输出的 `NissanDiagnosticsInput.detected` 仍为 `false`，
`NissanDiagnosticsReport.from` 对其拒绝生成报告

## 场景：分区标记的边界

**Given** 标记大小写不符（`WM SIZE:`）、标记文本嵌在行中、标记与内容写在同一行
（`wm size: Physical size: 800x1280`），或同一分区出现多次

**When** 解析文本记录

**Then** 大小写不符或嵌在行中的文本不是标记；同行剩余内容作为该分区首行；同名分区
跨块按原始顺序累计

## 场景：调用方转录规则

**Given** 调用方提供全量 `getprop` 输出的转录（含无关属性与空值），为 wm 与 dumpsys
段添加分区标签，且采集输出中夹杂失败信号行

**When** 解析文本记录并生成报告

**Then** 只有七个目标属性进入字段与报告；wm 分区的 `Security exception` /
`Remote exception` / `Error:` 行被丢弃；dumpsys 采集区的 `Permission Denial` 等
失败信号行原样保留为证据，供调用方提示重采

## 场景：组装带分区标签的转录

**Given** 各命令的采集输出（部分段落可能缺失或为空白）

**When** 调用 `NissanDiagnosticsTranscript.assemble(...)`

**Then** 输出带分区标签的转录：空白段落不产生空分区；组装结果可被
`NissanDiagnosticsTextParser` 完整还原（字段与源段落一致）

## 场景：检测采集失败信号

**Given** 采集输出中包含 `Permission Denial` / `Security exception` /
`Remote exception` / `Error` 类信号行（可能在任意行）

**When** 调用 `NissanDiagnosticsTranscript.captureFailureSignal(...)`

**Then** 返回首个信号行供调用方提示重采；无信号时返回 null；信号行组装后仍
原样保留在 dumpsys 采集区作为证据

## 场景：干净采集生成诊断报告

**Given** 四段采集输出干净（无失败信号），调用方提供 `detected == true` 与来源

**When** 调用 `NissanDiagnosticsCapture.from(...)`

**Then** 返回 `Reported`，报告逐行包含各字段的采集值

## 场景：采集失败信号要求重采

**Given** 任一采集段包含失败信号行

**When** 调用 `NissanDiagnosticsCapture.from(...)`

**Then** 返回 `CaptureFailed`，按段落顺序返回信号行，不生成报告，供调用方提示重采

## 场景：未识别车机一律拒绝

**Given** `detected == false`（无论采集干净与否、有无失败信号）

**When** 调用 `NissanDiagnosticsCapture.from(...)`

**Then** 返回 `Undetected`，不做任何生成或重采引导

## 场景：执行四条采集命令

**Given** 注入的命令执行器（桩或真实实现）

**When** `NissanDiagnosticsCommandCapture.run()`

**Then** 按固定顺序执行 `getprop` / `wm size` / `wm density` / `dumpsys display`
四条命令（全量 getprop 形式），各段输出与执行器返回一一对应

## 场景：单条命令失败不阻塞采集

**Given** 执行器对某条命令返回 null（失败或无输出）

**When** 运行采集

**Then** 该段保持 null，其余段落照常采集；null 段在后续组装中按空白段落跳过

## 场景：执行器输出直接进入编排分流

**Given** 执行器输出（干净或含失败信号）

**When** 采集结果交给 `NissanDiagnosticsCapture.from`

**Then** 干净输出 → `Reported`；含信号输出 → `CaptureFailed`（执行器→编排全链路）

## 场景：白名单外的命令不执行

**Given** 执行器收到非四条采集命令的任意字符串（含拼接了额外参数或命令的变体）

**When** 调用 `NissanDiagnosticsShellCommandExecutor.execute(...)`

**Then** 不启动任何进程，直接返回 null（杜绝命令注入）

## 场景：命令执行超时

**Given** 白名单命令的进程在时限内未结束

**When** 调用 `execute(...)`

**Then** 销毁进程并返回 null，不读取输出

## 场景：命令输出采集

**Given** 白名单命令正常结束且输出非空（或启动失败 / 输出空白）

**When** 调用 `execute(...)`

**Then** 返回合并输出（去首尾空白）；启动失败或输出空白返回 null

## Outcome UI 分支场景（2026-10-26 轮次 12 实现，轮次 13 补门控用例与模拟器实证）

以下场景是 SDD「Outcome UI 分支设计」的规格化，已由 `common` 模块
`NissanDiagnosticsUiTest`（Robolectric，桩注入采集链路）验证。

### 场景：采集失败提示重采

**Given** 采集结果为 `CaptureFailed`（含按段序收集的信号行）

**When** 用户查看诊断界面

**Then** 展示重采提示并列出全部信号行，不展示报告

### 场景：未识别车机拒绝说明

**Given** 采集结果为 `Undetected`

**When** 用户查看诊断界面

**Then** 展示拒绝说明，不引导重采，不展示报告

### 场景：报告渲染

**Given** 采集结果为 `Reported`

**When** 用户查看诊断界面

**Then** 逐行展示 `report.render()` 的 15 行 `key=value` 文本，可复制/导出

### 场景：门控不显示（轮次 13 补）

**Given** `NissanHeadUnit.detected=false`（非日产标识）

**When** 用户打开 Diagnostics 类别页

**Then** 不渲染日产诊断按钮（其余诊断功能不受影响）

测试映射（`NissanDiagnosticsUiTest`，4 用例）：采集失败提示重采 →
`captureFailedShowsRecapturePromptWithSignals`；未识别拒绝说明 →
`undetectedShowsRefusalWithoutRecapture`；报告渲染 →
`reportedRendersFifteenLines`；门控不显示 →
`nissanButtonHiddenWhenHeadUnitNotDetected`（钉住现有门控行为）。

模拟器实证（轮次 13）：google_apis AVD（注入 `ro.product.manufacturer=NISSAN`）
真实点击按钮走完整真实链路——四条命令经真实 `sh` 执行，getprop 全量输出正常
采集、wm 失败静默丢弃、dumpsys `Permission Denial` 信号保留，`CaptureFailed`
重采提示对话框展示真实信号行（截图
`docs/screenshots/nissan_e2e_capture_failed_real.png`）；default 镜像
（manufacturer=unknown）实证门控基线（无按钮，`baseline_*.png`）。

## 验收证据

- `shared/src/test/.../NissanHeadUnitTest.kt` 的定向单元测试通过。
- 未采集实车 `ro.product.*` 前，不把具体产品值写入正式识别列表。
- `NissanDiagnosticsReportTest.kt` 的定向单元测试通过；报告类保持纯数据变换，
  不依赖 Android framework、ADB、进程执行或文件写入。
- `NissanDiagnosticsTextParserTest.kt` 的定向单元测试通过；解析器保持纯文本变换
  （不执行命令、不做 I/O、不依赖 Android framework、不推断 `detected`）。
- 文本解析场景 ↔ 测试映射：
  - 解析实车文本记录 → `getprop括号格式的属性被解析且来源与识别结论原样透传`、
    `裸key等号格式的属性同样被解析且命令文本只作数据`、
    `wm分区只保留Physical与Override行且维持原始顺序`、
    `dumpsys在显式标记后采集并在wm标记或输入结束处停止`、
    `孤立回车也按行分隔处理`、`属性值包含分隔符序列不产生歧义`、
    `属性值去除首尾空白后保留`
  - 缺失与噪声不产生字段 → `缺失或空白的属性保持null`、
    `wm分区无Physical或Override行时保持null`、`wm分区仅有Override行时只保留该行`、
    `无显式标记时不得采集dumpsys`、`空白输入返回全null且透传来源与识别结论`
  - 解析不改变识别结论 → `detected由调用方提供解析器绝不推断`、
    `解析是纯文本变换且不产生platformHint`
  - 分区标记的边界 → `分区标记区分大小写`、`标记文本嵌在行中不构成标记`、
    `标记行内联的分区内容按首行处理`、
    `同一分区多次出现时跨块按原始顺序累计`、
    `Physical与Override按分区匹配前缀`、`属性行在任意分区被解析且重复键后值覆盖`、
    `属性值保留等号与右括号且键前缀不越界`
  - 调用方转录规则 → `全量getprop转录只提取七个目标属性`、
    `采集失败信号按噪声丢弃或保留为证据`（标签必需性另由
    `无显式标记时不得采集dumpsys` 钉住）
- 性能与规模化守卫：`大输入结构化冒烟测试`——约 1.4MB 合成转录逐项断言解析结果
  （结构断言为主，附数量级时序上限），拦截 O(n²) 类回归；不做基准测试（JMH 留在
  单元套件之外）。
- `NissanDiagnosticsTranscriptTest.kt` 的定向单元测试通过；组装与信号检测保持纯
  文本变换。场景 ↔ 测试映射：
  - 组装带分区标签的转录 → `组装带分区标签的完整转录`、
    `空白段落被跳过不产生空分区`、`组装结果可被解析器完整还原`
  - 检测采集失败信号 → `检测采集失败信号行`、`失败信号行组装后保留为证据`
- `NissanDiagnosticsCaptureTest.kt` 的定向单元测试通过；编排不新增文本规则。
  场景 ↔ 测试映射：
  - 干净采集生成诊断报告 → `四段干净采集生成报告`
  - 采集失败信号要求重采 → `失败信号要求重采且不生成报告`
  - 未识别车机一律拒绝 → `detected为false时拒绝生成`、
    `全部段落空白时生成全unknown报告或拒绝`（含拒绝优先于重采的断言）
- `NissanDiagnosticsCommandCaptureTest.kt` 的定向单元测试通过（桩执行器，不依赖
  实车环境）。场景 ↔ 测试映射：
  - 执行四条采集命令 → `四条采集命令按固定顺序执行`、
    `各段输出与执行器返回一一对应`
  - 单条命令失败不阻塞采集 → `单条命令失败不阻塞其余采集`
  - 执行器输出直接进入编排分流 → `执行器输出直接进入编排分流`
- `NissanDiagnosticsShellCommandExecutorTest.kt` 的定向单元测试通过（桩进程，
  不启动真实进程）。场景 ↔ 测试映射：
  - 白名单外的命令不执行 → `白名单外的命令不执行进程`
  - 命令执行超时 → `超时销毁进程并返回null`
  - 命令输出采集 → `白名单命令正常结束返回输出`、
    `启动失败或无输出返回null且四条命令均放行`

## AnwPhoneLink 蓝牙桥接场景（2026-10-26 轮次 25 实现，纯 JVM 桩 AIDL 验证）

以下场景是 SDD「AnwPhoneLink 蓝牙桥接设计（规划）」的规格化，实现前不得视为
已验证；建议 TDD 用例（common/shared 纯 JVM，桩 Anw 服务）：

### 场景：原生蓝牙可用时走原生路径

**Given** Android 蓝牙 adapter 可用
**When** 无线连接启动
**Then** 走既有原生 RFCOMM 路径，桥接代码完全不参与

### 场景：原生不可用且 Anw 服务可达时走桥接

**Given** adapter off/缺失，且 AnwPhoneLink binder 服务探测成功
**When** 无线连接启动
**Then** 经厂商 AIDL 建立 RFCOMM 字节流，适配为原生等价接口，上层 iAP2 无感知

### 场景：两者皆无时明确报错

**Given** adapter 不可用且 Anw 服务不可达
**When** 无线连接启动
**Then** 立即报错「无线 CarPlay 需要蓝牙，请用有线 USB」，不空等超时

### 场景：桥接失败时清理并回落报错

**Given** 桥接会话中连接/写失败
**When** 失败发生
**Then** 断开占用 socket、反注册回调、去初始化厂厂 Socket 子系统，报可操作错误

建议用例名：`nativeBluetoothPresentKeepsNativePath`、
`anwServiceReachableBridgesWhenNativeAbsent`、
`noBluetoothPathFailsFastWithUsbGuidance`、`bridgeFailureCleansUpAndReports`。


测试映射（`shared/src/test/.../bluetooth/AnwBluetoothBridgeTest.kt`，6 用例，
2026-10-26 轮次 25，Red→Green 留痕见 worklog）：原生可用走原生 →
`nativeBluetoothPresentKeepsNativePath`；Anw 可达走桥接 →
`anwServiceReachableBridgesWhenNativeAbsent`（含 UUID→GUID 字节序钉子）；
皆无明确报错 → `noBluetoothPathFailsFastWithUsbGuidance`；桥接失败清理并
报错 → `bridgeFailureCleansUpAndReports`（失败传播 + close 断开占用 socket）；
字节流语义（部分读保序/超时 null/对端结束空数组/单缓冲消费与系统调用一致）→
`bridgedStreamKeepsVendorByteStreamSemantics`；目标选择镜像 framework 顺序
（手动地址→已连接→唯一 iPhone，多 iPhone 抛错）→
`targetSelectionMirrorsFrameworkOrder`。实车 AIDL 绑定层（binder/服务解析）
不在纯 JVM 覆盖内，按 SDD 接入说明装配后待实车验证。

装配层映射（`AnwPhoneLinkWiringTest`，5 用例，2026-10-26 轮次 26）：
原生可用走原生 → `hybridGatePrefersNativeWhenBothResolve`；Anw 可达走桥接 →
`serviceProbeHitEnablesVendorCandidate`；皆无明确报错 →
`serviceProbeMissKeepsVendorUnsupportedAndManagerReturnsNull`；探针双匹配 →
`probeAsksForTheAnwPhoneLinkServiceIntent`（action+包名钉子）；UUID 字节序 →
`uuidGuidBytesPinTheAnwByteOrder`（#880 GUID 序显式断言）。AIDL 定义三文件
（IAnwPhoneLink 事务号 222–233/62–69，与 #880 实机对勘一致）已移植编译；
控制器接入待「无线 BT 依赖抽象化」切片（轮次 27）后窄改接入。

混合门映射（`WirelessBluetoothPlannerTest` 5 用例 + 控制器接线，2026-10-26
轮次 27b）：原生可用走原生 → `nativeAdapterPresentAlwaysWins`；Anw 可达走
桥接 → `missingNativeWithVendorSupportBridges`；皆无明确报错 →
`missingNativeWithoutVendorSupportIsNone` + `missingNativeAndMissingBridgeIsNone`；
探针崩溃不破坏门 → `vendorProbeFailureDegradesToNone`。控制器三处窄改
（预检放行 / 决策两型 / 流建立两型——原生 else 分支零变化，vendor 流与
`Iap2Session.openWireless` 消费点统一）由全量回归守卫（shared 268 类/1782
用例/0 失败）。实车端到端（服务绑定→SocketConnect→iAP2 交换）待实车验证。

## 低负载与资源约束场景（2026-10-26 轮次 29 实现）

### 场景：重复点击不并发采集

**Given** 用户在采集进行中再次点击日产诊断按钮
**When** 第二次点击发生
**Then** 忽略该点击（同一时刻只有一组子进程）→ `repeatedTapsRunTheCaptureOnlyOnce`

### 场景：销毁后不弹窗

**Given** 采集进行中用户退出设置页
**When** 采集完成后回调到主线程
**Then** Activity 已销毁/ finishing 则不显示对话框 → `outcomeAfterActivityDestroyedIsDropped`

### 场景：桥接关闭即释放

**Given** 厂商流已 close
**When** 厂商回调继续送达数据
**Then** 不再入队滞留 → `closedStreamReleasesItsVendorListener`

性能与资源预算矩阵见 SDD「低负载与资源约束设计」节（场景组 × 守卫 × 测试全表）。

## 代际仲裁字段钉子（P0-2，轮次 33）

### 场景：sdk 字段仅来自采集输入

**Given** 一段实车采集转录，`ro.build.version.sdk` 有值（如 19 或 28）或缺失

**When** 解析转录并渲染诊断报告

**Then** `ro.build.version.sdk` 只取转录中的采集值，缺失渲染 `unknown`，绝不由
公开资料（README、车主转述、论坛报告）回填——它是代际决策门的唯一仲裁字段
（见 SDD「代际决策门」节与「缺失诊断值使用 unknown」场景）

既有测试映射（无需新增测试）：`NissanTeanaRealWorldSampleTest`（shared）已钉住
该行为——4.4.3 样本 `teanaAndroid44SampleTranscriptParsesEndToEnd`（sdk=19
进报告）与 Android 9 第二平台样本
`teanaAndroid9SecondPlatformSampleParsesEndToEnd`（sdk=28 进报告、缺失 dumpsys
渲染 unknown）双平台覆盖「采集值原样呈现、缺失不猜测」。
