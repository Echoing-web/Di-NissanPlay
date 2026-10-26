# 日产天籁适配 PRD(调研会话产出,供主会话 loop 切片)

- 日期:2026-10-26(调研会话,非开发会话)
- 分支:`feat/nissan-teana-adaptation`;HEAD 仍 f65d5e86;全部未提交、未推送
- 读者:主会话(按 `NISSAN_TEANA_PARSER_HANDOFF.md` 第九节 goal 续推的 loop)
- 本文是用户指令「基于当前开发完成情况做模拟器功能测试检查 + 论坛资料收集 +
  汇总 PRD 供主会话切片」的交付物。它**不改变任何代码**;新增证据行**不并入**
  `NISSAN_TEANA_HEADUNIT.md`(该文件属他人未提交修改,红线不动)。

## 一、当前基线(截至轮次 28/29,引自既有文档,未重新验证)

- 诊断链路九件套(解析/转录/编排/命令/执行器/报告 + Outcome UI)+ AnwPhoneLink
  蓝牙桥接四层(纯 JVM 核心/装配层/AIDL×3/CarPlayController 窄改)全部完成。
- 全量回归(轮次 30 终态,经交付文档 `SESSION_DELIVERY_TO_MAIN_f4547e90.md` 口径
  同步):**shared 269 类 / 1786 用例 / 0 失败**;common 仅 4 个已知 POSIX sh 环境
  失败 + 1 个已归档 flaky;新增代码 lint 零命中。验收材料
  `NISSAN_TEANA_ACCEPTANCE.md` 就绪,独立验收未执行。操作计数 238(不含本会话)。
- 轮次 29 三守卫(诊断防重入、销毁不弹窗、桥接监听器释放)已入 SDD 资源预算矩阵。
- 最高价值排队项仍为**实车输入**(ADB 转录);上游检查点 #1071 OPEN、0.2.22 未发。
- 并发说明:本 PRD 撰写期间,另一 loop 会话完成轮次 29–31 并通过交付文档向本线
  交接口径:轮次 29 的三守卫与本会话模拟器 T4/T5 实测互证;轮次 30 新增真机拆解
  样本测试(`NissanTeanaRealWorldSampleTest` 3 用例,并勘误 NTDA3 = 屏驱/解码
  芯片代号、非主控)与本 PRD 结论无冲突。

## 二、模拟器功能测试与检查结果(本会话实测,证据级=实测)

环境:AVD `diplay_nissan_gapi`(Android 15 / google_apis,`ro.product.manufacturer=NISSAN`
属性注入,`-writable-system` 启动)+ AVD `diplay_nissan_35`(默认镜像,manufacturer=unknown,
英文 locale)。被测物:`:mobile:assembleDebug` 产物(`com.shihab.diplay.hudtest`,本次构建)。
驱动方式:raw adb + uiautomator 脚本(脚本在 %TEMP%,不入库)。

| # | 场景(对应 SDD 场景组) | 结果 | 关键证据 |
| --- | --- | --- | --- |
| T1 | 门控显示:NISSAN 属性 → 诊断卡片出现「日产诊断」按钮 | **通过** | 按钮 bounds=[99,1362][981,1527],高 165px 与卡内其他按钮一致 |
| T2 | 门控拒绝:default 镜像(unknown)→ 诊断页 0 个 Nissan 节点 | **通过** | `GATE_NISSAN_BUTTON_PRESENT=0`(英文 locale 下同样成立) |
| T3 | 端到端 CaptureFailed:点击 → 后台采集 → 对话框 | **通过** | 对话框列出信号行 `Permission Denial: can't dump DisplayManagerService from pid=3113, uid=10209 due to missing android.permission.DUMP permission` + 重采指引;**精确延迟 2.19s** |
| T4 | 防重入:采集窗口内 5 连点 | **通过** | 仅 1 个对话框;关闭后无第二弹窗(轮次 29 Robolectric 守卫的真机复现) |
| T5 | 销毁不弹窗:点击后 300ms 按 BACK | **通过** | logcat 见窗口输入通道销毁(`Input channel ... disposed`),无 WindowLeaked / BadTokenException / FATAL;重启后无残留弹窗 |
| T6 | 内存与进程清理:累计约 24 轮采集 | **通过** | PSS 锯齿 111.6↔114.3MB(无单调增长);无残留 `sh`/`getprop`/`dumpsys` 子进程;10 轮每轮恰 1 弹窗;全程 logcat 无崩溃 |
| T7 | 分类冒烟:连接/显示/音频/导航/车辆/高级/诊断 七页遍历 | **通过** | 页面均渲染(节点数 24–43),0 FATAL |
| T8 | 定向单测复验(诊断 47 + 桥接 16 + UI 4 + 布局/翻译守卫) | **通过** | `:shared:testDebugUnitTest` + `:common:testDebugUnitTest` 定向任务 BUILD SUCCESSFUL |
| T9 | Reported / Undetected 分支 UI | 不适用 | 需要 `DUMP` 权限或 `detected=false` 且按钮可见的组合,模拟器不可达,Robolectric 已覆盖 |

**结论:当前版本在模拟器上无阻断缺陷;app uid 下 `wm` 报 "Can't find service: window"
(非信号,按设计静默丢弃)、`dumpsys display` 报 Permission Denial(信号,保留)——
与 SDD 预测逐条吻合。** 模拟器 x86 与车机 i.MX6 性能不可比,T3 的 2.19s 仅作回归基线,
不外推到实车。

## 三、按开发文档要求收集的数据(社区调研,证据分级)

本节按 `NISSAN_TEANA_HEADUNIT.md` 证据矩阵的口径补充新行,供文档所有者并入
(本会话不触碰该文件)。调研路:中文社区 + 英文/俄文社区双代理;既有调研
(`NISSAN_TEANA_RESEARCH_20261026.md`)已覆盖的条目不重复。

| # | 事实或线索 | 来源 | 证据类型 | 可信度 | 对切片的意义 |
| --- | --- | --- | --- | --- | --- |
| R1 | T32 README 明确警告:**部分 NTDA3 车机系统版本造假**(设置显示假 Android 10/11/12,实际 4.4/5.1);社区工具 headunit-version-inspector 可检测 | github.com/winkinwong/DiPlay-Legacy-Android-T32(README) | 项目文档(实机教训) | 高 | 诊断报告的 `ro.build.version.*` 只能作参考;实车判定代际还需 kernel/ABI 旁证 |
| R2 | API 19 无系统 ChaCha:软件解密仅约 **0.1 MB/s(5–6fps)**;Conscrypt 注册进 Security provider 后 1.2–5.9ms/帧(~30fps)、3.5–23.7MB/s | issues/298 + T32 README(实测) | 实测日志 | 高 | 阶段 2 性能预算锚点;若实车为 API 19 世代,HEVC/码率设置是保命手段 |
| R3 | iAP2 控制通道静默 90s 即误复位;奇骏上 iPhone 静默可更久;触摸→画面延迟 24–126ms | issues/134(实测) | 实测日志 | 高 | 阶段 2 健壮性:复位阈值不能只看控制通道静默 |
| R4 | 音频坑三类:①PCM 正常但车机静音 → 需手动切音源到「安卓媒体/AV」(issues/134);②Siri 静默流不发 PCM 会拖死车机音频,须延迟到首个 PCM 才起轨(T32 commit 1bd1d44);③双音源音量不可调、通话麦克风丢失(issue 268) | issues/134、268;T32 | 实测 | 高 | 阶段 2 音频焦点/通道矩阵的最高频失败类别(与 HEADUNIT.md A6「共存版」清单互证) |
| R5 | AudioTrack.getAudioAttributes()(API 29)在 **API 28 车机上直接崩溃** | kavvos.com/zh/posts/nissan_carplay(逍客 Android 9 世代实测) | 车主实测(逆向) | 中 | 新增代码必须 API 门守卫;Robolectric 多 SDK 矩阵的动因(切片 P1-3) |
| R6 | Android 9 世代 kernel 4.9 **剥离 IPv6**(`ip -6 route` 为空):通告 link-local IPv6 不可达,须强制 IPv4 网关 | kavvos.com(同上) | 车主实测 | 中 | 与上游 #1071(StartSession 只带单地址)叠加后无线路径风险加倍;实车勘测补 `ip addr show` |
| R7 | 原厂桌面读硬编码白名单(config_with_dvr.json / white.txt),侧载 app **在桌面上不可见**;车主装 DUDU Launcher Pro 解决 | kavvos.com(同上) | 车主实测 | 中 | 实车首查项:DiPlay 图标是否可见;不可见时的进入路径(adb/启动器)要提前写进用户指南 |
| R8 | 倒车影像与 CarPlay 抢占冲突、倒车后黑屏(Legacy #300/#320,v0.2.96 修复);系统 USB 弹窗在 4.4.2 死锁(#311);wired 模式 getDeviceList() 偶发空(#266);NCM NTB bulk-OUT 超时(API 17,#307);VPN 权限对话框在 4.4.3 缺失(#105) | programmerguohuajing/DiPlay-Legacy-Android issues | 实测 | 高 | 若实车为 API 19 世代,这些是主线代码根本没有的补丁面——**只能走 Legacy fork 路线** |
| R9 | iPod 服务(com.hsae.ipod)占用 iPhone USB 接口;T32 的「USB 接入自动停 iPod 服务」为实验功能**默认关、需 root**;USB 偶发 write errno=19 需重试 | T32 README + commits bc534fb/ce00d3e;issues/134 | 实测 | 高 | 有线 CarPlay 前置检查项;自动停 iPod 服务与本项目「不 root」红线冲突,只能记录实测影响 |
| R10 | 12.3 寸天籁车机「硬件版本 S02.03、系统安卓 10」车主报告 | sv.m.yiche.com/detail/?id=115257073 | 车主转述 | 中 | 天籁(非兄弟车型)少见的直接平台数据;但 12.3 寸非目标车(8 寸) |
| R11 | 无线 CarPlay(2.4GHz 热点)触摸延迟 **40–60ms**、HEVC 降码率 ~40%;有线 USB **<5ms** 零丢包 | kavvos.com | 车主实测 | 中 | 阶段 2 验收阈值候选基准(盒子方案对标) |
| R12 | **仍未找到 2021 款天籁 8 寸机的直接 getprop / wm size 输出** | 双路代理检索结论 | — | — | 「天籁本机首次采集」仍是第一优先实车任务(与既有结论一致) |
| R13 | 上游动态(gh api,2026-10-26):v0.2.21 仍最新(0.2.22 未发);#1071 仍 OPEN(0 评论);#1085 已关;#1119/#1108/#880 OPEN;Legacy v0.2.96(10-25,修解码死锁/BT 音频切换无声);T32 v0.2.46-t32.4(10-21) | github.com/shihabal3amri/DiPlay 等三仓库 | 实测(api) | 高 | 检查点无同步动作;T32 有新版本可对勘 |

## 四、实车断点风险清单(按危险度排序,每条给预建缓解)

1. **代际不确定 = 安装门槛断点(最高危)**:主线 DiPlay minSdk=24(Android 7.0)。
   若 2021 天籁实为 API 19 世代(同平台轩逸/奇骏实测均为 4.4.3,R1 还证明版本号可
   造假),主线 APK **根本装不上**,且 R8 显示 API 19 需要的补丁面(VPN 对话框缺失、
   NCM NTB、USB 弹窗死锁)主线完全没有。**缓解**:诊断报告已记录 sdk 级别(仲裁字段);
   切片 P0-2 把「实车 API 19 → 转 Legacy fork 路线」写成显式决策门;实车首采清单把
   `ro.build.version.sdk` + kernel 版本列为第一优先。
2. **app uid 权限断点(已实证)**:本会话 T3 + AOSP 权限模型 + R5 证实:无 root 的
   第三方 app 跑 `dumpsys display` 必然 Permission Denial。**车机内一键采集大概率
   只能产出 CaptureFailed**。**缓解**:切片 P0-1 粘贴转录入口——车主在 PC 上用 ADB
   采集后粘贴文本,复用既有解析器(source=owner-reported)。
3. **无线 StartSession 断点(#1071 未合并 + R6 IPv6 剥离)**:若走无线路径,
   上游载荷缺陷 + 车机无 IPv6 可能同时命中。**缓解**:继续上游检查点;实车勘测记录
   `ip addr show`;#1071 合并后按 #1084 模式交叉验证(既有流程)。
4. **可见性断点(R7)**:侧载后 DiPlay 可能不出现在原厂桌面。**缓解**:实车首查项;
   用户指南补「图标不可见时」的进入预案(仅文档,不写运行时)。
5. **USB 数据口断点(R9)**:com.hsae.ipod 占用接口且无 root 不能自动停。**缓解**:
   阶段 2 有线路径的首测项;实测影响记录进勘测清单,不做任何自动停服务逻辑。
6. **性能断点(R2/R11)**:i.MX6 上 5–6fps 的失败签名与 Conscrypt 修复路径已知。
   **缓解**:阶段 2 开工前的预算复核(SDD 资源矩阵已含解析与子进程预算);模拟器
   无法复现,不投资模拟器性能仿真。

## 五、主会话 loop 切片建议(按优先级;每片先补 SDD/BDD 再写测试)

> 通用要求沿用既有规则:Red→Green 留痕、场景↔测试映射、新字符串 ×9 locale、
> 不动他人六文件、不把传闻值写进运行时。

### P0-1 诊断粘贴转录入口(owner-reported)

- **动机**:断点 #2(已实证)。解析器从设计起就支持粘贴转录,但没有任何 UI 调用方;
  车机内采集必败的前提下,这是诊断链路对实车唯一可用的完整路径。
- **SDD 输入**:在诊断卡片(或 CaptureFailed 对话框内)增加「粘贴诊断转录」入口:
  多行文本框 → `NissanDiagnosticsTextParser.parse(text, "owner-reported",
  NissanHeadUnit.detected)` → 复用三分支 Outcome UI(粘贴文本含信号行 → 重采提示;
  detected=false → 拒绝;干净 → 报告)。注意与 SDD「调用方接线规则」一致:粘贴时
  需要分区标签行的用户指引文案。
- **BDD 场景(新增)**:粘贴含 `[k]: [v]` + 分区标签的转录 → 报告 15 行,来源
  owner-reported;粘贴裸 dumpsys(无标签)→ 相应字段 unknown(不误采集);粘贴空
  → 不弹报告;detected=false → 拒绝。
- **TDD**:Robolectric 注入(不走 CommandCapture);解析器纯 JVM 既有 24 用例零改动。
- **验收**:全量 shared/common 无新增失败;UI 字符串 ×9 locale;大字号 + RTL 截图。
- **依赖/风险**:⚠️ 既有约定「验收前不接线」——本切片属于**新接线**,若独立验收方
  坚持先验收旧代码,则排到验收反馈之后。主会话开片前确认。

### P0-2 代际决策门 + 仲裁字段强化(文档为主,小切片)

- **动机**:断点 #1。把「实车 API 19 → 主线不可装,转 Legacy fork 路线」写成
  HEADUNIT.md/PRD 之外的显式决策门(放 SDD 证据与升级条件节或用户指南),并在
  诊断报告 BDD 里钉住:`ro.build.version.sdk` 是代际仲裁字段,禁止被任何公开
  资料值覆盖。
- **TDD**:1 个报告层用例(sdk 字段来自输入、无默认值)——预计已有覆盖,缺则补。
- **验收**:决策门文档入索引;无运行时代码变更。

### P1-3 Robolectric 多 SDK 矩阵守卫(API 门防崩溃)

- **动机**:R5 类缺陷(AudioTrack.getAudioAttributes API29 崩于 API28)是车机
  世代差异的典型断点;轮次 15 的 shared lint 已修过同类(destroyForcibly 守卫)。
- **做法**:为日产诊断 UI 测试与桥接相关 common 测试加 `@Config(sdk=[28, 34])`
  双档(28 = 天籁 Android 9 假设、34 = 开发基线),新代码 API 26+/28+ 调用若无守卫
  即红。
- **验收**:双档全绿;发现并修复真实 API 门(如有)→ 按 TDD 留痕。

### P1-4 无线路径勘测清单增补(纯文档)

- 动机:断点 #3/#4。在用户指南/勘测清单(不动 HEADUNIT.md)补:`ip addr show` 摘要、
  桌面可见性检查、进入预案;记录 R11 的 40–60ms/<5ms 作为阶段 2 验收阈值候选。

### P2(等外部输入,不排期)

- 音频焦点/方控/状态栏矩阵 BDD(阶段 2 前置,等实车与 iPhone);
- 桥接 FIFO 背压设界(等真实厂商流证据,SDD 已有条件条目);
- 实车采集 → KNOWN_UNIT_IDENTIFIERS 扩充;独立验收反馈修复;
- HEADUNIT.md getprop 命令修正(等所有者);
- (可选)JMH 基准——维持不做决策,1.4MB/0.041s 冒烟已够。

## 六、上游检查点(2026-10-26,本会话复查)

v0.2.21 仍为最新 release;#1071 OPEN 未合并(0 评论,10-24 更新);#1085 已关闭;
#1119/#1108/#880 OPEN;上游 main 无新提交。**无同步动作**。Legacy v0.2.96(10-25)
与 T32 v0.2.46-t32.4(10-21)有新版本,阶段 2 对勘时可参考其修复集(R4/R8/R9)。

## 七、红线与约束(对主会话重申,无变化)

不 git add/commit/push;不动他人未提交六文件(README.zh-CN.md、BRANCHING.md、
NISSAN_PORT_PLAN.md、NISSAN_TEANA_HEADUNIT.md、NissanHeadUnit.kt 及其测试);
传闻值(1280x720、NTDA3、P1xx)不进运行时代码;等待独立验收;构建用 pwsh 7 内联 +
X: 映射;模拟器 `diplay_nissan_gapi` 须 `-writable-system` 启动(本会话验证:属性
注入仍有效);上下文压缩超两次即交接。

## 附:本会话证据索引

- 模拟器:两 AVD 实测记录见第二节表格;UI 转储与脚本在 %TEMP%(会话级,未入库);
  无新增截图(既有 `nissan_e2e_capture_failed_real.png` 与本会话 T3 同分支)。
- 测试:定向 gradle 任务 BUILD SUCCESSFUL(2026-10-26 本机,10s)。
- 调研:R1–R13 全部带 URL;两代理原始报告未全文入库(沿既有惯例),本文含全部
  可执行结论。
- 操作计数:本会话按「文件变更或测试执行计 1 次」口径约 15 次(2 构建 + 1 定向
  测试 + 10 项模拟器测试批 + 2 文档),详见 worklog 条目。
