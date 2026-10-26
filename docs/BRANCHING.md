# 分支与开发门槛

本 fork 在上游 DiPlay（`shihabal3amri/DiPlay`）之上承载中国市场 2021 款天籁 2.0T 适配。
分支采用轻量 Git Flow，并保持可回溯的上游同步关系：

| 分支 | 基于 | 用途 | 合并到 |
| --- | --- | --- | --- |
| `main` | 上游 `main` | 上游镜像与已验收的日产适配合并线；发布 APK 只从此分支打标签 | — |
| `develop` | `main` | 日产适配集成线；上游同步先在此验证，再进入 `main` | `main` |
| `feat/*` | `develop` | 单一行为或功能一条分支，例如 `feat/nissan-wheel-keys` | `develop` |
| `hotfix` | `main` | 长期修复基线；只有生产修复才从此线切出 `hotfix/<issue>` | `main` 与 `develop` |

## 命名约定

- `feat/*` 使用 kebab-case；能表达范围时以领域开头，例如 `feat/nissan-wheel-keys`。
- `hotfix/<issue>` 只在确有线上修复时创建，完成后合并回 `main` 与 `develop`，再删除临时分支。
- 提交主题沿用上游祈使句风格；车机专用改动必须在正文记录实测行为或明确的未验证项。

## 上游同步

```bash
git fetch upstream
git switch main && git merge --ff-only upstream/main && git push origin main
git switch develop && git merge main && git push origin develop
```

冲突在 `develop` 解决，不重写 `main` 的上游历史。

## 车机适配规则

- 每个车机专用行为都必须经过车机识别闸门（例如 `GeelyHeadUnit`、`NissanHeadUnit` 或
  DiLink 代际检测），未验证的车机保持原有行为。
- 新增车机专用代码必须同时提供锁定闸门的单元测试（参考 `NissanHeadUnitTest`）。
- 文档只陈述已测量的事实；未验证行为必须明确标记。

## TDD、SDD、BDD 工作循环

每个适配切片按同一顺序推进：

1. **SDD**：先写目标、输入、输出、非目标和验收证据，禁止把猜测写成实车事实。
2. **BDD**：把驾驶员可观察的行为写成 Given / When / Then 场景，只覆盖本切片。
3. **TDD**：先让测试表达场景并失败，再写最小实现，最后运行定向测试。
4. 通过后才允许接入下一个车机能力；未完成实车验证的能力不得进入发布分支。
