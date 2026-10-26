# 日产诊断文本解析器工作日志

## 会话封盘记录（2026-10-26，依据上下文规则）

- 用户指示「依据上下文规则执行」：本会话上下文压缩已超过两次，正式按规则封盘，
  在本项目内交接给新会话。
- 用户确认 loop 工作方法：**本会话目标文本作为交接内容回传给新会话目标**——goal
  文本已设计完成并存入 `docs/NISSAN_TEANA_PARSER_HANDOFF.md` 第九节（可直接粘贴
  设立新会话 goal），并在持久记忆建立指针（nissan-teana-loop-goal）。
- 用户进一步指示「自行开启新会话（本项目内，非 subagents）」与「模型优先 GLM-5.3」：
  已通过 **CronCreate 一次性近即时自动化**（automation-942428f8-88d0-4b65-b213-906b09a403fe，
  2026-10-26 03:17 本地时间后 1 分钟触发，recurring=false）在工作区开启全新上下文
  会话，任务书 = 第九节 goal 文本 + 轮次 12（Outcome UI 分支：先探查 common 集成
  配方 → TDD Red 三用例 → 实现三分支 UI → 全量验证 → 更新本日志与交接文档）。
  这是本项目 loop 的标准会话开启机制：由会话侧通过定时自动化开启新会话；定时
  运行自身不得再创建 automation（工具约束），链式后续轮次由用户或 Automations
  页面按第九节 goal 文本安排（或由任一交互会话再开启）。
- **会话开启机制更新（同日，用户指示「安装cli，完成后用该方式开启新会话」）**：
  全机遍历未发现独立 CLI（winget 的 ZhipuAI.ZCode 即桌面版本体；npm 的 zcode/
  zcode-cli 为占位包），已安装官方桌面运行时的终端客户端 **zcode-app-cli
  3.16.3-39**（npm install -g zcode-app-cli，Node 26.5.0 满足 ≥22.19）。loop 会话
  开启机制自此更新为 CLI 无头会话：`zcode -p <任务书> --cwd <仓库> --mode yolo
  --target <goal 文本> --model account:zai-start-plan/GLM-5.3`；轮次 12 收尾即由
  该方式启动的新会话执行（轮次 12 的 UI 切片本体已在本会话完成：TDD Red→Green
  3/3 + 9 locale 字符串 + 翻译/布局套件全过，全量回归与文档收尾交由新会话）。
- 封盘时最后一轮（轮次 11）已把 Outcome UI 切片的规格先行部分完成（BDD 规划场景 +
  建议用例名）；轮次 12 曾派出的 common 模块 UI 集成配方探查代理因 goal 暂停被
  取消——新会话首切片（Outcome UI 分支）应先重做该探查（任务书第 2 步），再按
  BDD 规划场景补 TDD 用例、实现三分支 UI。
- 仓库终态：shared 全模块 265 类 / 1766 用例 / 0 失败（轮次 11 后实测核验）；
  既有他人修改零触碰；全部新增文件未跟踪、未提交、未推送。

## 会话管理备注（2026-10-26 补充）

- 长期 loop 任务（TDD+SDD+BDD 持续推进）当前处于**暂停**状态，恢复前不得继续推进。
- 用户规定：**coding 部分的任务会话/派发子代理，模型上限为 GLM-5.3-Flash**；
  交接新会话时请按此选择模型档位。已同步写入持久记忆（coding-session-model-policy）。
- 下一轮（恢复后）的既定路线：并行调研（wm/getprop/dumpsys 输出格式、日产超智联
  社区数据、Kotlin 解析性能实践）→ 为解析器补 SDD/BDD 条目 → 研究结论转 TDD 用例
  → 多轮交叉验证 + 性能冒烟 → 更新本日志的操作计数（目标累计 ≥20 次开发+测试操作）。

## 2026-10-26

### 轮次 1：TDD 初版（严格白名单，仅新增两个文件）

- 前置：确认 cwd 为 `D:\车机改造DIY\DiPlay`；通读 `NissanDiagnosticsReport.kt`
  （`NissanDiagnosticsSource`、`NissanDiagnosticsInput`、unknown 渲染）与
  `NissanHeadUnit.kt`（识别职责、`KNOWN_UNIT_IDENTIFIERS` 空列表）。
- Red：先写 `NissanDiagnosticsTextParserTest.kt`（10 用例）并运行 →
  `:shared:compileDebugUnitTestKotlin FAILED`，13 处
  `Unresolved reference 'NissanDiagnosticsTextParser'`，`BUILD FAILED in 2s`。
  证据：`%TEMP%\nissan_parser_red.log`。
- Green：写最小实现（整行标记、wm Physical/Override 过滤、dumpsys 显式标记采集、
  属性两种格式、空白→null）→ 定向 10/10 通过（JUnit XML：tests=10 failures=0）。
- 回归：`:shared:testDebugUnitTest` 全量 261 类 / 1735 用例 / 0 失败。
- CI：运行 AGENTS.md 全量命令。`:mobile|home|maphost` 的 `lintDebug`、`assembleDebug`
  及 `:home:testDebugUnitTest` 通过；`:common:testDebugUnitTest` 5 用例失败。
- 失败归因（对照实验：将两个新文件移出后重跑，失败依旧）：
  - `UsbPermissionSetupTest`×4：`Cannot run program "sh"`——Windows 本机无 POSIX sh，
    环境性失败，与本改动无关；
  - `CarPlayHostClusterStartupTest`×1：Mockito 验证偶发失败，基线重跑通过（flaky）。
- 纪律核查：`git status` 与会话开始快照逐字一致，仅多两个未跟踪新文件；未 add /
  commit / push；文档未动；`NissanHeadUnit.kt` 未动。
- 构建环境发现：`D:\dev-env\build-diplay.bat`（JDK21 + JDK25 工具链 +
  `X:` 盘符映射 + Gradle 9.5.0）；无 `local.properties`，SDK 在
  `D:\dev-env\android-sdk`（经 NDK 构建产物定位）。该 .bat 的中文路径在本会话
  代码页下报"命令语法不正确"，改用等价内联命令（见交接文档第六节）。

### 轮次 2：并行评审 + 优化（本轮）

- 并行任务：两个只读评审代理同时执行——
  1. 代码评审：对照 14 条契约逐条核查 + 列鲁棒性改进与测试缺口；
  2. 文档研读：通读 `NISSAN_PORT_PLAN.md`、`NISSAN_TEANA_HEADUNIT.md`、
     `NISSAN_TEANA_BDD.md`、`NISSAN_TEANA_SDD.md`，产出交接文档对齐要点
     （阶段 1 勘测、术语、完成判据、BDD/SDD 缺口）。
- 评审结论：无契约违反、无正确性缺陷；提出 9 项改进、13 项测试缺口。
- 采纳（行为增强，全部先 Red 后 Green）：
  1. 标记行内联内容按分区首行处理（`wm size: Physical size: 800x1280`）；
  2. wm 行过滤收紧为按分区完整前缀（size：`Physical size:`/`Override size:`；
     density：`Physical density:`/`Override density:`），杜绝跨分区漏收与
     `Physicality:` 误收；
  3. 孤立 `\r` 按行分隔（`lineSequence()`，顺带惰性遍历，避免大文本物化 List）。
- 采纳（重构，无行为变化）：每行单次 `trim`；属性前缀（getprop/裸）预生成
  `Triple` 列表；`substring` 取值替代 `removePrefix`+`removeSuffix` 链。
- 补钉行为（新增测试，代码未改）：标记大小写敏感；同名分区跨块按序累计；dumpsys
  区内属性解析且重复键后值覆盖并原样保留在采集文本中；属性值保留 `=` 与 `]`；
  键前缀不越界；标记前出现的 Physical 行不采集；空白输入全 null 且透传。
- 不采纳（理由见交接文档第八节）：仅分区外解析属性；重复分区取首块；dumpsys 首行
  缩进一致化；无空格 getprop 宽容格式。
- TDD 证据：
  - Red：加 8 用例后定向运行，18 用例中恰 3 个失败（`Physical与Override按分区匹配前缀`、
    `孤立回车也按行分隔处理`、`标记行内联的分区内容按首行处理`），
    证据：`%TEMP%\nissan_opt_red.log`。
  - Green：实现优化后 18/18 通过（tests=18 failures=0 errors=0）。
  - 回归：`:shared:testDebugUnitTest` 全量 261 类 / 1743 用例 / 0 失败，
    证据：`%TEMP%\nissan_shared_full2.log` 与
    `shared/build/test-results/testDebugUnitTest/`（JUnit XML）。
- 交付文档：`docs/NISSAN_TEANA_PARSER_HANDOFF.md`（交接）、本文件（日志）。

### 轮次 3：SDD + BDD 补齐（按 BRANCHING.md 循环的下一片）

- `NISSAN_TEANA_SDD.md`：新增「文本解析设计（NissanDiagnosticsTextParser）」一节
  （职责边界 + 完整输入契约），非目标补 2 条，证据补解析器条目。
- `NISSAN_TEANA_BDD.md`：新增 4 场景（解析实车文本记录 / 缺失与噪声不产生字段 /
  解析不改变识别结论 / 分区标记的边界），验收证据补「场景 ↔ 测试映射」全表。
- 交叉验证发现映射缺口：wm 标记"嵌在行中"无测试（仅 dumpsys 有）→ 补测试
  `标记文本嵌在行中不构成标记`（钉现有行为）→ 定向 19/19 通过（pwsh7 运行）。

### 轮次 4：网络调研（两个并行代理）+ 交叉验证 + 新不变量

- 用户明确要求网络调研必要，随即执行两个并行调研代理（避免 3 并发触发限额）：
  1. Android 命令输出格式（AOSP 8.1→14 源码逐版本验证，LineageOS 镜像 + 社区转录）；
  2. 日产超智联社区数据（汽车之家/知乎/贴吧/B站/GitHub/XDA，证据分级）。
- 重大发现：
  - 解析器三条核心假设全部经 AOSP 源码验证成立；键字符集保证值内 `]: [` 不歧义；
  - `NISSAN_TEANA_HEADUNIT.md` §实车采集清单 的 getprop 多参数命令是坏的
    （getprop 最多 2 参数）——已在新调研文档中标记待所有者修正，未直接改他人
    未提交的文档；
  - NTDA3 世代社区实测为 Android 4.4.3/i.MX6/HSAE（DiPlay-Legacy-Android 真实
    日志），与文档"Android 9-10"推测矛盾；同代存在 Android 9 ARM64 第二平台；
    1280x720 获兄弟车型实测背书；NTDA3 无 Android 蓝牙适配器（BT 在 MCU 后）。
- 落盘：`docs/NISSAN_TEANA_RESEARCH_20261026.md`（调研纪要 + 调用方接线规则 +
  证据矩阵更新 + 行动项，全部附来源 URL）。
- TDD：新测试 `属性值包含分隔符序列不产生歧义` 首跑 Red——Red 捕获的是测试自身
  的输入笔误（值未闭合括号），恰好额外验证「畸形行不误解析」；修正后 20/20 通过。
- 回归：`:shared:testDebugUnitTest` 全量 261 类 / 1745 用例 / 0 失败。

### 轮次 5：并发满载——性能调研 + 全链路交叉验证审计 + 冒烟测试

- 按用户要求并发用到上限（实测 2）：两个后台代理并行派出——
  1. Kotlin/JVM 解析与测试实践调研（Kotlin 文档/JetBrains/Stack Overflow/Uber 等，
     全部附源）；
  2. SDD↔BDD↔测试↔实现↔交接文档全链路交叉验证审计（只读，含对构建产物 XML 的
     数字核对）。
- 性能调研结论（全部采纳）：保留 `lineSequence()`（CRLF/LF/CR 是文档化行为）、
  保留 `startsWith` 前缀链（比 Regex 快 5-10 倍）、不引入 Parameterized、不加未经验证
  的首字符门（避免冗余代码）、`joinToString` 与 `MutableMap` 维持现状；性能保障采用
  「大输入结构化断言为主 + 数量级时序上限」的无抖动模式。
- 审计发现（全部属实并修复）：
  - BDD 映射漏引 2 个测试 → 补引并把内联标记测试移回其 Then 子句所属场景；
  - 交接文档头部 18 用例与正文 20 自相矛盾 → 统一为 22；
  - 「值原样保留」与实现 `value.trim()` 不符且无测试 → 措辞改为「去首尾空白后保留」
    并补测试 `属性值去除首尾空白后保留`；
  - wm 保留行与标记内联内容的去空白规则未文档化 → KDoc/SDD/交接文档三处补齐；
  - 「任意分区内」未覆盖首个标记之前 → 三处改为「任意位置（含任何分区内与首个
    标记之前）」。
- TDD：新增 `属性值去除首尾空白后保留` 与 `大输入结构化冒烟测试`（约 1.4MB 合成
  转录：20k 噪声属性行 + 20k dumpsys 行，逐项断言 + 10 秒数量级上限）→ 定向 22/22
  通过（全测试类 5 秒内）；全量回归 261 类 / 1747 用例 / 0 失败。
- Windows 专项备忘（调研导出，已记入交接文档）：CJK 反引号测试名只用于 JVM 单测、
  不得进 androidTest（DEX bad descriptor）；控制台乱码是显示问题非编译问题
  （JEP 400）；时序断言必须数量级宽限。

### 轮次 6：调用方接线规则落 SDD/BDD + TDD（验证器指定切片）

- SDD：文本解析设计新增「调用方接线规则」条目——全量 getprop（多参数/单键不可用）、
  分区标签必需（裸 dumpsys 输出不被采集）、wm 失败信号丢弃 / dumpsys 失败信号保留
  为证据、调用方检测提示重采。
- BDD：新增场景「调用方转录规则」；映射表补 2 个新用例（并交叉引用标签必需性由
  `无显式标记时不得采集dumpsys` 钉住）。
- TDD 新用例：
  - `全量getprop转录只提取七个目标属性`——15 行真实形态转录（字典序、空值、无关
    属性），断言七属性提取 + 无关值（SABRESD/freescale/running）不流入报告；
  - `采集失败信号按噪声丢弃或保留为证据`——wm 分区 Security exception / Remote
    exception 行丢弃（wmSize/wmDensity null），dumpsys 采集区 Permission Denial
    行原样保留为证据。
- 验证：定向 24/24 通过；全量回归 261 类 / 1749 用例 / 0 失败。
- 修正一处调研文档措辞偏差（在 SDD 落地时澄清）：「解析器按噪声丢弃」只适用 wm
  分区；dumpsys 采集区的失败信号行是证据，原样保留，检测职责在调用方。

### 轮次 7：调用方实现切片——纯 JVM 转录组装与信号检测组件（TDD）

- 功能框架拆解：调用方 = Android 采集流程（后续切片）+ 纯 JVM「转录组装 + 失败信号
  检测」组件（本片实现，可独立测试）。
- TDD：先写 `NissanDiagnosticsTranscriptTest`（5 用例）→ Red
  （`compileDebugUnitTestKotlin FAILED`，7 处 `Unresolved reference
  'NissanDiagnosticsTranscript'`，证据 `%TEMP%\nissan_transcript_red.log`）→ 最小实现
  `NissanDiagnosticsTranscript`（assemble：getprop 段在前 + 三个分区标签段、空白段
  跳过；captureFailureSignal：六个行首信号前缀、返回首个信号行）→ Green 5/5。
- 关键用例：`组装结果可被解析器完整还原`（组装→解析往返，钉住调用方↔解析器契约）、
  `失败信号行组装后保留为证据`（与轮次 6 的解析器行为闭环）。
- 全量回归：262 类（+1）/ 1754 用例（+5）/ 0 失败。
- SDD：新增「转录组装设计（NissanDiagnosticsTranscript）」小节（组装规则、信号前缀、
  职责边界：组装只加标签，过滤与采集由解析器决定，不重复实现）。
- BDD：新增场景「组装带分区标签的转录」「检测采集失败信号」+ 验收证据映射
  （`NissanDiagnosticsTranscriptTest` 5 用例全量映射）。

### 轮次 8：采集编排组件（Android 流程的可测核心，TDD）

- 拆解：Android 侧采集流程 = 纯数据编排（本片）+ Android 薄壳（执行命令 + UI 分支，
  后续切片）。
- TDD：先写 `NissanDiagnosticsCaptureTest`（4 用例）→ Red
  （`compileDebugUnitTestKotlin FAILED`，`Unresolved reference 'NissanDiagnosticsCapture'`，
  证据 `%TEMP%\nissan_capture_red.log`）→ 最小实现 `NissanDiagnosticsCapture`
  （sealed `Outcome`：`CaptureFailed(signals)` / `Undetected` / `Reported(report)`；
  `from(...)` 编排 信号检测→组装→解析→报告，零新增文本规则）→ Green 4/4。
- 分流优先级（钉死为测试断言）：`detected=false` 一律 `Undetected`（优先于重采提示）；
  有失败信号 → `CaptureFailed`（按段序收集）；否则 `Reported`；全空白段 +
  detected=true → 全 unknown 报告（空采集不是失败）。
- 全量回归：263 类（+1）/ 1758 用例（+4）/ 0 失败。
- SDD：新增「采集编排设计（NissanDiagnosticsCapture）」小节；BDD：新增三场景
  （干净采集生成报告 / 失败信号要求重采 / 未识别一律拒绝）+ 验收证据映射。
- 至此调用方纯 JVM 链路完整：Transcript（组装+信号）→ Parser（解析）→ Capture
  （编排分流）→ Report（渲染），Android 薄壳只剩命令执行与 UI 分支。

### 轮次 9：命令执行抽象层（Android 薄壳的可测核心，TDD）

- 拆解：Android 采集薄壳 = 命令执行抽象层（本片，桩可测）+ 真实执行器与 UI 分支
  （依赖实车/模拟器环境，后续切片）。
- TDD：先写 `NissanDiagnosticsCommandCaptureTest`（4 桩用例）→ Red
  （`Unresolved reference 'NissanDiagnosticsCommandCapture'`，证据
  `%TEMP%\nissan_cmdcap_red.log`）→ 最小实现（`NissanDiagnosticsCommandExecutor`
  可注入 fun interface + `NissanDiagnosticsCommands` 四条命令常量 +
  `NissanDiagnosticsCommandCapture.run()`）→ Green 4/4。
- 关键钉子：`四条采集命令按固定顺序执行` 断言命令恰为
  `getprop / wm size / wm density / dumpsys display`（全量 getprop 形式）——
  把调研发现的「多参数 getprop 不可用」固化为可执行守卫；`执行器输出直接进入编排分流`
  覆盖执行器→编排全链路（干净→Reported、信号→CaptureFailed）。
- 全量回归：264 类（+1）/ 1762 用例（+4）/ 0 失败。
- SDD：新增「采集命令执行设计」小节；BDD：新增三场景 + 验收证据映射。
- 至此纯 JVM 链路六件套齐备：Commands/Executor（执行抽象）→ CommandCapture
  （采集）→ Transcript（组装+信号）→ Parser（解析）→ Capture（编排分流）→
  Report（渲染）；Android 侧只剩真实执行器与 UI 分支。

### 轮次 10：真实命令执行器（Android 薄壳可测部分，TDD）+ UI 分支规划落 SDD

- TDD：先写 `NissanDiagnosticsShellCommandExecutorTest`（4 桩进程用例）→ Red
  （`Unresolved reference`，证据 `%TEMP%\nissan_exec_red.log`）→ 实现
  `NissanDiagnosticsShellCommandExecutor` → 首次编译失败（fun interface 函数引用
  需显式 SAM 构造；`thread` 参数为 `isDaemon` 而非 `daemon`）→ 修正 → Green 4/4。
- 设计要点：白名单（仅四条命令常量，其余字符串不启动进程直接 null——注入守卫，
  测试含 `rm -rf`、拼接参数、`dumpsys display; reboot` 变体）；超时 10s 默认，
  超时销毁（destroy→destroyForcibly）且不读输出；stdout/stderr 后台线程并发读取
  合并（防管道死锁）；启动异常/超时/空白输出一律 null 不抛出。
- 全量回归：265 类（+1）/ 1766 用例（+4）/ 0 失败。
- SDD：新增「真实命令执行器设计」小节（真实 `sh` 行为标记待实车验证）+
  「Outcome UI 分支设计（规划）」小节（新会话首切片：三分支展示 + AGENTS.md
  硬约束：Diagnostics 类别、全 locale 字符串、大字号截图、先补 BDD+TDD）。
- BDD：新增三场景（白名单外不执行 / 超时销毁 / 输出采集）+ 4 用例映射。
- 至此 shared 侧纯 JVM 七件套齐备（Commands/Executor → CommandCapture →
  Transcript → Parser → Capture → Report + ShellCommandExecutor），Android 侧
  仅剩 UI 分支薄壳（规划已就绪，依赖 common 模块集成）。

### 轮次 11：Outcome UI 切片规格先行（BDD 规划场景 + 建议用例，交接强化）

- 判断：本会话已消耗约 1670 万 tokens、经历多次上下文压缩，`common` 模块 UI 切片
  （DiPlayActivity 集成 + 全 locale 字符串 + 大字号截图）在本会话强行开工有留下
  半成品红仓的最高风险；按交接质量优先原则，本会话只完成该切片的规格先行部分。
- BDD：新增「规划：Outcome UI 分支场景（待实现，验收证据暂缺）」——三个场景
  （采集失败提示重采 / 未识别车机拒绝说明 / 报告渲染）+ 建议 TDD 用例名（common
  模块 Robolectric、英文命名与该模块惯例一致），明确标注实现前不得视为已验证。
- 交接文档第七节第 5 项更新为即插即用规格入口（场景 + 用例名 + 命令链路 +
  AGENTS.md 硬约束）。
- 本轮无代码变更（上一轮全量 265 类 / 1766 用例 / 0 失败仍然有效）。

### 轮次 12：Outcome UI 分支实现（TDD）+ CLI 会话机制攻坚

- 集成配方（后台探查代理产出，零 UI 盲改）：最小爆炸半径 = 现有 DIAGNOSTICS 卡片内加
  Nissan 门控按钮（DiPlayActivity.kt 不动 Settings IA）、复制 showDiagnosticReport/
  permissionHelp 对话框模式、Robolectric 测试范式（ShadowBuild.setManufacturer +
  ReflectionHelpers settingsCategory/render + ShadowAlertDialog）。
- TDD：NissanDiagnosticsUiTest 三用例先写 → Red（5 处 Unresolved
  R.string.settings_nissan_diagnostics*，证据 `%TEMP%\nissan_ui_red.log`）→ 实现：
  DiPlayActivity 三处（5 个 network import、卡片门控按钮、runner 可注入字段 +
  showNissanDiagnostics 后台线程 + 三分支对话框 + 报告对话框）+ 5 个新字符串
  （settings_nissan_diagnostics / _recapture_title / _recapture_message(%1$s) /
  _refusal_message / _report_title）× 9 个 locale 目录（values/-ar/-es/-it/-ja/
  -ru/-uk/-zh-rCN/-zh-rTW，pwsh 脚本统一插入）→ 两轮修正（Sequence.toList、
  Settings 页面须以 page=settings Intent 打开）→ **Green 3/3**。
- 定向守卫：SettingsTranslationsTest / JapaneseTranslationsTest /
  TraditionalChineseTranslationsTest / SettingsLayoutPolicyTest 全过（9 locale
  字符串与占位符一致性验证）。
- 全量回归（轮次 12 收尾实测）：`:common:testDebugUnitTest` 1675 用例中仅 4 个已知
  Windows 环境失败（UsbPermissionSetupTest 缺 POSIX sh，基线同样失败，与本改动无关），
  其余全过（含 3 个新 NissanDiagnosticsUiTest 用例与全部翻译/布局套件）；
  `:shared:testDebugUnitTest` 265 类 / 1766 用例 / 0 失败（保持不变）。
- 侧边栏新会话开启（最终机制，用户确认路径）：CLI 余额受阻后改用 **computer-use 驱动
  桌面版 UI**——点击「切换侧边栏」→「新建任务」→ 向新会话输入框 typeText（Electron
  输入框不响应 AXSetValue，须真实键入）goal + 任务书 → 点「发送」。新会话实测已在
  **DiPlay · feat/nissan-teana-adaptation** 项目下运行（GLM-5.3 最高档、完全访问、
  任务：截图验证/独立验收材料等下一切片）。此为本项目 loop 的标准会话开启机制。
- CLI 会话机制攻坚（用户指示：安装 CLI、新会话进桌面侧边栏、GLM-5.3 余额不足
  路由 Flash）：全机遍历无独立 CLI → npm 安装 zcode-app-cli 3.16.3-39（Node 26.5）；
  探明用法（-p/--mode yolo/--surface desktop/--model provider/model/--cwd）；三次启动
  失败并逐一修复（--target 与 -p 互斥→提示词内嵌 /goal；模型 ID 须 provider/model
  格式）；**余额检测：CLI 共享登录（account:zai-individual-coding-plan）对 GLM-5.3
  与 GLM-5.3-Flash 均 1113 无余额**——按用户授权的余额判断，CLI 侧边栏会话被
  支付状态阻塞（需充值或以 Start Plan 账户 `zcode login`），loop 转由本会话
  （zai-start-plan/GLM-5.3，有余额）继续执行轮次 12 收尾。

### 轮次 13：布局截图验证（模拟器环境自装）+ 独立验收材料

- 用户指示「你需要环境配置自行安装」「你应该用简体中文做验证」：放弃截图记待办，
  自行安装模拟器全链路，以简体中文为主语言完成 AGENTS.md 布局截图验证。
- 环境安装（sdkmanager，一次 zip 损坏重试后成功）：emulator 包 +
  `system-images;android-35;default;x86_64` + `system-images;android-35;google_apis;x86_64`；
  创建 AVD `diplay_nissan_35`（default）与 `diplay_nissan_gapi`（google_apis，pixel_5）。
- Nissan 门控注入三连试：①`emulator -prop ro.product.manufacturer=…`——新版 emulator
  只接受 `qemu.*` 属性，WARNING 拒绝；②default 镜像 `/data/local.prop`——Android 15 init
  不再加载；③google_apis 镜像 `adb root → disable-verity → reboot → remount`（default
  镜像因 f2fs scratch 失败，google_apis overlayfs 成功）→ sed 改
  `/product/etc/build.prop` 与 `/system/build.prop` 的 manufacturer 行为 NISSAN
  （vendor 行优先级被 product 分区的 Google 压制，须改 product+system 两处）→
  **ro.product.manufacturer=NISSAN 生效**，`NissanHeadUnit.detected=true`。注入只在
  AVD 磁盘 overlay 内，运行时代码零改动，「传闻值不回填」红线不受影响。
- 截图（font_scale 1.5，存 `docs/screenshots/`，uiautomator bounds 几何验证 +
  screencap）：门控基线 `baseline_full_font150.png`/`baseline_compact_font150.png`
  （default 镜像 manufacturer=unknown，诊断卡**无**日产按钮——拒绝路径实证）；
  简中主验证 `nissan_full_zh_font150.png`（日产诊断按钮高 165px 与卡内全部按钮一致、
  x 对齐 [99,981]、行距 33px）+ `nissan_compact_zh_font150.png`（540x1140 窄窗完整
  可见、同高无裁剪）；RTL `nissan_rtl_ar_font150.png`（تشخيص نيسان 高 165px 一致、
  图标/文本右对齐镜像正常）。语言经 app 内 Language→简体中文 切换（阿语验证后回切，
  APPLY 按钮在 RTL 下镜像至左侧需注意坐标）。
- 全量回归复验（X: 映射 + pwsh7 内联）：**shared 265 类 / 1766 用例 / 0 失败**；
  **common 208 类 / 1675 用例 / 5 失败**（恰为已知 4×POSIX sh + 1×Mockito flaky，
  无新增回归）。本会话一次构建调用漏 `Set-Location X:\`（中文路径 AGP 检查失败），
  已修正并复跑。
- 红线扫描：NTDA3 / 1280x720 / P1xx 于 shared+common 运行时代码零命中（findstr）；
  5 个 Nissan 字符串 × 9 locale 目录逐目录核验全存在。
- 交付：`docs/NISSAN_TEANA_ACCEPTANCE.md`（独立验收材料：交付物清单、复验命令与
  预期结果、测试证据、红线核查表、截图几何证据、已知限制）。
- 注：MCP android-emulator 插件进程缺 `ANDROID_HOME`/adb 环境不可用，全程 raw
  adb/emulator（工具不覆盖场景）；截图 PNG 在本环境仅 CDN 化无法内嵌目视，几何
  验证以 uiautomator bounds 数据为准。

### 轮次 14：门控钉子用例 + 模拟器端到端实证 + 上游 #1084 hotfix 交叉验证（用户追加目标：续推 20 次迭代）

- 切片 A（TDD 钉子）：`NissanDiagnosticsUiTest` 补第 4 用例
  `nissanButtonHiddenWhenHeadUnitNotDetected`（ShadowBuild manufacturer=Google →
  Diagnostics 页无日产按钮）——钉住门控行为（此前仅截图实证）→ 定向 4/4 Green。
- 切片 B（模拟器端到端实证真实执行器）：google_apis AVD（NISSAN manufacturer，
  须带 `-writable-system` 启动否则 overlay 不挂载、manufacturer 回落 Google——
  本次实测发现并记录）真实点击「日产诊断」→ 完整真实链路：四条命令经真实 `sh`
  执行；`getprop` 全量输出正常采集（app uid 无限制，`[ro.product.manufacturer]:
  [NISSAN]` 在列）；`wm size/density` 报 `Can't find service: window`（非信号
  前缀→按接线规则静默丢弃）；`dumpsys display` 报 `Permission Denial`（信号→
  保留为证据）→ `CaptureFailed` → 重采提示对话框展示真实信号行（截图
  `docs/screenshots/nissan_e2e_capture_failed_real.png`）。**重要修正**：SDD 原
  「getprop/wm/dumpsys 对 shell 可读」假设在 **app uid** 不成立（shell≠app uid），
  已更新 SDD；实车行为取决于车机对 DiPlay 的权限配置。run-as 在该模拟器报
  unknown package（工具限制），app uid getprop 形态由早期 run-as 成功输出 +
  端到端行为双重佐证。
- 切片 B2（用户指示：上游 issue 调研 + hotfix 交叉验证）：上游
  `shihabal3amri/DiPlay` 30 个 open issues 调研（gh CLI）→ 选定 **#1084**
  （开机自启卡在已连接界面，DiLink 4.0/热点/iOS，6 条评论多人复现，真实 bug；
  上游修复 **#1085**「开机自启发现后台已有会话则直接回到 CarPlay」已于
  2026-10-24 合入 upstream/main）。交叉验证（用户澄清：hotfix 基于本地 main，
  本地是日产专用演进线）：
  1. 本地 main（9d896330）= upstream main 当前 HEAD，**已含 #1085**（tarball
     解包核验 BootReceiver 修复在位；PR patch 应用失败即重复应用反证）。
  2. 日产分支对 BootReceiver/LauncherIntentTest **零差异**（git diff 空）——
     两条线正交，hotfix 基线不受日产工作影响。
  3. worktree `%TEMP%\DiPlay-hotfix1084`（分支 `hotfix-1084-boot-return-carplay`
     基于本地 main；首次建在中文路径触发 AGP 检查后迁至 ASCII 路径；本地已有
     `hotfix` 分支导致分层分支名冲突，改名规避）：`LauncherIntentTest` 14 用例
     （7 用例 × SDK 28/33，覆盖 launcher intent 判定/有会话回 host 不停会话/
     settings 收 launcher 清返回栈/重建保留 settings/无会话留首页+autoConnect
     决策）**全 Green**。
  4. hotfix 基线 common 全量回归：207 类 / 1672 用例 / 4 失败——**恰好全部**为
     已知 POSIX sh 环境失败（UsbPermissionSetupTest×4，flaky Mockito 这次未犯），
     无新增回归；与日产分支基线（208 类/1675 用例，差值=日产 UI 测试）自洽。
  5. 模拟器 boot 冒烟（Android 15 真实系统广播）：BOOT_COMPLETED 为受保护广播
     （shell uid 拒绝、adb root 后 su 0 可发）；force-stop 后广播 → 新进程启动 →
     DiPlayActivity 前台（该 AVD 自启开启态，正路径实证）；负路径与决策分支由
     LauncherIntentTest 覆盖（模拟器无 iPhone 会话，「有会话回 CarPlay」分支
     无法端到端复现，已在 BDD 注明）。git fetch 对 github.com:443 不通，
     tarball 走 gh api 通道成功（网络差异记录在案）。
- 切片 C（SDD/BDD 同步）：BDD「规划：Outcome UI 分支场景」→ 已实现状态 + 第 4
  场景（门控不显示）+ 4 用例映射 + 模拟器实证小节；SDD 真实执行器小节补模拟器
  实证与 app uid 权限修正，Outcome UI 小节从规划转已实现。
- 切片 D（手动基准，零依赖）：JUnit XML 计时提取——
  `大输入结构化冒烟测试`（1.4MB 合成转录：20k 噪声属性行 + 20k dumpsys 行）
  **0.041s**（≈34MB/s 量级，含逐项断言开销），其余 23 用例合计 0.002s。结论：
  吞吐量级远超任何实车转录（全量 getprop 约 60–200 行），维持轮次 5 决策
  （不引入 JMH 依赖），性能证据以结构化断言 + 本计时为准。

### 轮次 15：完整 CI 合规验证 + shared lint 揪出 NewApi 缺陷（TDD 修复）+ hotfix 覆盖性确认

- 切片 E（AGENTS.md 完整 CI）：轮次 12 加 Activity 代码后首次重跑完整命令
  `:mobile|home|maphost:lintDebug + assembleDebug + :home:testDebugUnitTest` →
  **BUILD SUCCESSFUL**（41s）。另追加 `:shared:lintDebug`（CI 命令不含，系本
  loop 新增代码的质量检查）。
- 切片 G（lint 揪出日产交付缺陷 → 修复）：`:shared:lintDebug` 报
  `NissanDiagnosticsShellCommandExecutor.kt` 3 个 **NewApi 错误**——
  `Process.waitFor(long,TimeUnit)` / `isAlive` / `destroyForcibly` 均为 API 26+
  而项目 minSdk=24（API 24/25 设备将 NoSuchMethodError 崩溃；目标车若为
  Android 8+ 不触发，但 minSdk 承诺必须兑现）。**Red 证据 = lint BUILD FAILED
  （3 errors，首跑日志）**；修复：超时等待改 `exitValue()` 轮询（10ms，中断即
  返回当前状态），`destroyForcibly` 加 `SDK_INT >= O` 守卫（旧平台 `destroy()`
  即强杀语义）；中途一次编译修正（companion private 常量对文件级类不可见 →
  移至文件级 const）。**Green**：`NissanDiagnosticsShellCommandExecutorTest`
  4/4（行为等价，超时销毁桩用例仍过）；lint 强制重跑（清中间产物）后
  **日产文件零命中**，模块总错误 36 → 33（恰减 3；剩余 33 为既有代码存量债务，
  非本 loop 范围，不修以控爆炸半径）。SDD 已补 API 24 兼容条目。
- 切片 F（hotfix 覆盖性确认）：#1095 已被上游作者关闭为 #1084 重复；#986 经
  贡献者日志分析确认同一启动路径（仍开仅因附带壁纸功能请求）——**三 issue
  同根因，#1085 统一覆盖，无需额外 hotfix**。#1119（0.2.21 无线热点连接回归，
  0.2.20 可连）拉取分析：协议栈至 carplay-start-session 正常、iPhone 不建 TCP
  （tcpAccepted=0）——复现需 iPhone 真机，本地仅可做版本 diff 分析，维持候选
  记录于 handoff。
- 环境备注：lint 首跑输出被任务过滤截断 + lint 任务 UP-TO-DATE 复用旧报告
  （行号不变是识别线索）——清 `lint_intermediate_text_report` 后重跑方得新结论。

### 轮次 16：#1119 回归 diff 考古（排除法结论）+ 日产诊断用户指南

- 切片 H（#1119 连接回归版本 diff 分析，阶段性结论落
  `NISSAN_TEANA_RESEARCH_20261026.md`）：v0.2.20...v0.2.21 = 118 commits /
  172 files。**已排除**：`CarPlayVpnService`（无线 TCP 的 VPN 转发层）零改动；
  `WirelessStartupPolicy` 仅失败枚举扩展（Android 13+ 场景，报告者 Android 10
  不涉及）；`AirPlayConfig` 仅显示角遮罩；`CarPlayController` 无 TCP/mDNS 变更；
  权限类变更与症状（协议栈已过权限阶段）不匹配。**剩余嫌疑**（未定论）：
  0.2.21 新增的 Wi-Fi 恢复路径（wifiRecoveryButton / WirelessStartupRecoveryTest
  +79 行 / CarPlayHostActivity 大改中该区域）。定位方式：症状反查（tcpAccepted
  计数器 → CarPlayVpnService/FirstTcpWatchdog 区域）+ compare API 逐文件核对。
  定论需 iPhone 环境复现或上游作者介入；本仓库 main 同样携带嫌疑行为。
- 切片 I（用户指南）：新增 `docs/NISSAN_DIAGNOSTICS_USER_GUIDE.md`——入口与
  门控可见性、四条只读命令、三分支对话框含义与车主应对、ADB 重采要点
  （app uid 权限现实 vs shell 权限）、隐私安全约定、已知限制。面向验收方与
  车主，引用 HEADUNIT.md 采集清单而不复制（禁触文件零引用正文改动）。
- 环境备注：gh api compare 的 patch 过滤 + 症状反查（grep tcpAccepted）比通读
  920 行 patch 高效；here-string 经 cmd 传输会撕裂，复杂追加走临时 .ps1。

### 轮次 17：已知 flaky 用例根因调查（只读，验收方问询预备）

- 对象：`common` `CarPlayHostClusterStartupTest.aReplacementSessionDetachesTheOld
  ClusterSurfaceUntilItsOwnMainFrame`（已知 5 个本机失败之一，Mockito 偶发）。
- 失败形态（多次全量跑观察）：稳定为 `TooFewActualInvocations`——
  `verify(sink, times(2)).setSurface(111, surface)` 只观察到 1 次。
- 路径分析（只读）：第二次 `setSurface(SCREEN_TYPE_ALT)` 走
  `CarPlayHostActivity.kt:1544` 的 `else if (clusterOutputReady())` 条件分支，
  由 `onVideoFrameRendered(replacement)` + 主线程 idle 触发；测试用
  `shadowOf(Looper.getMainLooper()).idle()`（PAUSED 模式）——**idle() 只执行
  当前队列任务**，若实现路径存在任何条件性 post 或 `clusterOutputReady()`
  状态在两次 idle 间不稳定（该用例未调用 display()，对比 stale 用例有调用），
  times(2) 偶发不满足。已排除无关嫌疑：50ms postDelayed 为 pictureHoldTick
  （图片保持动画，与 cluster surface 无关）。
- 处置：**不修改**（既有上游测试/实现，非本 loop 资产；改动扩大爆炸半径）。
  结论供验收方参考；若上游后续修复则同步。基线重跑通过的历史记录
  （worklog 轮次 1 对照实验）维持有效。

### 轮次 18：#1108/i.MX 帧率风险预警 + #1116 台账（日产阶段 2 预研）

- #1108（i.MX8/Android 8.1 有线帧率坍塌）详情与上游计数器分析拉取：码率恒
  ≤835kbps、坍塌时单帧变大——iPhone 侧码率协商/带宽瓶颈模式，非解码器能力
  主因。结合既有调研（NTDA3 = i.MX6 + Android 4.4.3 双平台结论）写入
  `NISSAN_TEANA_RESEARCH_20261026.md`：**日产阶段 2（视频通路）的首要实测项
  = 码率协商与带宽**；对策线索 = DiPlay 既有 Advanced 设置（HEVC/帧率/
  分辨率）+ 勘测时补记 `dumpsys media.codec` 摘要（仅勘测证据，不进运行时）。
  阶段 1 诊断链路不受影响。
- #1116（热点绿屏）台账简记：副厂机 SDK 27、握手/音频/iPhone 截图正常、
  车机渲染全绿——典型解码初始化/像素格式问题；上游无分析线索，关联弱，
  仅跟踪。

### 轮次 19：#1119 深挖——嫌疑升级为上游 open PR #1071（对症修复）+ 本地交叉验证

- 深挖方法：精读 CarPlayHostActivity 920 行 patch 的 recovery/重连区域 +
  上游 open PR 扫描（15 个 open PR 过目）。
- **首要嫌疑（上游已确认的缺陷）**：open PR **#1071**（faizannazir，10-21，
  未合并）"Fix wireless CarPlay StartSession IP address payload"——iAP2 0x4301
  StartSession 载荷只含部分接口地址；修复 = 全部热点接口主机地址（IPv4 +
  link-local IPv6）。与 #1119 症状链精确闭环（MFi 过→StartSession 载荷 IP 不全
  →iPhone 无地址可连→tcpAccepted=0→first TCP timeout→重试同理失败→放弃）。
  **PR 存在即上游对该缺陷的确认**。次要候选降级：0.2.21 新增重连跳过条件
  （`controlRunningCount` 不分有线/无线，注释引 #873 有线场景——影响重试行为，
  可能叠加症状）。结论已更新至 RESEARCH 文档 #1119 节（轮次 16 的「Wi-Fi
  恢复路径嫌疑」据此修正）。
- **本地交叉验证（#1084 模式）**：#1071 三文件 patch（CarPlayController.kt +
  Iap2WirelessControlClientTest + WirelessStartupControllerTest）在 hotfix
  worktree（本地 main 基线）**干净应用零冲突**；PR 自带测试全绿——
  `Iap2WirelessControlClientTest`（shared）12/0、`WirelessStartupControllerTest`
  （common）8/0。修复工作副本保留在 worktree 未提交状态；待上游合并后同步，
  日产线无需独立修复（同一 main 基线携带同一缺陷）。
- 其他 open PR 扫描台账：#1113（同局域网手动指定网卡）、#968（无线性能保护）
  与日产场景弱相关，跟踪即可。

### 轮次 20：#1071 全量回归补全（交叉验证完整性对齐 #1084 标准）

- worktree（#1071 已应用）双模块全量：**shared 258 类 / 1714 用例 / 0 失败**
  （含修复与 PR 新增用例）；**common 207 类 / 1673 用例 / 4 失败**——恰为已知
  POSIX sh 环境失败（UsbPermissionSetupTest×4），**零新增回归**。#1071 在本地
  main 基线的验证就此完整（干净应用 + PR 测试 12/0 + 8/0 + 全量无回归），
  达到 #1084 同级交叉验证标准；修复副本保留 worktree 未提交，待上游合并后
  按同模式同步。

### 轮次 21：#1071 技术细节深读 + 上游 release 节奏核查

- #1071 diff 精读（RESEARCH 文档补节）：修复前 `ipAddresses =
  listOf(hostAddressText)` 只携带**单个**地址——双栈接口（IPv4 + link-local
  IPv6）上选中 iPhone 不可达的地址即复现 tcpAccepted=0；修复后主地址 +
  `hotspotInfo.hostAddresses` 全量去重，两个新测试钉住双栈编码（iAP2 参数
  id=3 ×2）与 IPv6 去 `%scope` 文本。日产阶段 2 预警：天籁超智联热点同理会
  遇到双栈选择问题，上游合并后同基线自动受益；勘测可补 `ip addr show`。
- release 节奏核查：最新 v0.2.21（10-17 pre-release），0.2.22 未发布——#1085
  （已合并）与 #1071（open）均未进 release；上游 2–3 天一个 pre-release，
  0.2.22 临近。下次 loop 检查点：0.2.22 发布时核对包含关系。

### 轮次 22：上游合并冲突预检 + #1090 台账

- **合并冲突预检**（`git diff --name-only main feat/nissan-teana-adaptation`）：
  日产分支相对 main 仅改 5 文件（BRANCHING.md、NISSAN_PORT_PLAN.md、
  NISSAN_TEANA_HEADUNIT.md、NissanHeadUnit.kt 及其测试）——与 #1085 两文件
  （BootReceiver.kt、LauncherIntentTest.kt）、#1071 三文件（CarPlayController.kt、
  Iap2WirelessControlClientTest、WirelessStartupControllerTest）**零交集** →
  日产分支合并上游 main（含两个修复）**无冲突**；工作区未跟踪的诊断链路新文件
  不受合并影响。整合决策材料就绪。
- #1090（DiLink 7.0 更新后 HUD 导航信息消失）台账：Boring206 分析确认 iPhone
  有发 iap2 0x5201 路口信息到 DiPlay（信息到达、下游显示断链）——BYD HUD
  专属输出协议，与日产场景无关，仅跟踪。

### 轮次 23：日产车机蓝牙问题三路调研 + 生产流程落位（用户指令切片）

- 用户指令：「diplay 的蓝牙协议日产车机连接似乎存在一定问题……查看源项目仓库
  不同版本对应更新和 readme、访问技术论坛、最后根据生产流程进入 loop」。
- 三路调研（RESEARCH 新增大节）：①上游 issue 族——#703 日产轩逸官方定性
  （后装车机蓝牙独立模块、Android 系统蓝牙不可用、无线必败）、#880 HSAE
  CN202SR/i.MX6 完整工程 outline（Anw 出厂栈桥接实车验证通过 + 上游作者
  准入条件）、#581/#764/#918 同族；②社区 fork——DiPlay-Legacy-Android
  （0.2.96+，scope 含东风日产 HSAE 私有蓝牙栈）#79 二十九评论完整还原
  19 款逍客（NTDA3）无线失败史与解法；**winkinwong/DiPlay-Legacy-Android-T32
  （v0.2.46-t32.3）AnwPhoneLink 桥接实机验证无线+有线可用**；③技术论坛
  （WebSearch）+ 本仓库轮次 4 既有调研（BT 在 MCU 后）交叉印证。
- **根因定案**：NTDA3 家族 Android 系统蓝牙不可用（蓝牙经 MCU + 厂商服务），
  上游无线 CarPlay 依赖原生 BT → 必败。社区已有实机验证的 Anw 桥接方案，
  上游欢迎隔离 PR——天籁大概率适用（同家族，待实车确认）。
- 生产流程落位：SDD 新增「AnwPhoneLink 蓝牙桥接设计（规划）」（检测门三分支/
  字节流等价适配/ABI 防御/清理回落）；BDD 新增 4 规划场景 + 建议用例名；
  loop 排队首位 = 轮次 24 T32 桥接实现代码级分析（gh api 通道）。
- 环境备注：github.com 443 仍不通（WebFetch/git fetch 均超时），gh api 与
  gh search 通道稳定；gh search issues 的 OR 语法不支持（需拆分查询）。

### 轮次 24：T32 Anw 桥接代码级对勘 → 移植规格草案（生产流程：规格先行完成）

- T32 仓库结构定位（git trees 递归 1233 文件）：四层结构——AIDL 定义
  （com.anwsdk.service ×3 接口 + 第二厂商 com.hsae.bluetoothsdk）、适配层
  （AnwPhoneLinkBluetoothAdapter 实现 **CarPlayBluetooth 上游接口** /
  HsaeRfcommStream 实现 **BlockingDuplexByteStream** / Discovery / Hsae 侧）、
  接入点（CarPlayBluetoothManager 混合门 + selectVendorWirelessTarget，
  CarPlayController 四行改动）、TDD 测试双文件。
- 核心源码精读：Adapter（服务 action 解析检测、诚实边界、bluetooth_address
  MAC 读取与校验）+ HsaeRfcommStream（4KB 分块写、有界 FIFO 部分读保序、
  RemoteException→IOException 故障传播、close 反注册清理）+ 控制器接入点 grep。
- **对勘结论**（两独立实现互证）：字节流边界稳定同构；T32 的 AIDL 命名方法
  优于 #880 裸事务号（版本容错 + Socket/SPP 双模式）；#880 独有的连接异步
  确认与配对列表解析顺序防御应并入移植。
- 交付：RESEARCH 新增「T32 Anw 桥接实现分析与移植规格草案」节（五点规格）；
  SDD AnwPhoneLink 节补对勘补强。**至此阶段 2.5（Anw 蓝牙桥接）的 SDD/BDD
  规格全部就绪，实现按 TDD 开工条件成熟**（纯 JVM 桩 AIDL，四场景用例名已定）。

### 轮次 25：AnwPhoneLink 蓝牙桥接 TDD 实现（阶段 2.5 核心，Red→Green）

- 前提核查：本地 main 有 `BlockingDuplexByteStream`（上游接口，recv 契约
  null=超时/空数组=对端结束）；无 `CarPlayBluetooth` 抽象（T32/新版上游才有）
  → 随移植定义。
- **Red**：先写 `AnwBluetoothBridgeTest`（6 用例，纯 JVM 桩通道）→
  `:shared:compileDebugUnitTestKotlin FAILED`，8 处 `Unresolved reference`
  （AnwPhoneLinkBluetoothAdapter / CarPlayBluetoothManager / HsaeRfcommStream /
  CarPlayBluetoothDevice / selectVendorWirelessTarget 等）。
- **实现**（shared 新包 `com.shilapi.xcertplay.bluetooth`，4 文件）：
  `CarPlayBluetooth.kt`（接口 + 设备数据类，无 Android 类型保持纯 JVM）；
  `AnwVendorChannel.kt`（厂商通道抽象隔离 AIDL binder + `uuidToAnwGuidBytes`
  ——iAP2 UUID 的 GUID 字节序反转，#880 对勘结论固化）；`HsaeRfcommStream.kt`
  （BlockingDuplexByteStream 适配：有界 FIFO 部分读保序、4KB 分块写、负返回→
  IOException 故障传播、close 反注册回调+断开占用 socket、幂等）；
  `AnwPhoneLinkBluetoothAdapter.kt`（CarPlayBluetooth 替身：resolver 检测门、
  诚实边界 isEnabled=isSupported、MAC 校验拒绝匿名地址 + `CarPlayBluetooth
  Manager.pick` 混合门原生优先 + `selectVendorWirelessTarget` 手动地址→已连接
  →唯一 iPhone→多 iPhone 抛错，皆无时「use wired USB」fail-fast）。
- 中途修正×3（测试侧）：函数调用括号、可空接收者 `!!`、List<Byte> 误用扩展；
  一次语义对齐——recv 单缓冲消费（系统调用 read 语义，与 T32 一致、上游契约
  "may split or coalesce arbitrarily" 允许）取代初版「强制聚合填满」期望。
- **Green**：定向 6/6 通过（`BUILD SUCCESSFUL`）。全量回归 **shared 266 类 /
  1772 用例 / 0 失败**（+1 类 +6 用例恰为本测试）；lint 新包**零命中**，
  模块存量错误维持 33。
- BDD：四场景标题转「已实现」+ 6 用例映射落盘；实车 AIDL 绑定层（binder/
  服务解析装配）按 SDD 留待实车，纯 JVM 边界内全部验证。



























### 操作计数（开发/测试/调研操作，每次文件变更或测试执行计 1）

| 轮次 | 操作 | 累计 |
| --- | --- | --- |
| 1 初版 TDD | 写用例、Red、实现、Green、全量、CI、对照实验 | 7 |
| 2 并行评审+优化 | 加用例、Red、优化实现、Green、全量（另 2 评审代理） | 12 |
| 3 SDD/BDD | SDD 扩展、BDD 场景、BDD 证据、新用例、定向运行 | 17 |
| 4 网络调研 | 调研代理×2、调研文档、新用例（含 Red 捕获+修正）、全量 | 23 |
| 5 性能+审计 | 性能调研代理、审计代理、2 新用例、定向运行、四文档修正、全量 | 29 |
| 6 调用方规则 | 2 新用例、定向运行、SDD 接线规则、BDD 场景+映射、全量 | 35 |
| 7 转录组件 | 5 用例、Red、实现、Green、全量、SDD 小节、BDD 场景+映射 | 42 |
| 8 采集编排 | 4 用例、Red、实现、Green、全量、SDD 小节、BDD 三场景+映射 | 49 |
| 9 命令抽象层 | 4 用例、Red、实现、Green、全量、SDD 小节、BDD 三场景+映射 | 56 |
| 10 真实执行器 | 4 用例、Red、实现（含编译修正）、Green、全量、SDD 两节、BDD 三场景+映射 | 63 |
| 11 UI 规格先行 | BDD 规划场景三则 + 建议用例、交接文档强化 | 65 |
| 12 Outcome UI 实现 | 探查代理、三用例、Red、Activity 三处实现、9 locale 字符串、Green（含两轮修正）、i18n 定向、CLI 安装与三次启动修复、余额检测、全量回归 | 75 |
| 13 截图验证+验收材料 | 环境安装×3（emulator/两镜像，含损坏重试）、AVD×2、门控注入三连试、APK 构建+安装×2、基线截图×2、简中/RTL 截图×3（含语言切换自动化与几何 dump×5）、全量回归+XML 提取、红线扫描、字符串核查、验收文档、本日志 | 92 |
| 13b 交接同步 | handoff 第七节第 4 项勘误（性能调研已完成于轮次 5）、第九节 goal 文本更新至轮次 13 后状态 | 93 |
| 14 门控用例+端到端+hotfix | 门控钉子用例+定向运行、模拟器重启修正（-writable-system）、端到端点击+dump+截图、run-as 对照×3、issues 调研×3（列表/#1084/#1085）、tarball 拉取解包、worktree 建立（含路径与分支名两次修正）、patch 失败分析+tarball 核验、分支零差异核验、LauncherIntentTest、hotfix 全量回归+XML 提取、boot 冒烟×2、SDD/BDD 更新×2、手动基准提取、handoff/验收文档更新 | 119 |
| 15 CI+lint 缺陷修复+覆盖性 | CI 首跑（发现 shared lint NewApi×3）、修复实现（含编译修正）、定向 4/4、lint 强制重跑核验（Nissan 零命中 36→33）、AGENTS CI 复跑全绿、#1095/#986/#1119 调研×3、handoff/SDD 更新、本日志 | 130 |
| 16 #1119 考古+用户指南 | tag/compare 拉取、连接类提交过滤、VPN/策略/配置逐项排除、症状反查 tcpAccepted、嫌疑聚焦、调研文档追加、用户指南撰写、本日志 | 137 |
| 17 flaky 根因调查 | 用例与失败形态核对、setSurface 调用点与条件分支分析、postDelayed 嫌疑排除、worklog/验收文档落笔 | 141 |
| 18 i.MX 风险预研 | #1108 详情+计数器分析拉取、i.MX6 关联推断、阶段 2 风险与对策落 RESEARCH、#1116 台账、本日志 | 144 |
| 19 #1119 深挖 | host patch 重连区域精读、open PR 扫描、#1071 详情与症状闭环分析、patch 干净应用、PR 测试×2 全绿（12/0+8/0）、RESEARCH 嫌疑升级、本日志 | 151 |
| 20 #1071 全量回归 | worktree 双模块全量（258/1714/0 + 207/1673/4 已知）、失败清单核对零新增、本日志 | 154 |
| 21 #1071 深读+release 核查 | diff 精读（单地址→全量地址形态）、RESEARCH 补节与日产预警、release list 核查（0.2.22 未发）、本日志 | 157 |
| 22 冲突预检+台账 | 分支文件清单 diff、与 #1085/#1071 零交集结论、#1090 台账、本日志 | 160 |
| 23 日产蓝牙三路调研 | git/README 搜索×2、gh issue 双语搜索×2、#703/#880 详情、#880 完整 outline×3、WebSearch×2、Legacy README×3、COMPATIBILITY、#79 二十九评论、RESEARCH 大节、SDD/BDD 规划节、本日志 | 175 |
| 24 T32 对勘+规格草案 | 仓库定位×2、trees 递归、Adapter/RfcommStream/Discovery/接入点源码精读×4、双实现对勘、RESEARCH 规格节、SDD 补强、本日志 | 183 |
| 25 Anw 桥接 TDD 实现 | 基座核查、6 用例 Red（8 处 Unresolved）、4 文件实现、测试修正×3、语义对齐×1、Green 6/6、全量回归（266/1772/0）、lint 零命中、BDD 转已实现+映射、本日志 | 193 |
| 26 装配层+AIDL 移植 | 5 用例 Red（Unresolved）、字节序钉子修正、Wiring 实现、Green 5/5、AIDL 三文件拉取（含换行压扁修复）与编译产物核对、事务号双源互证、控制器接入评估（延至轮次 27 抽象化）、全量回归（267/1777/0）、lint 零命中、BDD 映射、本日志 | 203 |
| 27a 抽象化规格细化 | 四处直用点实读（192/1300/1388/2574）、RFCOMM 消费点定位（1478+既有流适配）、窄改三点设计、TDD 守卫策略、封盘交接、本日志 | 208 |
| 27b 混合门接入控制器 | Planner 5 用例 Red→Green、AndroidAnwServiceProbe 薄层、控制器三处窄改（预检放行/决策两型/流建立两型）、编译修正×3、全量双模块回归（shared 268/1782/0，common 1676/4 已知）、lint 零命中、BDD 映射、本日志 | 217 |
| 28 检查点+验收补全 | 上游三态核查（#1071 OPEN/0.2.22 未发/HEAD 不变）、验收文档 Anw 交付物三处补全、本日志 | 220 |
| 29 低负载切片 | 缺口审查×3、Lifecycle Red（双失败精确命中）→修复→Green 2/2、监听器钉子（Bridge 7/7）、SDD 矩阵+预算节、BDD 三场景、goal 文本加任务目标、全量回归（shared 1783/0，common 1678/5 含新 flaky 归档）、本日志 | 230 |
| 30 拆解样本模拟 | 拆解调研（1280x720/i.MX6/NTDA3 勘误）、样本 3 用例 Red→修正→Green 3/3、模拟器真机规格（1280x720+d160）几何验证+截图、RESEARCH 样本节、本日志 | 238 |
| 31 交付+口径同步 | 交付文档+记忆指针（主会话不可直达改双通道）、验收文档三命令+限制第7条、goal 文本至轮次30、接管终检（无设备/矩阵全覆盖）、本日志 | 243 |
| 32 细致打磨 | 禁用反馈用例 Red→实现（按钮引用+禁用恢复）→Green 3/3+4/4、本日志 | 246 |
| 33 PRD+矩阵+并行派发 | PRD 入账、双 SDK 矩阵全绿（8+6）、双代理并行派发（P0-1 开发/P0-2+P1-4 文档）、本日志 | 252 |
| 34 并行三线合并 | A 复验（12/12+直查）、粘贴截图×2、B 三文件核验、C 对勘入 RESEARCH、独立全量（269/1786/0 + 1698/4 已知）、验收同步、本日志 | 262 |

**累计 262 次（loop 持续中，未喊停即续推）。** AnwPhoneLink 蓝牙桥接至此
**端到端接线完成**（纯 JVM 核心 + 装配层 + AIDL + 控制器混合门），剩实车
验证与 NTDA3 之外的收尾。Loop 排队切片：真实执行器与 Anw 桥接实车验证、
HEADUNIT.md 采集清单修正（待所有者）、独立验收（材料已就绪）。
（历史排队注记：轮次 27a 曾封盘交接，27b 已按规格实现完毕。）

### 交接终态（2026-10-26 轮次 28 会话封盘，本会话 = 轮次 13–28）

- 代码（全部未跟踪、未提交，零 staged/commit，HEAD 仍 f65d5e86）：
  ①诊断链路九件——`NissanDiagnosticsTextParser`（24 用例）、`Transcript`（5）、
  `Capture`（4）、`CommandCapture`（4）、`ShellCommandExecutor`（4，API 24
  兼容已修）、既有 `Report`（6）、`NissanDiagnosticsUiTest`（4，common）；
  ②**AnwPhoneLink 蓝牙桥接全链**——`shared/.../bluetooth/` 新包七文件
  （契约/通道抽象+UUID 字节序/流适配/替身+混合门/装配层/生产探针/规划器）+
  `com/anwsdk/service` 三 AIDL 文件（T32 fork 移植，事务号与 #880 互证）+
  `CarPlayController.kt` 三处窄改（混合门接入，原生分支零变化）；测试
  `AnwBluetoothBridgeTest`（6）+ `AnwPhoneLinkWiringTest`（5）+
  `WirelessBluetoothPlannerTest`（5）。
  全模块实测：shared **268 类 / 1782 用例 / 0 失败**；common 1676 用例仅
  4 个已知 POSIX sh 环境失败；lint 新增代码零命中（模块存量 33 属上游债务）。
- 文档（七文档体系）：SDD（含 Anw 桥接设计+对勘）、BDD（全部场景带映射）、
  RESEARCH（蓝牙三节+T32 对勘+移植规格+#1119/#1108 分析）、验收文档
  （含桥接 16 用例复验命令）、用户指南、handoff（第六节环境含模拟器要点、
  第九节 goal 文本已至轮次 28）、本日志（28 节 + 计数 220）。
- 截图：docs/screenshots/（简中 full/compact + 阿语 RTL + 门控基线 ×2 +
  端到端 CaptureFailed 实证）。
- hotfix worktree：`%TEMP%\DiPlay-hotfix1084`（分支 hotfix-1084-boot-return-
  carplay @ main，含 #1071 patch 已验证未提交副本）。
- 新会话启动：粘贴 handoff 第九节 goal 文本；最高价值项 = 实车 ADB 转录
  （解锁三项）；上游检查点（#1071 OPEN/0.2.22 未发/HEAD 9d896330）下次复查。

### 当前仓库状态（轮次 28 结束时）

- 本会话新增未跟踪文件：shared `network/NissanDiagnosticsUiTest 之外无`——
  详见上节清单；common `NissanDiagnosticsUiTest.kt`；docs 六份 + screenshots
  五张 + e2e 一张。
- 本会话修改（工作区 M）：`common/DiPlayActivity.kt`（+64）、`strings.xml`
  ×9 locale、`shared/CarPlayController.kt`（混合门三处窄改）。
- 会话前已存在的他人修改六文件全部原样保留、零触碰。
- 未 add、未 commit、未 push。等待独立验收 + 实车输入。
### 轮次 26：Anw 装配层 + AIDL 定义移植（Red→Green）

- **Red**：先写 `AnwPhoneLinkWiringTest`（5 用例：探针命中启用厂商候选 /
  探针落空不支持且混合门返回 null / 双可用时原生优先 / 探针必以
  action+包名双匹配询问 / UUID→GUID 字节序显式钉子）→
  `AnwPhoneLinkWiring`、`AnwServiceProbe` Unresolved 编译失败。
- **实现**：`AnwPhoneLinkWiring.kt`——`AnwServiceProbe` 函数接口（隔离
  PackageManager）+ `vendorCandidate(...)` 组装（resolver = 探针
  `queryServiceCount("com.anwsdk.service.AnwPhoneLink", "com.anwsdk.service")>0`，
  生产探针即 `PackageManager.queryIntentServices`）。**Green 5/5**（与桥接
  合计 11/11）。
- **AIDL 定义移植**（来源：T32 fork `com.anwsdk.service` 三文件，GPL 同源；
  gh api raw 拉取，pwsh 管道会压扁换行须 cmd 重定向字节保真——环境备注）：
  `IAnwPhoneLink.java`（573 行，完整 Stub/Proxy，事务号 SPP 62–69 /
  Socket 222–233）、`IAnwSocketDataCallBack.java` / `IAnwSPPDataCallBack.java`
  （各 90 行）。**事务号与上游 #880 实机对勘表完全一致——双源 ABI 互证**。
  编译产物核对：`IAnwPhoneLink.class` + `$Stub` + `$Stub$Proxy` 均生成。
- **CarPlayController 接入评估（有意延后）**：本地控制器 BT 为 framework
  `BluetoothAdapter` 直用（1300/1304/1388/2574 行四处 + selectWireless
  BluetoothDevice 2574），无 T32 的 hybrid 抽象（其 4 行接入建立在 fork 已
  重构的接口上）——直接接入=盲改无测试保护的无线主路径。前置切片：轮次 27
  「无线 BT 依赖抽象化」（把控制器四处 BT 使用点收敛到 CarPlayBluetooth
  契约，原生路径行为不变，纯 JVM+Robolectric 可守卫），完成后接入即窄改。
- 全量回归：**shared 267 类 / 1777 用例 / 0 失败**（+1 类 +5 用例恰为
  WiringTest）；lint ANW+桥接全部文件零命中。
- BDD：装配层 5 用例映射补入 Anw 桥接场景节。


### 轮次 27a：无线 BT 依赖抽象化——实现级规格细化（本会话封盘，实现交棒）

- 四处直用点行为图景（本 Turns 实读源码）：①192–206 `currentBluetoothAdapter()`
  （BluetoothManager→adapter，回退 getDefaultAdapter）+ `awaitBluetoothEnabled`
  （STATE_TURNING_ON 轮询至 3s）；②1300–1309 无线启动预检
  （`BluetoothRadioCheck.blockerForState(adapterPresent, state, pausedByDiPlay)`）；
  ③1388–1401 连接执行（null→NO_ADAPTER；BydBluetoothSuspend.resumeAndWait；
  isEnabled→RADIO_OFF；`selectWirelessBluetoothDevice`；`accessoryBluetoothMac`）；
  ④2574+ 设备选择（bonded→手动地址→直连→唯一 iPhone→多 iPhone 失败——与
  `selectVendorWirelessTarget` 语义同构，双源互证）；RFCOMM 消费点 =
  **1478 行** `device.createRfcommSocketToServiceRecord(IAP2_IPHONE_UUID)` +
  既有 `BluetoothRfcommDuplexStream`（原生 socket→BlockingDuplexByteStream
  适配已存在，vendor 侧等价物 = HsaeRfcommStream）。
- **窄改设计（新会话按此实现，勿再探查）**：
  1. 1388–1397 段：adapter 获取改为 `CarPlayBluetoothManager.pick`（原生
     candidate = `currentBluetoothAdapter()?.takeIf { it.isEnabled || 可恢复 }` 包装）；
     原生可用 → **既有代码零变化**（含 Suspend/await/选择/MAC）；原生不可用 →
     vendor 路径（`selectVendorWirelessTarget` + `AnwPhoneLinkBluetoothAdapter`
     的 `openRfcommStream` + MAC=`localAddress()`）；
  2. 1478 流建立段：有原生 `BluetoothDevice` 走 `createRfcommSocket`（现状），
     vendor 路径直接用 `hybrid.openRfcommStream(address, IAP2_IPHONE_UUID)`
     返回的 BlockingDuplexByteStream（绕开 BluetoothDevice 类型）；
  3. 1300–1309 预检：原生不可用但 vendor.isSupported 时不抛 blocker（vendor
     栈 isEnabled=isSupported 语义）；`BydBluetoothSuspend` 仅原生路径调用。
- TDD：先写行为守卫（原生路径零变化——现有无线相关测试全绿即守卫）+ vendor
  路径用例（无原生时经混合门走 vendor 流）+ 预检语义（vendor 可用不抛
  NO_ADAPTER）。Robolectric 在 common（shared 无 Robolectric 依赖，控制器测试
  既有位置即 common）。
- 封盘原因：本会话 token 已 37M+（上下文压缩超两次临界），按 loop 强制交接
  规则封盘；轮次 25/26 已交付桥接核心+装配层+AIDL（全绿 267/1777/0），轮次 27
  实现按上述规格由新会话执行。


### 轮次 27b：无线 BT 混合门接入 CarPlayController（按 27a 规格实现，Red→Green）

- **Red**：`WirelessBluetoothPlannerTest`（5 用例：原生在场永远优先 / 原生缺失
  且厂商可达走桥接 / 厂商不可达 None / 双缺失 None / **探针崩溃降级 None 不破
  坏混合门**）→ Planner Unresolved 编译失败。
- **实现一（决策抽象）**：`WirelessBluetoothPlanner.kt`（sealed Plan：
  Native/Vendor/None，纯函数全分支纯 JVM 可测）+ `AndroidAnwServiceProbe.kt`
  （生产探针薄层：queryIntentServices + GET_META_DATA，runCatching 降 0）。
  **Green 5/5**（bluetooth 包合计 16/16）。
- **实现二（控制器窄改，按 27a 三点）**：①1300 预检——原生在场或厂商桥不可
  用走既有 blockerForState（零变化）；原生缺失且桥可用时跳过 blocker 并记
  vendor 日志；②1388 段——原生 adapter 非 null 走既有整段（Suspend/await/
  RADIO_OFF/设备选择/MAC，零变化），null 时经 `wirelessVendorBridge?.takeIf
  { isSupported }`（探针崩溃降 null），双缺失抛既有 NO_ADAPTER；vendor 分支
  目标=`selectVendorWirelessTarget`、MAC=`localAddress() ?: 02:00 匿名回退`；
  ③1478 流建立两型——vendor 用 `bridge.openRfcommStream(address, IAP2_UUID)`
  返回 BlockingDuplexByteStream（与既有 `BluetoothRfcommDuplexStream` 同接口，
  `Iap2Session.openWireless(stream)` 消费点天然统一——27a 探查结论兑现），
  原生 else 分支零变化（局部非空 nativeDevice 消除可空性）；InstantBluetoothLink
  启发式仅原生路径。字段：`wirelessVendorBridge` lazy + `wirelessVendorBridge
  Usable()`。中途编译修正×3（takeIf 安全调用 / btMac 可空回退 / else 分支
  device 可空性）。
- **验证**：`:shared:compileDebugKotlin` BUILD SUCCESSFUL；全量回归
  **shared 268 类 / 1782 用例 / 0 失败**（+1 类 +5 用例恰为 PlannerTest；
  控制器改造后既有全绿 = 原生路径零变化守卫）；**common 208 类 / 1676 用例 /
  4 失败恰为已知 POSIX sh 集**（flaky Mockito 未犯）；lint bluetooth 包零命中、
  模块存量 33 不变。
- 边界说明：vendor 路径的真机端到端（服务绑定→ANWBT_SocketConnect→iAP2 交换）
  仍待实车——纯 JVM 已覆盖决策与字节流语义，控制器接线由全量回归守卫原生
  行为。BDD Anw 场景节补映射。


### 轮次 28：上游检查点核查 + 验收文档补 Anw 桥接交付物

- 检查点核查（gh api）：#1071 仍 OPEN（未合并）、0.2.22 未发布（最新仍
  v0.2.21，10-17）、上游 main 仍 9d896330 无新提交——**无同步交叉验证触发**，
  检查点结论记入验收文档第七节。
- 验收文档补全轮次 25–27b 交付物：交付清单新增「AnwPhoneLink 蓝牙桥接」节
  （新包七文件 + `com/anwsdk/service` 三 AIDL 文件 + 控制器三处窄改，16 用例
  全绿）；复验命令表新增 `--tests com.shilapi.xcertplay.bluetooth.*`（16 用例）；
  已知限制补桥接真机路径待实车与检查点结论。


### 轮次 29：低负载与资源约束切片（用户新目标入 loop：卡顿/延迟/内存泄漏防护）

- 用户指令：「针对当前版本设计功能测试和集成测试多组场景，减少性能设备要求及
  负载，减少卡顿延迟内存泄漏」+「这条信息加入 loop 的任务目标切片」——已写入
  handoff 第九节 goal 文本（持续任务目标）。
- 缺口审查（实读实现）：①`showNissanDiagnostics()` 无防重入——连点 N 次 =
  N×4 并发子进程（低配车机卡顿/内存压力）；②采集最长 ~40s，Activity 销毁后
  `runOnUiThread` 弹对话框 = WindowManager 泄漏风险；③桥接流 close 后监听器
  释放无钉子用例。
- **Red**：新 `NissanDiagnosticsLifecycleTest`（2 用例）首跑双失败——
  `repeated taps must not spawn concurrent captures expected:<1> but was:<3>`、
  `no dialog may be shown from a destroyed activity`（精确命中缺口）。
- **实现**：`nissanDiagnosticsRunning` AtomicBoolean 防重入（compareAndSet）+
  回调处 `!isFinishing && !isDestroyed` 守卫后弹窗、复位标志。**Green 2/2**
  （UiTest 4/4 同步复验）；shared 补 `closedStreamReleasesItsVendorListener`
  钉子（close 后厂商回调不入队）→ Bridge 7/7。
- SDD 新增「低负载与资源约束设计」节（场景组×守卫×测试矩阵 7 行 + 资源预算
  条目：子进程 ≤4 串行/无常驻线程/FIFO 背压待实车证据/探针惰性）；BDD 新增
  三场景（防重入/销毁不弹窗/关闭即释放）。
- 全量回归：**shared 268 类 / 1783 用例 / 0 失败**（+1 钉子）；**common
  209 类 / 1678 用例 / 5 失败** = 已知 4×POSIX sh + **新确认第 2 个环境
  flaky**（SettingsBackupServerTest.severalIdleConnections…，并发时序类，
  单独重跑 30/0 全过，与本轮改动零交集，归档为已知）。


### 轮次 30：真机拆解样本模拟测试（用户指令：按网上拆解真机实际数据）

- 调研（WebSearch）：知乎/腾讯日产车机拆解实测——9 英寸 1280x720（歌乐制造）、
  i.MX6 主控；**勘误 NTDA3 = 屏驱/解码芯片代号而非主控**（RESEARCH 样本节）。
- TDD：`NissanTeanaRealWorldSampleTest` 3 用例（4.4.3 世代全字段 / Android 9
  双平台双行 wm / MCU 蓝牙形态）——首跑 2 失败暴露两个断言与实现的真实差异
  （board 非七属性、多行值字面 \n 转义），按真实行为修正后 **3/3 Green**
  （样本值仅测试，运行时零回填）。
- 模拟器真机规格模拟：1280x720 + density 160 + NISSAN → 最新 APK Diagnostics
  页几何验证（按钮高 60px 与卡内一致、对齐一致），截图
  `nissan_realspec_1280x720_d160.png`；规格已复位、模拟器已关。


### 轮次 31：跨会话交付与口径同步（用户指令：交付主会话 session_f4547e90）

- 交付通道（目标会话 runtime 不可直达）：①工作区交付文档
  `docs/SESSION_DELIVERY_TO_MAIN_f4547e90.md`（任务背景/交付清单/关键结论/
  接管入口）；②持久记忆 `delivery-to-main-session` + MEMORY.md 索引（主会话
  同项目自动加载）。
- 口径同步（轮次 29–30 成果此前未入交接链）：验收文档补三条复验命令
  （拆解样本 3 用例 / 生命周期守卫 2 用例）与已知限制第 7 条（低负载守卫 +
  真机规格模拟结论）；handoff 第九节 goal 文本更新至轮次 30（269/1786/0 +
  轮次 29/30 交付描述）。
- 接管视角终检：adb 无设备（实车输入仍不可得）；SDD 预算矩阵本地项全覆盖
  （FIFO 背压待实车证据）——本地可执行项保持已尽状态。


### 调研会话：模拟器功能测试矩阵 + 双路社区调研 + PRD（2026-10-26，独立调研会话，与轮次 29–31 并行；即交付文档所指向的 session_f4547e90）

- 用户指令：基于当前开发完成情况做模拟器功能测试检查（避免实车断点）+ 论坛
  资料收集（按开发文档要求）+ 汇总 PRD 供主会话 loop 切片。本会话**零代码变更**。
- **模拟器实测**（AVD `diplay_nissan_gapi` -writable-system + `diplay_nissan_35`
  基线；mobile debug 包 `com.shihab.diplay.hudtest` 本会话重新构建）：九项检查
  全部通过——①门控显示（NISSAN 属性 → 按钮在诊断卡，高 165px 与卡内一致）；
  ②门控拒绝（基线 AVD unknown，英文 locale 下诊断页 0 个 Nissan 节点）；
  ③端到端 CaptureFailed（信号行 `Permission Denial: ... missing
  android.permission.DUMP` 完整入对话框 + 重采指引；精确延迟 2.19s，x86 基线
  不外推实车）；④5 连点防重入（恰 1 弹窗、关闭后无第二弹窗）——与轮次 29
  守卫真机互证；⑤销毁不弹窗（点击后 300ms BACK，窗口输入通道销毁证据 + 无
  WindowLeaked/BadToken/FATAL + 重启无残留弹窗）；⑥约 24 轮采集 PSS 锯齿
  111.6↔114.3MB 无单调增长、无僵尸 sh/getprop/dumpsys 子进程；⑦七分类页冒烟
  0 FATAL；⑧定向单测复验 BUILD SUCCESSFUL（诊断 47 + 桥接 16 + UI 4 + 布局/
  翻译守卫）；⑨Reported/Undetected 分支模拟器不可达（Robolectric 已覆盖）。
- **双路调研**（中文 + 英文/俄文代理各一，≤2 并发合规；英代理首轮撞并发上限、
  单独重派成功）：新事实 R1–R13 全部入 PRD 第三节——版本造假警告（T32 README）、
  API19 无 ChaCha 0.1MB/s 与 Conscrypt 修复锚点、iAP2 静默 90s 误复位、音频坑
  三类（音源切换/Siri 静默流首 PCM 起轨/双音量不可调）、AudioTrack.
  getAudioAttributes API29 崩于 API28、kernel4.9 剥离 IPv6、原厂桌面白名单致
  侧载 app 不可见、API19 补丁面（VPN 对话框缺失/NCM NTB/USB 弹窗死锁/倒车
  抢占）、iPod 服务占用 USB（自动停需 root）、12.3 寸天籁 Android 10 车主报告、
  无线 40–60ms/有线 <5ms 对标、**仍未找到 2021 款天籁 8 寸机直接 getprop
  （实车首采仍是第一优先，与轮次 30 拆解样本互证）**、三仓库版本动态（上游无
  同步动作；Legacy v0.2.96、T32 v0.2.46-t32.4 有新版可对勘）。
- **交付物**：`docs/NISSAN_TEANA_PRD_20261026.md`——基线（对齐轮次 31 口径
  269/1786/0）、模拟器测试矩阵 T1–T9、证据分级数据 R1–R13、实车断点风险清单
  （6 条带缓解）、切片建议（P0-1 粘贴转录入口 / P0-2 代际决策门 / P1-3
  Robolectric 多 SDK 守卫 / P1-4 无线勘测清单增补 / P2 等外部输入）、上游检查
  点、红线重申。**注意**：P0-1 属新接线，与「验收前不接线」旧约定有张力，
  开片前需确认。
- 环境备注：后台 Bash 启动的模拟器会被会话回收（本会话首次实例遭外部正常关闭，
  时间线与轮次 30 的模拟器操作吻合）——改 `Start-Process` 脱离 shell 启动后
  全程无冲突；两 AVD 门控对照复验通过，属性注入仍有效。
- 计数：本会话约 15 次（2 构建 + 1 定向测试 + 10 模拟器测试批 + 2 文档）；
  绝对计数因与轮次 29–31 并行，由接管方在计数表合并（其终态 238 不含本会话）。


### 轮次 32：细致打磨——采集期间按钮禁用反馈（用户目标：功能细致打磨）

- Red：`NissanDiagnosticsLifecycleTest` 新用例
  `captureInProgressDisablesTheButtonUntilItFinishes` 首跑失败（按钮未禁用）；
  中途一次测试 import 修正（assertFalse）。
- 实现：`nissanDiagnosticsButton` 引用字段（卡片构造 `.also` 捕获）+ CAS
  成功即禁用、回调恢复。**Green：Lifecycle 3/3、UiTest 4/4**。

### 轮次 33：PRD 入账 + 双 SDK 矩阵守卫（P1-3）+ 并行开发派发

- **PRD 入账**：调研会话交付 `docs/NISSAN_TEANA_PRD_20261026.md`（T1–T9
  模拟器实测全过、R1–R13 社区证据、六项断点风险、切片建议 P0-1/P0-2/
  P1-3/P1-4/P2）——本 loop 采纳其切片优先级，按序推进。
- **P1-3 双 SDK 矩阵守卫**：`NissanDiagnosticsUiTest` 与 LifecycleTest 的
  @Config 升为 `sdk=[28, 34]` 双档（28=天籁 Android 9 假设、34=开发基线，
  动因=PRD R5 API 门崩溃类）→ **双档全绿（Ui 8=4×2、Lifecycle 6=3×2）**：
  现有诊断代码无 API 门缺陷。
- **并行开发派发（用户指令：GLM-5.3 最高档并行）**：代理 A = P0-1 粘贴
  转录入口（TDD 全流程 + 9 locale 字符串 + 全量验证）；代理 B = P0-2 决策门
  + P1-4 勘测增补（纯文档三文件追加）。两代理不写 worklog/docs 重叠区，
  完成报告由主线（本会话）合并记录。P0-1 的「验收前不接线」约定：用户连续
  开发指令（打磨/并行开发）视为开工确认，已在派发任务书中注明红线。


### 轮次 34：并行代理三线合并（P0-1/P0-2+P1-4/对勘）——并行开发轮

- **代理 A（P0-1 粘贴转录入口，TDD 全程）**：Red（4 处 Unresolved，
  %TEMP%\p01_red.log）→ 实现（DiPlayActivity 粘贴按钮 + 多行等宽输入框 +
  NissanDiagnosticsCapture.from(OWNER_REPORTED) 复用既有编排与三分支 UI +
  防重入/销毁守卫）+ 4 字符串 ×9 locale → Green **12/12**（6 用例 ×2 SDK：
  完整转录→15 行 owner-reported / 裸 dumpsys 不误采集 / 空白不弹 /
  detected=false 拒绝 / 按钮门控 / 输入框+标签指引）；其全量 common 1698 用例
  仅既有 5 已知失败。**主线独立复验**：PasteTest 12/12 复跑 ✓、字符串直查
  ✓（注意：cmd 嵌套 for 的 %k 不展开曾致 8 目录假 MISSING——多键核查用直查）。
- **主线补拍（A 遗留）**：粘贴对话框截图 ×2（`nissan_paste_dialog_zh_font150.png`
  输入框+指引+解析按钮；`nissan_paste_report_zh_e2e.png` 尝试注入）——按钮
  高 165px 与日产按钮一致、指引含三标签说明；UI 文本注入受 adb input text
  %XX 不解码限制，端到端逻辑由 PasteTest 覆盖（记录跳过原因）。
- **代理 B（P0-2+P1-4 文档批）**：SDD 代际决策门（sdk=19→Legacy 路线、仲裁
  字段钉死）+ 用户指南两节（桌面不可见预案/勘测增补）+ BDD 钉子映射——三文件
  追加 16/24/18 行，主线核验在位。
- **代理 C（T32/Legacy 对勘，只读）**：报告全文入 RESEARCH 新节——关键结论：
  keepalive 声明一行级高价值、changeModes 协议级缺口、onTaskRemoved 误拆、
  无线韧性四件套（60s 看门狗/3s 重连/停顿自愈催帧）、倒车非破坏性恢复、
  **Anw 桥接无需同步**（t32.4 未触 bluetooth 包，三级链留档）、阶段 2 无线
  首测基线引用 t32.4 实测数字。缺口清单已入 RESEARCH 供阶段 2 排队。
- 主线收尾：独立全量回归（后台进行中）；清理/推送按用户指示等实测后执行。



### 轮次 35：角色形象 DIY × MikuCarLauncher 互补调研（专项调研轮）

- **调研对象**：用户备份 `D:\车机改造参考仓库\MikuCarLauncher1.0/2.0/3.0`（无 LICENSE，
  **作者授权已由用户于 2026-10-27 取得**，作者 X @jlxc2001）+ DiPlay 现有地基 +
  全平台生态检索（GitHub/奇珀网/抖音/B站/YouTube/X/Reddit/TikTok）。
- **关键结论**：Live2D 角色装饰层可行但必须「分级渲染 T0/T1/T2 + CarPlay 会话互斥
  （销毁 WebView）+ 默认 30fps/quality1.0」；`:home` 嵌入式地图 minSdk=30 不兼容日产，
  地图走悬浮卡；形态定为新模块 `:nissanhome`；悬浮窗桌宠路线因与倒车/地图卡抢层否决。
- **产出**：`docs/CHARACTER_DIY_RESEARCH_20261027.md`（MikuCarLauncher 技术栈与
  v35–v67 性能踩坑清单、互补盘点、许可合规四条、三轮设计 loop 记录、S1–S6 切片计划、
  NTDA3 性能预算线、风险清单）。
- **口径**：本轮调研/文档操作约 30 次（文件读写+命令+网络检索）；无代码改动，
  CI 未触发（报告轮）。
