# 角色形象 DIY × MikuCarLauncher 互补调研（2026-10-27）

调研问题：基于 Di-NissanPlay 现有基础，能否吸收 MikuCarLauncher 的"角色形象 DIY"能力，
让车主 DIY 自己喜欢的角色（或更多个性化需求），同时不给日产天籁 NTDA3 弱车机增加过多负担。

本文只记录调研结论与设计，未改动任何代码。所有外部信息标注来源；所有本地结论标注代码路径。

---

## 1. 结论摘要

1. **可行，且互补性强**。MikuCarLauncher 证明了"WebView + Live2D 角色装饰层"在车机弱硬件上可以稳定运行
   （作者从 v48 迭代到 v0.7.5.5 / 公开分发版 0.13.38，仍在活跃维护）；但它是一个孤立的 Java 单体桌面，
   没有 CarPlay、没有诊断链路、没有多语言与测试体系。Di-NissanPlay 反之：有完整的 CarPlay 客户端、
   启动器集成协议、日夜/环境光主题、解码器管理与设置 IA 规范，唯独没有"角色形象 DIY"这一层。
   两边几乎不重叠，是标准的互补组合。
2. **性能约束可以满足，但必须用"分级渲染 + 会话互斥"**：低配机 WebView 里 Live2D 与主界面同跑会跌破
   20fps（外部实测证据见 §7），所以角色层必须：只在启动器桌面显示、CarPlay 会话激活时彻底销毁
   WebView、默认 30fps / 画质 1.0、提供静态图与 Lottie 低档位兜底。
3. **形态定为一个新模块 `:nissanhome`（日产车机桌面）**，而不是往 mobile 里塞装饰层，也不是独立悬浮窗桌宠。
   理由与被否掉的方案见 §8。
4. **许可前提**：MikuCarLauncher 仓库无 LICENSE 文件，默认保留所有权利；**作者授权已由用户于 2026-10-27
   取得**（作者 X 账号 [@jlxc2001](https://x.com/jlxc2001)），复用代码时须在仓库内保留出处说明。
   Live2D Cubism Core 是专有许可：可嵌入 APK 免费发布（小规模/个人收入门槛内），但**不得把 Core 文件
   提交进 Git**，应在构建时下载（MikuCarLauncher 的 `prepareOfflineLive2DRuntime` Gradle 任务就是范本）。
5. **生态空白**：国内外没有任何开源项目同时具备"CarPlay + 用户可导入的 Live2D 角色桌面"。国内嘟嘟/迪友
   等桌面只有内置"萌宠主题"；海外社区（r/Androidheadunits、YouTube、TikTok）只有开机 Logo、痛车贴膜、
   动漫语音包，没有任何角色桌面工具。做出来在两个社区都是首发内容。

---

## 2. 调研范围与方法

- 本地：`D:\车机改造参考仓库\MikuCarLauncher1.0 / 2.0 / 3.0`（用户备份，无 LICENSE，作者授权已取得）；
  DiPlay 仓库（`mobile` / `common` / `shared` / `automotive` / `samples/home` / `samples/maphost`、docs/*）。
- 外部：GitHub（gh 搜索 + api）、奇珀网/第一游戏网下载页、X（作者开发日记）、YouTube、Reddit、TikTok、
  抖音、B站、酷安生态站群（dudu-lucky.com、budingui.com、lecoauto.com、autoui.cn）。
- 网络检索一轮失败重试一次（并发限制），未留缺口：海外社媒按用户要求单独补了一轮。

---

## 3. MikuCarLauncher 仓库解读

### 3.1 形态与平台

- 面向改装安卓车机（作者的测试机为 32:9 2560×720 的奥迪 A4L 改装主机，`com.ts.MainUI` CAN 系统）。
  设计坐标系固定 2560×720。
- Java 单模块工程：`app/src/main/java/com/jlxc/mikucarlauncher/` 下 46 个源文件，
  主界面为自绘 Canvas（`LauncherCanvasView.java` 135KB）+ 分层 View（背景层 → Live2D 装饰层 → 桌面 UI 层
  → 小组件层，见 `MainActivity.java:53-177`）。
- 版本演进 82+ 个版本（v17 → v0.7.5.5），公开分发版已到 **0.13.38**（2026-10-16，25.88MB，奇珀网
  [m.7po.com/rj/14572.html](https://m.7po.com/rj/14572.html)、
  [diyiyou.com/soft/31525.html](https://www.diyiyou.com/soft/31525.html)），新增 OBD 车况、倒车影像
  自动弹出、KWDT/KWGT 小组件等。README 中"关于软件"页含作者 B站/抖音/X 信息。

### 3.2 角色 DIY 技术栈（Live2D 全链路）

核心文件：`Live2DDecorView.java`（543 行）、`assets/live2d/live2d_decor.html`（908 行）、
`Live2DModelImporter.java`（32KB）、`Live2DSettingsActivity.java` / `Live2DAdjustActivity.java`。

- **渲染**：透明 WebView（`LAYER_TYPE_HARDWARE`）+ PIXI 6.5.10（WebGL）+ pixi-live2d-display 0.4.0
  + Cubism 2 (`live2d.min.js`) 与 Cubism 4 (`live2dcubismcore.min.js`) 双核，覆盖新旧模型格式。
- **用户 DIY 流程**：SAF 选文件夹导入模型（递归复制到应用内部目录，自动找 `model3.json`/`model.json`）
  → 拖动/双指捏合直接调整中心点与缩放（`pointerMode=3` 修复捏合后残指拖走模型的 bug）
  → 设置画质倍率 0.5–2.0（渲染分辨率 = devicePixelRatio × quality）与帧率 15–60（`ticker.maxFPS`）。
- **动效**：读取模型 Motion Groups，每 6–12 秒随机播放动作并随机表情；点击人物切下一个动作；
  无动作模型注入通用动作/表情（v0.7.4.2 起默认关闭）+ 参数级兜底待机（眨眼、转头、呼吸，
  直接写 `ParamEyeLOpen` 等参数，兼容 Cubism 2/3/4 两套参数名）。
- **稳定性**：JS 每 2 秒心跳 → Java 层 watchdog 每 5 秒检查（加载后 12 秒宽限，断心跳 >10s 软重载，
  连续 2 次失败重建 WebView）；夜间模式给 canvas 加 brightness 滤镜单独压暗角色。
- **运行库三级回退**：APK assets → `/sdcard/MikuCarLauncher/live2d/runtime/` → 在线 CDN；
  Gradle 任务 `prepareOfflineLive2DRuntime` 构建时下载 4 个 JS，**Core 不入库**（许可合规，见 §6）。
- **遮罩**：CSS 裁切 + Pixi mask 双层，只裁角色不裁背景/卡片（v54/v56 修复"露腿"）。

### 3.3 演进史里的性能踩坑清单（对日产最有价值的部分）

| 版本 | 教训 |
| --- | --- |
| v35 | 应用扫描/图标加载必须进后台线程，否则低配车机进抽屉主线程卡死 |
| v58 | 激进的 low-power WebGL 参数 + 20fps 限制在部分车机 WebView 上直接导致 canvas 初始化失败 |
| v59 | 缩略图缓存降到 72dp + 内存缓存 + 只在安装/卸载时重建，避免车规慢存储反复解码 |
| v60 | 重载要有防抖（12s），否则短时间反复 reload 更糟 |
| v61 | 回滚 v58 激进参数——"能加载"优先于"最省电" |
| v62 | 把画质/帧率做成用户可调（默认稳态），比自动探测可靠 |
| v64 | 取消自动重载（闪烁），只保留手动修复入口 |
| v67 | 心跳 watchdog 自愈：车机长时间运行后 WebView 空白但 Java 层仍存在的场景 |

### 3.4 其他可借鉴的 DIY 能力

图标包（Nova 兼容 `appfilter.xml`，v46–47）、单应用换图标/重命名/隐藏（v46）、
日/夜壁纸分别自定义（v43/47）、天气卡（v41 中国天气免 Key）、预设备份导出/导入（v0.7.3.4）、
UDP HUD 广播与后置文字屏节点（v71/72.1，配合外接屏）。车辆数据部分（`VehicleDataProvider` 绑定
`com.ts.MainUI` CarInfoService 读 baseInfo[82]）**不可移植**——那是 A4L 改装主机的私有 Binder 协议，
日产要走 DiPlay 自己的 `NissanHeadUnit` 闸门与诊断链路。

---

## 4. Di-NissanPlay 现状盘点（互补视角）

- 模块与 SDK：`settings.gradle.kts` → `:common`、`:mobile`、`:automotive`、`:shared`、
  `:maphost→samples/maphost`、`:home→samples/home`。minSdk：mobile/common **24**，automotive 28，
  home/maphost **30**（注释注明 `SurfaceView.getHostToken / setChildSurfacePackage` 需要）。
  UI 为 Jetpack Compose（mobile/common/automotive），`samples/home` 为纯 View 体系
  （`HomeActivity.kt` 12KB + `DiPlayMapPanel.kt` + `WidgetBoard.kt`，共约 30KB 源码）。
- 启动器集成四机制（docs/LAUNCHER_INTEGRATION.md）：导航小组件（4×2，任何启动器）、悬浮地图卡
  （SYSTEM_ALERT_WINDOW + Usage Access，**在任意默认桌面可用，含 Android 9**）、嵌入式地图
  （Messenger + SurfacePackage，**仅 Android 11+**）、桌面悬浮球。**日产 Android 9/10 上：嵌入式
  地图不可用，地图卡与小组件可用**——这决定了日产桌面的地图形态。
- 主题体系：`CarPlayNightMode`（SYSTEM/AMBIENT）、`AmbientLightThreshold`（lux）、
  `AmbientAlbumPalette`（专辑封面取色）、`AppAppearance`（common/src/main/java/com/shilapi/xcertplay/）。
  角色层的夜间压暗可以直接复用"日夜判定"的模式。
- 媒体栈与性能先例：`shared/src/main/java/com/shilapi/xcertplay/media/` 下
  `MediaCodecSupport` / `MtkDecoderTuning` / `VideoDecodeQueue`——已有按芯片调优解码器的先例，
  "给重资源功能做闸门与预算"是既有工程文化。
- 缺口：无 CarPlay 会话状态对外广播（grep 仅见 BYD HUD 输出广播）；无图像加载/动画库依赖
  （无 Glide/Coil/Lottie）；无角色/壁纸/图标包能力。

---

## 5. 生态与平台调研

### 5.1 GitHub 开源项目

| 项目 | 星数/活跃 | 与本课题关系 |
| --- | --- | --- |
| [guansss/pixi-live2d-display](https://github.com/guansss/pixi-live2d-display) | 1516★，MIT，最后推送 2024-08 | Live2D 网页渲染核心；Cubism 2.1/4；支持本地文件加载；版本应锁定 0.4.0（PIXI 6 配套） |
| [dw2lam/openlauncher](https://github.com/dw2lam/openlauncher) | 189★，2026-07 活跃 | 面向改装车机的离线优先启动器，架构参考 |
| [seijikohara/femto-car-launcher](https://github.com/seijikohara/femto-car-launcher) | 34★，Compose，Android 13+ | 现代化车机桌面参考（但 minSdk 过高不适用日产） |
| [fvfpq/desktop-pet-android](https://github.com/fvfpq/desktop-pet-android) | 活跃 | Live2D 悬浮窗桌宠 + AI 聊天，交互参考（悬浮窗方案被我们否决，见 §8-R1） |
| [airbnb/lottie-android](https://github.com/airbnb/lottie-android) | 主流动画库 | T1 档位候选；6.1 起多线程渲染（[作者文章](https://gpeal.medium.com/lottie-android-6-1-lottie-goes-multithreaded-67c09c091fd7)） |
| [Live2D Cubism SDK](https://www.live2d.com/en/sdk/license/) | 官方 | 原生 SDK 是远期升级路线（性能上限更高，集成成本高） |

### 5.2 国内车机桌面生态与平台

- **嘟嘟桌面**（dudu-lucky.com）：事实标准级产品，抖音教程 98 万赞，有比亚迪/风神/领克/哈弗专享版；
  支持动态壁纸、萌宠主题、高德画中画。**B站有「21-23 款日产车机系统桌面改装」专视频**
  （[BV1Gb421Y7xz](https://www.bilibili.com/video/BV1Gb421Y7xz)）——日产平台装第三方桌面有公开先例。
- **迪友桌面**（v3.1.0）：比亚迪社区桌面，宠物主题；**布丁 UI**（budingui.com，主题中心+自定义壁纸）、
  **乐酷**（lecoauto.com，100+ 主题、昼夜双模、车速联动）、**氢桌面**、autoui.cn。
  ——全部是"内置主题/壁纸"模式，**没有一家支持用户导入 Live2D 角色模型**。
- 抖音「车机百变主题」专题、B站 Live2DViewerEX 桌宠教程（[BV1A24y1x7Bm](https://www.bilibili.com/video/BV1A24y1x7Bm)）、
  VPet、初音/洛天依模型配布文化——**角色模型供给生态已经存在**，缺的只是车机端承载工具。
- 日产平台线索（docs/NISSAN_TEANA_HEADUNIT.md 已录）：52pojie 帖 1759868 回帖推荐低资源
  AGAMA Car Launcher 换原厂桌面——第三方桌面在 NTDA3 上可行。

### 5.3 海外平台（用户指定必查）

- **X**：仅 MikuCarLauncher 作者 [@jlxc2001](https://x.com/jlxc2001/status/2067953491965051374) 的
  开发日记（文字喊话系统等），无西方社区做同类事。
- **YouTube**：车机生态内容全是通用主题/开机 Logo/启动器评测
  （如 [Finding the BEST Android Car Launcher in 2025](https://www.youtube.com/watch?v=_XvPJm-tfKM)、
  [26 in 1 Launchers](https://www.youtube.com/watch?v=tkBWu2wCFo4)、boot animation 教程），无 Live2D 角色桌面。
- **Reddit**：[r/Androidheadunits UI 替换讨论](https://www.reddit.com/r/Androidheadunits/comments/1f6gc4s/how_easy_is_it_to_customize_the_interface_for)
  等帖只谈启动器替换与开机动画，**没有 Live2D/角色桌面讨论帖**。
- **TikTok**：需求侧存在——[Miku 痛车贴膜](https://www.tiktok.com/discover/hatsune-miku-car-wrap)、
  [动漫语音包上车](https://www.tiktok.com/discover/how-to-install-anime-voice-on-car)、CarPlay 开机动画改
  Miku 风格——但工具侧空白。
- **判断**：海外是"有文化、有需求、无工具"，国内是"有工具、无角色 DIY"。Di-NissanPlay 做角色 DIY
  是两边社区的首发内容，且天然带传播素材（TikTok/YouTube 演示视频）。

---

## 6. 许可与合规

1. **MikuCarLauncher 代码**：三个备份仓库均无 LICENSE → 默认保留所有权利。用户已于 2026-10-27 说明
   **取得作者授权**；落地时在 `docs/THIRD_PARTY_NOTICES.md` 与新模块 README 记录授权来源与日期，
   并建议向作者要一段可引用的书面授权（X 私信即可）。
2. **Live2D Cubism Core**（`live2dcubismcore.min.js`）：专有许可，允许嵌入应用免费发布
   （[发布许可](https://www.live2d.com/en/sdk/license/)，年收入低于门槛免费，个人/爱好者项目适用），
   但**不得独立分发/不得提取**——Git 不入库，构建时下载或 CI 缓存。Framework 部分是
   Live2D Open Software License。
3. **pixi-live2d-display 0.4.0、PIXI 6.5.10**：MIT，可入库可分发。
4. **角色模型**：应用本身不分发任何模型（用户自行导入），规避模型版权问题；文档提示用户使用
   有授权/配布许可的模型。
5. 官方 Cubism Core CDN 链接不稳定（pixi-live2d-display README 提示"don't use it in production"），
   构建时下载需固定版本 + 校验和。

---

## 7. 性能与负担分析（NTDA3 预算）

车机：8 英寸 1280×720，Android 9/10（待实车 `wm size` / `getprop` 确认），约 2GB RAM，MTK 系 SoC，
DiPlay 同时要跑 CarPlay H.264 硬解 + 音频 + UI。

- **证据**：低端 Android 机在 WebView 中跑 Live2D 且主界面同时渲染，容易跌破 20fps
  （[CSDN Live2D 原生嵌入指南](https://blog.csdn.net)）；Android 官方明确 WebView 是完整 Chromium 引擎，
  内存大、生命周期不可控，要求按需创建/销毁、避免多实例
  （[developer.android.com WebView 内存指南](https://developer.android.com/topic/performance/memory/guide/webview-memory?hl=zh-cn)）；
  Live2D 内存大头是纹理，实测可从 13.6MB 压到 4.3MB（[UWA 案例](https://blog.uwa4d.com)）；
  美团 WebView 优化实践：独立进程/按需销毁/预热复用
  （[tech.meituan.com](https://tech.meituan.com/2017/06/09/webviewperf.html)）。
- **预算目标（Phase 1 验收线）**：角色层开启时新增 PSS ≤ 40MB；桌面冷启动 ≤ 2s；
  角色关闭时桌面无可感知掉帧；CarPlay 会话激活期间 WebView **销毁**（非暂停），解码零竞争；
  Live2D 默认 30fps / quality 1.0 / 关抗锯齿，用户可调 15–60fps / 0.5–2.0。
- **分级渲染**（负担控制的最终形态，见 §8-R3）：T0 静态图（任意角色 PNG，日夜各一张）→
  T1 Lottie（可选 Phase 2）→ T2 Live2D WebView。低 RAM 车机自动建议 T0。

---

## 8. 方案设计（三轮 loop）

### R1 草案——三条路线

- **A. 塞进 mobile**：CarPlay 未连接时在 DiPlay 界面显示角色待机页。优点：不新增应用；
  缺点：Compose 里嵌 WebView 与现有 UI 冲突大，DIY 空间小（没有"桌面"场景），违背"互补"初衷。
- **B. 新建日产桌面 `:nissanhome`**：以 samples/home 的纯 View 模式 + MikuCarLauncher 的装饰层架构，
  做一个可设为默认桌面的应用（带 1×卡片地图卡、导航小组件、应用抽屉、角色装饰层）。
  优点：DIY 空间最大、与 CarPlay 天然互补（桌面闲置时角色营业，CarPlay 启动时角色退场）；
  缺点：新增一个模块要维护。
- **C. 独立悬浮窗桌宠**：SYSTEM_ALERT_WINDOW 悬浮在任意界面。**被否决**：悬浮窗会与 DiPlay 地图卡、
  倒车/全景安全画面抢层——MikuCarLauncher 为高德悬浮窗写了十几版 showmap/closemap/安全阻断状态机
  （v72–v0.7.4.4）才压稳，前车之鉴；角色层只活在启动器 Activity 内部则天然安全（永远压不过系统
  倒车画面，也不会遮 CarPlay）。

**初选 B**。

### R2 约束校验（逐条打回/修正）

1. ~~复用 `:home` 嵌入式地图~~ → **minSdk 30 不兼容日产 Android 9/10**；改为 `:nissanhome` 独立模块
   （minSdk 24，与 mobile 对齐），地图走 **DiPlay 悬浮地图卡**（Android 9 可用）+ 导航小组件。
2. WebView 兼容：Android 9 WebView 支持 WebGL1，PIXI 6 可跑；但 **v58 教训**——禁用一切激进 WebGL
   参数，采用 v61/v62 稳态配置（quality/fps 用户可调，关抗锯齿默认）。
3. 内存互斥：CarPlay 会话激活时**销毁** WebView；检测方式 Phase 1 用 UsageStats 前台判断（与 DiPlay
   地图卡同模式，启动器自身声明即可，无需改 DiPlay），Phase 2 可加 DiPlay 会话广播（可选优化）。
4. ~~在 DiPlay 设置里加"角色形象"卡片~~ → **否决**：跨应用持久化无法满足 AGENTS.md
   "Overview 快捷设置必须同持久化"的约束；角色设置全部放启动器自己的设置页，DiPlay 侧零改动
   （Phase 1 不动 mobile/common，CI 风险最小化）。
5. AGENTS.md 适用性：若后续 DiPlay 侧出现联动设置，归 **Display** 类、标 ` (experimental)` 八语言
   后缀、`settings_` 前缀全语言翻译；UI 级开关**不**调 `markReconnectNeeded()`（不断 CarPlay）。
6. 许可：§6 四条全部落实后才动代码（Core 不入库 / 授权记录 / 锁定版本）。
7. 流程：按 docs/BRANCHING.md 的 SDD→BDD→TDD 切片，每片先补设计/行为规格再写测试；
   沿用 NISSAN_TEANA_PARSER_WORKLOG 的操作计数（每轮 ≥20 次）。
8. 车辆数据：**不移植** MikuCarLauncher 的 `VehicleDataProvider`（com.ts 私有协议）；日产侧车况
   后续走 DiPlay 自己的 `NissanHeadUnit` 闸门，Phase 1 不做车况卡。

### R3 定稿——`nissanhome` v1 架构

```
:nissanhome（新模块，Kotlin + 纯 View，minSdk 24，无 Compose）
├─ HomeCanvasLayer      自绘桌面 UI（参考 LauncherCanvasView 的卡片布局，Kotlin 重写）
├─ BackgroundLayer      日/夜壁纸（SAF 导入，URI 持久化）
├─ CharacterDecorLayer  角色装饰层（层级：背景之上、UI 之下；默认关闭）
│   ├─ T0 StaticCharacterView   静态角色 PNG（日夜各一，Phase 1 最小可用）
│   ├─ T1 LottieCharacterView   （Phase 2，可选依赖 com.airbnb.android:lottie）
│   └─ T2 Live2DWebView         WebView + PIXI6 + pixi-live2d-display 0.4.0 + Cubism Core(构建期下载)
│        · SAF 导入模型文件夹 → 内部目录副本 → 自动定位 model3.json
│        · 拖动/捏合调整（pointerMode=3 修复）· 画质 0.5–2.0 · 帧率 15–60 默认 30
│        · 心跳 watchdog（2s 心跳 / 5s 检查 / 软重载→重建）· 夜间 brightness 压暗
│        · 随机动作循环 6–12s + 点击切换 + 参数级兜底待机
│        · CarPlay 会话激活 → destroy()；回到桌面 → 重建（带 12s 防抖）
├─ AppsDrawer           应用抽屉（后台线程扫描 + 72dp 缩略图缓存 + PackageChangeReceiver 重建）
├─ MapIntegration       DiPlay 悬浮地图卡引导 + CarPlay 导航小组件承载
└─ Settings             我的 → 车机桌面设置（含预设备份导出/导入）
```

**切片计划**（SDD→BDD→TDD，每片独立可交付）：

- S1 模块骨架 + 背景层 + 设置持久化骨架（含 backup codec 单测）
- S2 T0 静态角色层 + 日夜切换 + 会话互斥守卫（UsageStats 判定的纯逻辑抽出来做单测）
- S3 模型导入管线（SAF 复制、model3.json 定位、路径解析——纯函数，重点单测）
- S4 T2 Live2D WebView（assets HTML + 运行库 Gradle 下载任务 + watchdog 状态机单测）
- S5 应用抽屉 + 缩略图缓存 + DiPlay 地图卡/导航小组件引导页
- S6（Phase 2）Lottie 档位、图标包、预设备份、天气卡（视用户优先级排队）

**验收标准**（真机 NTDA3，未验证项单独标注）：§7 预算线全绿；
`adb shell dumpsys meminfo` 前后对比记录进 worklog；compact/full + 大字号截图（改行对齐时补 RTL 截图）。

---

## 9. 风险与未决问题

1. **实车未验证**：NTDA3 的 Android 版本、RAM、WebView 版本均待 Phase 1 实车采集
   （沿用 docs/NISSAN_TEANA_HEADUNIT.md 的采集清单）；2GB RAM 车机上"CarPlay 解码 + WebView"
   互斥必须实测确认。
2. **授权范围**：用户口头授权已记录，书面留档建议尽快补；授权范围（复用 vs 参考重写）建议与作者
   明确——本方案按"架构参考 + Kotlin 重写为主，许可文件（HTML/Gradle 任务）可直接复用"推进，
   风险最低。
3. Cubism Core 构建期下载在国内网络环境可能不稳 → 允许本地手动放置（v50 已有该兜底）。
4. T1 Lottie 引入新依赖，需过 CI 与 lint；放 Phase 2 不阻塞主线。
5. 桌面形态与原厂桌面的共存：设置默认桌面 / 恢复原厂桌面的引导文案要写清（NTDA3 上
   `cmd package set-home-activity` 路径待实车验证）。

---

## 10. 参考来源

本地：MikuCarLauncher1.0/2.0/3.0 备份（README.md ×3、Live2DDecorView.java、live2d_decor.html、
MainActivity.java、LauncherCanvasView.java 等）；DiPlay：AGENTS.md、docs/BRANCHING.md、
docs/LAUNCHER_INTEGRATION.md、docs/NISSAN_TEANA_HEADUNIT.md、settings.gradle.kts、
samples/home/*、common 主题类、shared/media 解码器类。

外部：[Live2D SDK 许可](https://www.live2d.com/en/sdk/license/) ·
[Web SDK 下载页](https://www.live2d.com/en/sdk/download/web) ·
[pixi-live2d-display](https://github.com/guansss/pixi-live2d-display) ·
[openlauncher](https://github.com/dw2lam/openlauncher) ·
[femto-car-launcher](https://github.com/seijikohara/femto-car-launcher) ·
[Android WebView 内存指南](https://developer.android.com/topic/performance/memory/guide/webview-memory?hl=zh-cn) ·
[美团 WebView 优化](https://tech.meituan.com/2017/06/09/webviewperf.html) ·
[UWA Live2D 优化](https://blog.uwa4d.com) ·
[Lottie 6.1 多线程](https://gpeal.medium.com/lottie-android-6-1-lottie-goes-multithreaded-67c09c091fd7) ·
[MikuCarLauncher 发布页](https://m.7po.com/rj/14572.html) ·
[@jlxc2001 开发日记](https://x.com/jlxc2001/status/2067953491965051374) ·
[嘟嘟桌面日产改装视频 (B站)](https://www.bilibili.com/video/BV1Gb421Y7xz) ·
[Live2D 桌宠教程 (B站)](https://www.bilibili.com/video/BV1A24y1x7Bm) ·
[车机启动器评测 (YouTube)](https://www.youtube.com/watch?v=_XvPJm-tfKM) ·
[r/Androidheadunits](https://www.reddit.com/r/Androidheadunits/comments/1f6gc4s/how_easy_is_it_to_customize_the_interface_for) ·
[Miku 痛车 (TikTok)](https://www.tiktok.com/discover/hatsune-miku-car-wrap) ·
[动漫语音上车 (TikTok)](https://www.tiktok.com/discover/how-to-install-anime-voice-on-car)
