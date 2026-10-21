# QA 截图目录指南

> 位置：`captures/`

本目录只存放本地或真机 QA 截图，不属于应用运行时资源。截图默认被 `.gitignore` 忽略，仅本说明入库。生成截图时使用明确设备序列；不得提交含用户信息的画面。正式 UI 资源位于 `app/src/main/res/`，不要从本目录运行时加载。

## 2026-10-21：24 点工坊主题

- 设备：本机 `AndroidTV` ARM64 / API 33 模拟器，明确序列 `emulator-5556`；覆盖安装 Release，并执行 speed 编译。没有连接真机。
- `twenty-four-before.png`：升级前的存档局面；`twenty-four-workshop-resumed.png` 验证升级后同一原题、负数结果、选择与焦点正确恢复。
- `twenty-four-workshop-ready.png` / `twenty-four-workshop-final.png`：1920×1080 的可操作页面预览。
- `twenty-four-workshop-reset-focus.png`：底部按钮的遥控器焦点。
- `twenty-four-workshop-first-merge.png`、`operation.png`、`hint.png`、`second-merge.png`、`solved.png`（均有 `twenty-four-workshop-` 前缀）：全 DPAD 完成 `3 + 5 = 8`、`9 ÷ 3 = 3`、`3 × 8 = 24`；检查源数字消失、结果落点、自动选择、菜单提示不改变待执行操作，以及玩家三步记录。
- `twenty-four-workshop-invalid-division.png` / `not-24.png`（同前缀）：触摸验证非整除拒绝、失败终局和重置。
- `twenty-four-workshop-tablet-4x3.png`：同一模拟器临时设为 1600×1200，以实际触摸完成一步合并，验证等比居中与命中；之后已恢复物理 1920×1080。
- 两张 `*-qa*.jpg` 为审阅拼图；单张 PNG 才是原始模拟器截图。
- 构建验收：27 项 JVM 测试通过；`lintRelease` 无错误（16 项现有警告均不在新增 Renderer）；`assembleRelease` 成功；本轮 crash buffer 为空。

