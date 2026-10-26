本项目基于 [DiPlay](https://github.com/shihabal3amri/DiPlay) 进行开发。

# Di-NissanPlay — 2021 款日产天籁 CarPlay 适配

Di-NissanPlay 是 [DiPlay](https://github.com/shihabal3amri/DiPlay) 的衍生项目。DiPlay 是把 iPhone 的 CarPlay 画面和声音投到安卓车机上的开源方案,原本主要针对比亚迪 DiLink 车机;本 fork 在**完整保留 DiPlay 全部功能**(基线上游 0.2.21)的基础上,针对**2021 款日产天籁 2.0T 的超智联车机**(中国市场第七代 Altima,2019–2023 同族车机思路通用)做了适配开发。

上游原版 README:[English](docs/UPSTREAM_README.md) · [简体中文](README.zh-CN.md) · [繁體中文](README.zh-TW.md)

## 我们开发了什么

### 1. 日产车机自动识别
通过系统标识(`ro.product.*` 等)自动判断当前车机是不是日产超智联。
识别成功才显示日产专属功能;在其他任何设备上,本项目和原版 DiPlay 表现完全一致。

### 2. 一键诊断(给车主和开发者排障用)
- 位置:**设置 → 诊断 → 「保存诊断报告」卡片 → 日产诊断按钮**(仅日产车机可见)
- 点一下,应用自动执行 4 条**只读**命令:`getprop`、`wm size`、`wm density`、`dumpsys display`
- 输出一份 15 行的诊断报告,**长按即可复制**,粘贴给开发者即可定位车机型号和屏幕参数
- 安全约定:命令白名单之外的任何命令(包括拼接变体)一律不执行;每条 10 秒超时;报告不含协议数据、凭据或个人信息

### 3. AnwPhoneLink 蓝牙桥接(无线 CarPlay 的关键)
天籁超智联车机的蓝牙硬件挂在 MCU 后面,Android 系统里根本看不到蓝牙适配器,
所以原版 DiPlay 的无线 CarPlay 在这台车上**必然失败**。本 fork 增加"原生优先 → 厂商回落"策略:

- 系统蓝牙正常时:行为与上游 DiPlay 完全一致,一行代码不多走
- 系统蓝牙不可用但车机自带 AnwPhoneLink 厂商服务时:自动改走厂商桥接,无线 CarPlay 得以继续

### 4. 继承 DiPlay 的全部既有能力
有线/无线 CarPlay、音乐与导航音频、方向盘按键、仪表投显、诊断报告导出、多语言设置等,全部来自上游 0.2.21,没有裁剪。

## 使用场景

| 场景 | 说明 |
| --- | --- |
| 有线 CarPlay | iPhone 数据线插车机 USB 口,即插即用 |
| 无线 CarPlay | 蓝牙桥接 + Wi-Fi Direct,上车自动连接(天籁必须走桥接) |
| 车机排障 | 一键诊断生成报告,发给开发者,不用口头描述车机型号 |

## 操作步骤(Steps)

**Step 1 — 把 APK 装进车机**
用 U 盘拷贝安装,或者电脑连车机后执行 `adb install Di-NissanPlay.apk`。

**Step 2 — 打开应用**
安装后如果桌面看不到图标,不是安装失败,是原厂桌面只显示白名单应用。
用 ADB 直接启动即可:
`adb shell am start -n com.shihab.diplay.hudtest/com.shilapi.xcertplay.DiPlayActivity`
(也可以安装第三方桌面作为日常入口,社区有车主这样用)

**Step 3 — 首次授权**
按应用内引导,把 USB、附近设备、位置等权限给齐。

**Step 4 — 连接 iPhone**
- 有线:iPhone 插线,点连接,等 CarPlay 画面出现即可
- 无线:确认 iPhone 的蓝牙和 Wi-Fi 都开着,按引导走无线连接;车机侧蓝牙由 Anw 桥接自动接管,你不需要额外操作

**Step 5 — 出问题了?一键诊断**
设置 → 诊断 → 点日产诊断按钮:
- 弹出报告 → 长按复制,发给开发者
- 提示"重新采集" → 按提示用 ADB 重新采集(部分车机会拒绝应用内执行 `wm`/`dumpsys`,这是预期行为,详见 [诊断使用指南](docs/NISSAN_DIAGNOSTICS_USER_GUIDE.md))
- 提示拒绝 → 说明本机识别不是日产车机,确认一下车型

## 当前状态(诚实声明)

- 功能已实现:shared 模块 1786 个单元测试 0 失败,common 模块除 4 个 Windows 环境性失败外全绿;模拟器(Android 15,注入 NISSAN 标识)端到端验证通过
- **实车验证尚未完成**:无线桥接的实机路径、真实 `ro.product.*` 值,都在等实车 ADB 转录
- 本项目与 Apple 无关、未获 MFi 认证,沿用上游相同的配件身份方案与声明

## 分支说明

| 分支 | 用途 |
| --- | --- |
| `main` | 上游镜像 + 已验收的日产适配合并;发版 APK 从这里打 tag |
| `develop` | 日产适配集成线;上游同步先落这里,验证后再进 `main` |
| `feat/nissan-teana-adaptation` | 日产天籁适配主开发线(当前全部成果在此) |
| `hotfix` / `hotfix-*` | 针对上游问题的紧急修复(例如 #1071 无线连接修复) |

分支与合并规则详见 [docs/BRANCHING.md](docs/BRANCHING.md)。

## 版本记录

见 [CHANGELOG.md](CHANGELOG.md):顶部是本 fork 的日产版本条目,往下是上游 0.2.9–0.2.21 的完整变更与修复记录。

## 构建与安装

- 构建:[docs/BUILD.md](docs/BUILD.md)(需要 Android SDK + JDK 21,`./gradlew :mobile:assembleDebug`)
- 安装与连接:[docs/INSTALL.md](docs/INSTALL.md)
- 日产适配设计与验收材料:[docs/NISSAN_PORT_PLAN.md](docs/NISSAN_PORT_PLAN.md)、[docs/NISSAN_TEANA_ACCEPTANCE.md](docs/NISSAN_TEANA_ACCEPTANCE.md)

## 许可

本项目沿用上游 [DiPlay](https://github.com/shihabal3amri/DiPlay) 的 [GPL-3.0](LICENSE) 许可证。
