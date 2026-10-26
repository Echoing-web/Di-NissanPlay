# 日产天籁适配调研纪要（2026-10-26）

两个并行调研代理的结论汇编（Android 命令输出格式 + 日产超智联社区数据），
全部主张附来源。证据分级：**实测**（来源含真实输出/日志）、**车主转述**、
**公开资料**。本文只陈述调研事实与行动项，不改变任何代码；待文档所有者
并入 `NISSAN_TEANA_HEADUNIT.md` 证据矩阵。

## 一、Android 命令输出格式（AOSP 8.1→14 逐版本验证）

来源：AOSP/LineageOS 镜像源码（`getprop.cpp`、`Wm.java`、
`WindowManagerShellCommand.java`、`DisplayManagerService.java`）+ Stack Overflow、
dev.to、longluo.me、juejin 等真实转录。

- `getprop` 全量输出恒为 `[key]: [value]`，按键名字典序排列；空值打印 `[]`。
  属性键字符集只允许字母数字与 `. - @ : _`（`init/util.cpp IsLegalPropertyName`），
  **键内不可能出现 `]: [`，因此值内包含 `]: [` 也不会产生歧义**。
- `getprop NAME` 只打印裸值（无括号），未设置时打印空行——**不要把单键 getprop
  输出喂给 `[k]: [v]` 解析器**（调用方规则，非解析器缺陷）。
- `wm size` 查询路径恒为 `Physical size: WxH`，仅当存在覆盖时追加
  `Override size: WxH`；`wm density` 同构。Android 9+ binder 失败时可能追加
  `Remote exception: <e>`；其余 `Error: bad size`、`Security exception: ...`、
  `wm: not found` 均为错误文本。Android 14 与 main 分支输出字符串逐字节相同。
- `dumpsys display` 首行恒为 `DISPLAY MANAGER (dumpsys display)`（8.1→14 验证），
  随后空行、缩进键值、`Display Adapters/Devices`、`Logical Displays`（13+ 挪入
  `LogicalDisplayMapper:` 并新增 `Display States` 段）。无任何 AOSP 组件会输出
  以 `wm size:` 或 `dumpsys display:` 开头的行——**解析器分区标记无碰撞风险**。
  权限被拒时正文首行为 `Permission Denial: can't dump DisplayManagerService ...`，
  应视为采集失败信号。
- pty 下的 `adb shell` 输出为 CRLF，Windows 粘贴常带杂散 `\r`；`adb exec-out` 可避免。

### 解析器交叉验证结论（NissanDiagnosticsTextParser）

| 假设 | 判定 |
| --- | --- |
| getprop 全量输出恒为 `[k]: [v]` | 成立（8.1→14 双实现验证） |
| wm 只输出 Physical/Override 行 + 可能错误文本 | 成立（错误行按噪声丢弃是正确行为） |
| `dumpsys display:` 行首标记无歧义 | 成立（AOSP 无碰撞输出） |
| 空值 `[]` → 缺失 | 成立（已有测试） |
| CRLF/杂散 `\r` 容忍 | 成立（lineSequence 已覆盖） |

新钉不变量：值内 `]: [` 不歧义（键字符集保证）——已补 TDD 测试
`属性值包含分隔符序列不产生歧义`。

## 二、对实车采集清单的修正要求（文档 bug，待所有者修）

`NISSAN_TEANA_HEADUNIT.md` §实车采集清单 前两条命令**按现状执行会失败**：

```
adb shell getprop ro.product.device ro.product.model ro.product.manufacturer ro.product.brand   # 坏
```

`getprop` 最多接受 2 个位置参数（Android 9/13 `toolbox/getprop.cpp`：
`Max 2 arguments`；Android 8.1 toybox 同限）。**修正方案**：逐属性查询，或直接用
全量 `getprop` 输出（格式稳定、字典序，正是解析器的输入契约）。
`wm size && wm density` 与 `dumpsys display | findstr -i displayId` 两条可用。

### 调用方设计规则（由调研导出，供接线切片使用）

1. 采集用全量 `getprop`；不要用单键形式。
2. 粘贴转录时给每段加分区标签行（`wm size:`、`wm density:`、`dumpsys display:`），
   解析器按显式标记采集（真实 dumpsys 首行是 `DISPLAY MANAGER (dumpsys display)`，
   不加标签就不会被采集——这是契约要求的行为，不是缺陷）。
3. `Permission Denial` / `Remote exception` / `Error:` 行是采集失败信号，
   调用方应提示重采，解析器则按噪声丢弃。

## 三、超智联社区证据矩阵更新（供并入 HEADUNIT.md）

- **NTDA3 世代实测（同平台他车，DiPlay 真实日志，公开链接）**：
  Android 4.4.3 / API 19，kernel 3.10.53，Freescale i.MX6（SABRESD），32 位，
  板号 `NTDA3_BOARD_S02`，固件 `NTDA3_P350`（19款轩逸 issue #298、2020 奇骏 T32
  README）。制造商为深圳航盛 HSAE（骐达 HS-N2165 / 轩逸 HS-N2166）。
  来源：github.com/programmerguohuajing/DiPlay-Legacy-Android issues #298/#134/#137、
  github.com/winkinwong/DiPlay-Legacy-Android-T32。
- **关键矛盾**：现有文档推测 NTDA3 是"Android 9-10 时代"，实测日志指向 4.4.3；
  同时存在第二代 HSAE 平台（P170/P660，Android 9 / API 28 / ARM64 / 4GB，
  kavvos 逍客实测），21款天籁 NTDA3_P330 与 KitKat 世代板号相同但社区有"64 位
  高德"矛盾报告——**目标车 getprop 仍是唯一仲裁**，现有 `wm size`/实车采集
  门控不变。
- **分辨率 1280x720 证据升级**：兄弟车型实测日志（`Display detected: 1280x720`）、
  kavvos 逍客实测（1280×720 横屏）、台湾 X-TRAIL 官方 1280×720。目标车仍未实测，
  门控保留；**未发现任何 800x1280 竖屏证据**。
- **蓝牙事实（NTDA3）**：无 Android 蓝牙适配器（`getDefaultAdapter()` null），
  蓝牙在 MCU 后（`com.anwsdk.service` AIDL `IAnwPhoneLink`）；无线 CarPlay 只能走
  该桥；USB CarPlay 前需处理 `com.hsae.ipod` 抢占 USB 接口。
- **ADB 进入**：设置→版本信息倒序长按三行→系统诊断→四角→USB TEST MODE（21款），
  公对公线；无线 `adb tcpip 5555`（znds 1245642，2021 天籁 2.0 舒适版实测）。
- **相关项目**：DiPlay-Legacy-Android-T32（NTDA3 实车可用先例，含 Conscrypt/
  iAP2 修复）、DiPlay-Legacy-Android issues #268/#79/#105/#230、kavvos.com 逆向
  分析（services.jar、ANWBT、IPv4 强制、白名单绕过）、headunit-version-inspector
  （识别伪造 Android 版本）。

## 四、后续行动

1. HEADUNIT.md 所有者修正 §实车采集清单 的 getprop 多参数命令（见第二节）。
2. 接线切片按第二节调用方规则设计采集与转录格式。
3. Kotlin 解析性能实践调研（原计划的第三路）留待下一切片执行。
4. 调研原始代理报告全文过长未随文档保存；本文已含全部可执行结论与来源 URL。

## 上游 #1119（0.2.21 无线热点连接回归）版本 diff 分析（2026-10-26 轮次 16，阶段性结论）

- 症状（上游评论日志分析）：BT RFCOMM/iAP2/MFi 全过、carplay-start-session 已发，
  iPhone 从未向车机建 TCP（tcpAccepted=0 → first TCP timeout）；0.2.20 可连、
  有线 USB 正常。报告者：BYD E2 DiLink 3.0 / Android 10 / iPhone 15 Pro。
- diff 范围：v0.2.20...v0.2.21 = 118 commits / 172 files。
- **已排除**（逐项核对 patch）：
  - `CarPlayVpnService.kt`（无线 TCP 的 VPN 转发层）零改动；
  - `WirelessStartupPolicy.kt` 仅新增 `WIFI_DIRECT_BESIDE_HOTSPOT` 失败枚举与
    retryable 收紧（Android 13+ Wi-Fi Direct 场景，报告者 Android 10 不涉及）；
  - `AirPlayConfig.kt` 仅显示角遮罩声明；`CarPlayController.kt` 为蓝牙适配器
    重取/有线看门狗/角遮罩（无 TCP/mDNS/监听变更）；
  - 权限类变更（ACCESS_LOCAL_NETWORK 文案、WirelessPermissions）——报告者
    协议栈已过权限阶段，不匹配症状。
- **剩余嫌疑**（未定论）：0.2.21 新增的 Wi-Fi 恢复路径（`wifiRecoveryButton`、
  `WirelessStartupRecoveryTest` +79 行、CarPlayHostActivity 大改中该区域）——
  若恢复逻辑在连接尝试间错误重置网络/套接字状态，可解释「重试多次后放弃、
  0.2.20 不受影响」。需要带 iPhone 环境复现或上游作者定位；本仓库（main=0.2.21
  后）同样携带该嫌疑行为。
- 本地可测性：协议交互需 iPhone 真机；模拟器/Robolectric 只能覆盖策略单测
  （上游已覆盖）。后续切片：跟踪上游进展；若上游修复，按 #1084 模式交叉验证。

## 上游 #1108（NXP i.MX8 帧率坍塌）对日产移植的阶段 2 风险预警（2026-10-26 轮次 18）

- 现象：i.MX8 / Android 8.1 / OMX.Freescale 硬解，静止 25–33fps，画面一变化
  即跌至个位数；同机第三方 MediaCodec 应用可放 1920x720@60。
- 上游贡献者计数器分析：**码率在所有 5 秒窗口恒 ≤835kbps**，帧率坍塌时单帧
  变大——指向 iPhone 侧码率协商/带宽瓶颈模式，而非解码器能力主因。
- 日产关联（基于本文档既有的 NTDA3 = i.MX6 / Android 4.4.3 与同代 Android 9
  ARM64 双平台结论）：i.MX6 弱于 i.MX8，若目标车头单为该世代，**视频通路
  （阶段 2）的码率协商与带宽是首要实测项**；诊断链路（阶段 1）不受影响。
- 移植对策线索：DiPlay 已有的 Advanced 设置（HEVC、帧率、分辨率）是该场景的
  调节手段；实车勘测时建议在采集清单之外额外记录 `dumpsys media.codec`
  摘要与实测带宽（不进运行时默认值，仅勘测证据）。

## 上游 #1116（热点连接后绿屏）台账（轮次 18，简记）

- 副厂机 / Android SDK 27 / 热点无线；握手与音频正常、iPhone 截图正常、车机
  渲染层全绿——典型解码初始化/像素格式问题。0 条上游评论，无分析线索；
  与日产场景关联弱，仅记台账跟踪。

## #1119 嫌疑升级：上游 open PR #1071 即对症修复（2026-10-26 轮次 19 深挖）

- 深挖方法：精读 CarPlayHostActivity 920 行 patch 中 recovery/重连区域 + 上游
  open PR 扫描。
- **首要嫌疑（上游已确认的缺陷）**：open PR **#1071 "Fix wireless CarPlay
  StartSession IP address payload"**（faizannazir，2026-10-21，未合并）——
  iAP2 0x4301 StartSession 载荷只含部分接口地址，iPhone 可能拿不到有效地址；
  修复 = 包含全部热点接口主机地址（IPv4 + link-local IPv6）。与 #1119 症状链
  精确闭环：MFi 过 → StartSession 发出（载荷 IP 不全）→ iPhone 无地址可连 →
  tcpAccepted=0 → first TCP timeout → 重试同理失败 → 放弃。**PR 的存在即上游
  对该缺陷的确认**；为何 0.2.20 可连待上游解释（疑似 0.2.21 接口选择变化）。
- 次要候选（降级）：0.2.21 新增的重连跳过条件
  （`controlRunningCount != controlRunsAtFailure && !lossSinceControlRunning`
  → 不重连，注释引 #873 有线场景但条件不分有线/无线；无线 BT 控制通道持续
  存活时可误跳过重连）——影响重试行为，可能叠加症状。
- **本地交叉验证（轮次 19，按 #1084 模式）**：#1071 三文件 patch 干净应用于
  hotfix worktree（本地 main 基线，零冲突）；PR 自带测试
  `Iap2WirelessControlClientTest` / `WirelessStartupControllerTest` 运行结果
  见 worklog 轮次 19。
- 对本仓库的意义：日产线（分支基于同一 main）同样携带该载荷缺陷；待上游
  合并 #1071 后按 #1084 模式同步交叉验证即可，无需独立修复。

## #1071 技术细节与日产阶段 2 预警（2026-10-26 轮次 21 补充）

- 修复前形态：`ipAddresses = listOf(hostAddressText)`——StartSession（iAP2
  参数 id=3）只携带**单个**地址；双栈接口（IPv4 + link-local IPv6）上若选中的
  地址对 iPhone 不可达（选错栈），TCP 永远建立不了，即 #1119 的 tcpAccepted=0。
- 修复后形态：主地址 + `hotspotInfo.hostAddresses` 全量（去重），测试
  `startSessionEncodesMultipleIpAddresses` 钉住双栈端点编码出两个 IP 参数、
  `endpointAddressesIncludeAllHostAddresses` 钉住地址文本（IPv6 去 `%scope`）。
- 日产阶段 2 预警：天籁超智联车载热点场景同理会遇到接口双栈选择问题——上游
  合并 #1071 后日产线自动受益（同基线）；实车勘测时若走无线路径，采集清单
  可补记 `ip addr show` 摘要（仅勘测证据）。
- 上游 release 节奏（轮次 21 核查）：最新 v0.2.21（2026-10-17 pre-release），
  0.2.22 未发布——#1085（已合并）与 #1071（open）均未进 release；上游 2–3 天
  一个 pre-release，0.2.22 临近。下次 loop 检查点：0.2.22 发布时核对包含
  关系（#1085 应含；#1071 视合并状态）。

# 日产车机蓝牙连接问题调研（2026-10-26 轮次 23，用户指令：版本更新 + README + 技术论坛三路）

## 结论（TL;DR）

用户报告的「日产车机蓝牙连接问题」根因已定案：**日产超智联家族（逍客/轩逸/奇骏/
天籁）的 HSAE 航盛 NTDA3 车机使用私有蓝牙栈（出厂电话/音乐应用经 MCU 蓝牙，
com.anwsdk.service / com.hsae.bluetoothservice 厂商服务），Android 系统级蓝牙
不可用**，而 DiPlay 无线 CarPlay 依赖 Android 原生 BT 做 iAP2 发现/认证——
原生路径在这类车机上必然失败。**社区已有实机验证的解决方案**（AnwPhoneLink
厂商蓝牙栈桥接），上游作者已表态欢迎隔离实现的 PR。

## 三路证据

1. **上游 issue 族**（gh 调研）：
   - #703（日产轩逸，官方定性）：后装车机蓝牙常是独立模块，状态栏有图标但
     Android 系统蓝牙不可用，其他 app 用不了 → 无线连接无法使用。
   - #880（HSAE CN202SR/五菱，i.MX6）：出厂蓝牙正常但 Android BT off；
     贡献者用出厂 Anw 栈实现**固件专用蓝牙传输后端**（v0.2.20 基础，无线
     CarPlay + 重启重连实车验证通过）；含完整工程 outline（Binder 事务表
     3/7/16/222/224/225-228/231-233、iAP2 UUID 字节序、RFCOMM 参数、异步
     连接判定、BlockingDuplexByteStream 适配、上游 iAP2/认证/AirPlay 全复用、
     未验证清单）。**上游作者准入条件**：仅当原生 BT off/缺失 且
     com.anwsdk.service 存在时启用、隔离实现、欢迎 PR。
   - #581（Android 9 MTK 原生栈不可用）、#764（OEM BT 可用 RFCOMM 失败）、
     #918（双芯片睡眠关 BT）——同族问题。
2. **社区 fork**（技术论坛路 → WebSearch）：**DiPlay-Legacy-Android**
   （programmerguohuajing，上游深度兼容 fork，Android 4.4–6.0 + 老硬件，
   0.2.96+）scope 明确含「Hangsheng HSAE for Dongfeng Nissan 私有蓝牙栈」；
   其 #79（19 款逍客，29 评论）完整记录：原生无线失败（No Bluetooth
   adapter / iAP2 无响应）→ 353845354 代码级方案（resolveWireless
   BluetoothTarget 原生优先 → MCU 回落，字节流适配为原生等价接口）→
   作者认可并记录为优化方向 → **winkinwong/DiPlay-Legacy-Android-T32**
   （基于上游 v0.2.46 定制）实现 **AnwPhoneLink 桥接**，19 款逍客（奇骏 T32
   同款 NTDA3）实机验证**无线+有线均可**（v0.2.46-t32.3，独立签名）。
3. **本仓库既有调研交叉印证**：轮次 4 已发现 NTDA3 无 Android 蓝牙适配器
   （BT 在 MCU 后，HSAE 后装）——与上述完全一致。

## 对 2021 天籁适配的含义

- 天籁超智联与逍客/奇骏同属 NTDA3 家族（待实车确认变体）；**T32 fork 的
  AnwPhoneLink 桥接大概率适用**。
- 现实路径分三档：①短期——实车试装 T32 定制版（v0.2.46-t32.3，注意独立
  签名与上游包不共存）；②中期——本仓库（上游主线 + 日产诊断链路）按 #880
  的准入条件与 T32 实现做隔离桥接（阶段 2.5）；③长期——上游合并社区 PR
  后同基线受益。
- 无线 CarPlay 硬约束（Legacy #79 作者原话）：iAP2 发现/配对必须走蓝牙，
  这是苹果协议要求——无可用蓝牙路径时唯一选项是有线 USB。

## 生产流程落位（SDD/BDD 规划 + loop 排队）

- **SDD 规划节**（AnwPhoneLink 桥接设计，待实现）：检测门（原生 adapter
  off/缺失 + AnwPhoneLink 服务可达）、适配层（厂商 AIDL 字节流 ↔ 上游
  BlockingDuplexByteStream 等价接口，上层 iAP2 无感知）、ABI 防御（事务号
  按机型校验，包名存在≠ABI 兼容——#880 明示）、失败回落（桥接失败明确报错
  指导有线，不静默）。
- **BDD 场景**（规划）：原生 BT 可用→原生路径不变；原生不可用+Anw 服务在→
  桥接路径；两者皆无→明确报错建议有线；桥接中途失败→清理 socket/回调并报错。
- **Loop 排队首位（轮次 24 候选）**：拉取 winkinwong/DiPlay-Legacy-Android-T32
  仓库的 Anw 桥接实现做代码级分析（gh api 通道），与 #880 工程 outline 对勘，
  产出本仓库的移植规格草案。

来源：上游 #703/#880/#581/#764/#918（github.com/shihabal3amri/DiPlay）；
Legacy README/COMPATIBILITY/issue#79（github.com/programmerguohuajing/
DiPlay-Legacy-Android）；T32 fork（github.com/winkinwong/DiPlay-Legacy-
Android-T32 releases v0.2.46-t32.3）；7Car 日产手册 PDF（蓝牙 V4.2）。

## T32 Anw 桥接实现分析与移植规格草案（2026-10-26 轮次 24，代码级对勘）

- 源：winkinwong/DiPlay-Legacy-Android-T32 @ main（1233 文件；基于上游 DiPlay，
  10-21 最后推送；19 款逍客/NTDA3 实机验证 v0.2.46-t32.3）。
- **结构**（四层）：①AIDL 接口定义 `com.anwsdk.service`（IAnwPhoneLink /
  IAnwSPPDataCallBack / IAnwSocketDataCallBack）+ 第二厂商 `com.hsae.
  bluetoothsdk`（IBluetoothManager/IBluetoothCallback）；②适配层
  `com.shilapi.xcertplay.bluetooth`：AnwPhoneLinkBluetoothAdapter（实现上游
  `CarPlayBluetooth` 接口）、AnwPhoneLinkDiscovery（厂商配对列表）、
  HsaeBluetoothAdapter、HsaeRfcommStream（实现上游 `BlockingDuplexByteStream`）；
  ③接入点：新增 `CarPlayBluetoothManager.get(context)` 返回混合适配器（原生
  优先→厂商回落），CarPlayController 四处改经它取 adapter +
  `selectVendorWirelessTarget(hybrid, savedAddr)`（手动地址优先→已连接→唯一
  配对 iPhone；多 iPhone 抛错镜像原生路径）；④TDD 测试
  AnwPhoneLinkBluetoothAdapterTest + AnwPhoneLinkDiscoveryTest。
- **检测门**：`queryIntentServices(Intent("com.anwsdk.service.AnwPhoneLink")
  .setPackage("com.anwsdk.service"))` 非空——服务 action 解析（比 #880 的
  binder descriptor 探测轻量，比裸包名存在严谨）。
- **与 #880 对勘**（两个独立实现互证）：
  - 字节流：双实现同构（有界 FIFO 部分读保序、4KB 分块写、written 计数、
    RemoteException→IOException 故障传播、close 反注册回调+断开+去初始化）——
    证明该适配边界稳定。
  - **T32 优于 #880 之处**：用 AIDL 命名方法（ANWBT_SocketWrite/SPPWrite/
    Registry/UnRegistry…）而非裸 transact 事务号——binder stub 自带接口版本
    容错；支持 Socket 与 SPP 双模式。
  - **#880 有而 T32 未见（移植时应并入）**：连接成功的异步确认（轮询
    connection info 校验目标地址与 connected 状态，防误连）；配对列表
    NAMES-first/ADDRESSES-second 的解析顺序陷阱。
  - 诚实边界一致：不能开蓝牙/发现新手机（固件电源与配对由车机管理）、
    isEnabled=isSupported、本地 MAC 从 Settings.Secure/Global
    `bluetooth_address`（拒 02:00 匿名与全零）。
- **移植规格草案（本仓库阶段 2.5）**：
  1. 检测门 = T32 的服务 action 解析 + #880 的连接异步确认补强；
  2. 适配层 = `CarPlayBluetooth` 替身 + `BlockingDuplexByteStream`（上游两个
     既有接口，零上游协议改动）；AIDL 定义文件随移植（`com.anwsdk.service`
     命名空间）；
  3. 接入 = `CarPlayBluetoothManager` 混合门（原生优先），`CarPlayController`
     改四行取 adapter + 目标选择函数；
  4. TDD 先行 = 四个 BDD 规划场景的用例（检测门三分支/字节流等价/清理回落/
     多 iPhone 镜像行为）全部纯 JVM 桩 AIDL；
  5. ABI 防御与失败回落按 SDD 既有条目；实车前不声称年款支持。

## 真机拆解样本数据集与样本模拟测试（2026-10-26 轮次 30，用户指令）

### 拆解实测样本（来源附注）

- 屏幕：9 英寸 1280x720 电容屏、双 USB、支持 CarPlay（知乎《最新日产车机拆解》
  zhuanlan.zhihu.com/p/581451953，2022-11，歌乐/Clarion 制造美国版；腾讯新闻
  座舱拆解同源）。兄弟车型实测背书 1280x720（轮次 4 既有调研）。
- 主控：NXP i.MX6（SABRESD-MX6DQ 板，DiPlay-Legacy-Android 实车日志与 T32
  CN202SR 同板互证）。**勘误**：搜索提示 NTDA3 更可能是大陆/诺威达系屏驱或
  解码芯片代号而非主控 SoC——此前文档按社区习惯把 NTDA3 当平台名，语义修正
  为「NTDA3 世代（屏驱代号）= i.MX6 主控家族」。
- 平台双世代：Android 4.4.3 / API 19（i.MX6 实车日志）与 Android 9 / API 28
  ARM64（同代第二平台社区实测，轮次 4）。

### 样本模拟测试交付（轮次 30）

1. **TDD 样本用例**（`NissanTeanaRealWorldSampleTest`，3/3 Green）：4.4.3 世代
   全字段样本（SABRESD 形态 + 1280x720/density 160 + dumpsys 摘要，端到端
   解析→15 行报告）；Android 9 双平台样本（Physical+Override 双行 wm，多行
   值按字面 `\n` 转义渲染；缺失 dumpsys 渲染 unknown）；MCU 蓝牙形态样本
   （系统字段完整呈现，诊断不依赖蓝牙状态）。**红线重申**：样本值仅存于
   测试，运行时代码零回填。
   测试修正记录：ro.product.board 不在七属性集（首次断言误含，删）；
   多行值渲染为字面 `\n` 转义非空格连接（对齐 Report.render 实现）。
2. **模拟器真机规格模拟**：复刻 1280x720 + density 160 + NISSAN manufacturer
   → 最新 APK（含轮次 25–29 全部代码）Diagnostics 页验证——日产诊断按钮
   bounds=[320,531][1224,591]，高 60px 与卡内全部按钮一致、x 对齐一致，
   低密度横屏真机规格下布局合规。截图
   `docs/screenshots/nissan_realspec_1280x720_d160.png`。
3. 环境备注：同 AVD 双实例会 FATAL（需 -read-only）——复用存活实例即可。

## T32/Legacy 新版修复集对勘（2026-10-26 轮次 34，并行代理 C，gh api 只读）

来源：T32 v0.2.46-t32.3→t32.4（compare + commits a4b1a52/d86b768/23e920f/2146eef/b4cd238）
与 Legacy v0.2.95→v0.2.96（commits 4ae8fff/0a07010）。

### 日产直接相关修复（对本仓库阶段 2 的含义）

- **AirPlay keepalive**（T32 t32.4，三轮实车验证）：请求应答回显 + InfoPlist 声明
  `keepAliveLowPower`，连接初期卡顿显著减少。本仓库 `AirPlaySession.kt:683` 已有
  应答机制但 `AirPlayInfoPlist.kt:47` 仍声明 false——**一行级高价值改动**。
- **视频停顿自恢复**（t32.4）：1s soTimeout 断点续读 + ≥2s 限速上报 +
  forceKeyFrame 催帧，**删除 10s 强拆**（实测 5.5–17.7s 停顿自愈，强拆反致 20s
  重建）；i.MX6 720p30 decode p50=55ms 天花板。本仓库 ScreenStream 无此机制。
- **无线韧性**（t32.4）：首-TCP 看门狗 30s→60s（实测建连 20.7–30.7s）；iAP2
  隧道重连 15s→3s（重连中位 29–38s→约 9s）；mDNS 端口绑定竞态改伴生对象级。
  本仓库 `WirelessStartupPolicy.kt:10` 仍 30_000L、Bonjour 仍实例级。
- **导航 duck 指令链**（t32.4）与 **changeModes MainAudio 回收**（Legacy #312：
  changeModes 扩展 + takeMainAudio + 三触发点回收）：本仓库**完全没有
  changeModes 协议能力**（grep 0 命中）——R4① 切音源无声类的协议级缺口。
- **任务划掉误拆**（t32.4 TaskRemovalPolicy）：本仓库
  `DiPlaySessionService.kt:57-61` onTaskRemoved 无条件 stop——日产机划掉即断。
- **悬浮小白点入口**（t32.4，移植上游 #119）：直接缓解 PRD 断点 #4（桌面不可见）。
- **倒车非破坏性恢复**（Legacy 0a07010：allowRebuild=false 只重置参考链索 IDR
  不注销解码器 + 3s 防抖 + 退挡广播集）：任何代际通用的倒车冲突解法。
- 无线链路健康监控（网卡计数 + /proc/net/wireless 电平二分「手机停发 vs
  空口干扰」）——阶段 2 无线勘测方法论可借鉴。

### 主线缺口清单（危险度序，阶段 2 排队输入）

1. changeModes/Take MainAudio 协议能力（音频矩阵 BDD 前置）；2. 音轨急切创建
（Siri 静默流，T32 1bd1d44，主线未回移）；3. 无线韧性四件套（keepalive 声明
是唯一一行级）；4. onTaskRemoved 按厂商保活；5. 倒车非破坏性恢复；（条件）
外置 USB BT 12s 静默让位。

### Anw 桥接对勘结论

t32.4 未触碰 bluetooth 包——桥接全部随 t32.3（实车验证「无线可用、略优于
USB」）。两仓库接口面平行（selectVendorWirelessTarget 两侧同名）；**无需从
t32.3 同步代码**；T32 的 HybridBluetoothAdapter 三级链（HSAE→Anw→框架）
留档——若天籁实机出现「框架适配器在场但残缺」中间态，级联回退是现成答案。
阶段 2 无线首测对照基线直接引用 t32.4 实测数字。
