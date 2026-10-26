# 天籁适配方案（2021 款 2.0T）

目标是在中国市场 2021 款天籁 2.0T 的原厂超智联车机上运行本 fork 的 CarPlay 接收器，并在
后续阶段评估 MikuCarLauncher 风格的车机桌面。车机事实在第一阶段实车采集完成前都属于公开资料
假设，不能直接作为兼容性承诺。

## 架构边界

```text
天籁车机（Android 9/10，屏幕规格待实车确认）
├─ 车机桌面：导航小组件 / 地图卡片
└─ DiPlay：连接、协议、渲染和设置
       ▲ USB 或 Wi-Fi                         ▲ CarPlay 会话
       └─ 工作站通过 ADB 安装                 └─ iPhone
```

- DiPlay 保持主框架；日产专用行为只挂在 `NissanHeadUnit.detected` 后面，其他车机保持原有路径。
- 启动器属于独立职责。先复用已有导航小组件和地图卡片能力；Android 9/10 不纳入依赖 Android 11
  的嵌入式地图能力。

## 阶段

### 阶段 0：工作站准备

- [x] 已克隆 fork、添加 `upstream`，并建立 `develop`、`feat/*`、`hotfix` 分支。
- [x] 已提交日产识别闸门、研究文档和方案文档。
- [ ] 安装 JDK 与 Android SDK，运行 `shared` / `common` 单元测试、lint 和 debug 构建；工具链
  未就绪前，CI 是唯一可用的构建验证来源。

### 阶段 1：实车勘测

- [ ] 在工程菜单打开 ADB，连接 `adb connect <车机 IP>:5555`。
- [ ] 采集 `ro.product.*`、Android 版本、屏幕尺寸 / 密度并写入诊断报告。
- [ ] 确认 USB 口是否支持数据、Android 蓝牙栈是否能看到手机，以及车机固件实际版本。
- [ ] 先安装未修改的上游 APK，导出一份 DiPlay 诊断报告。

### 阶段 2：原版 DiPlay 验证

1. 先验证有线 USB，排除无线和蓝牙变量。
2. 再验证同一局域网、Wi-Fi Direct 和车机热点。
3. 根据实测屏幕尺寸选择显示预设，记录卡顿、帧率和音频结果。

### 阶段 3：日产专用适配

每项能力单独建立 `feat/*` 分支，并先补 BDD 场景和 TDD 测试：

- `feat/nissan-hotspot-guide`：在车机不允许应用直接控制热点时，提示驾驶员在原车设置中完成操作。
- `feat/nissan-wheel-keys`：只有确认方向盘按键绕过标准 `KEYCODE_MEDIA_*` 后才实现。
- `feat/nissan-autostart`：只有确认固件允许时才实现开机启动。
- 无线功能前先完成蓝牙归属验证。

### 阶段 4：车机桌面

- 先做适合 16:9、1280 × 720 的地图卡片和导航小组件。
- 不直接复用 A4L 的 32:9 素材；先验证布局、触摸和实体按键焦点。
- 是否新增 `:nissan-home` 模块，等桌面需求和实车结果明确后再决定。

### 阶段 5：收口

- `feat/*` 通过 CI 后合并到 `develop`，稳定后再合并 `main`。
- 发布标签和 APK 只从 `main` 产生；生产修复遵循 `hotfix/<issue>` 流程。

## 已知风险

- 实验性 CarPlay 配件身份不是 Apple 认证身份，未来 iOS 可能拒绝连接。
- 若原厂蓝牙模块独占电话或音乐，无线 CarPlay 可能无法完整工作；USB 是规避路径。
- Android 9 的 Wi-Fi Direct 有系统限制。
- 车机 USB 口可能只能充电，必须实车确认。
- 1280 × 720 目前只是公开资料，最终以 `adb shell wm size` 为准。
