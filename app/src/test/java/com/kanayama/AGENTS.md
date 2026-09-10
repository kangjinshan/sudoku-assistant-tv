# 测试组织包导航

> 位置：`app/src/test/java/com/kanayama/`

当前唯一测试应用包为 `sudokuassistant/`。新增测试模块时在对应应用命名空间下组织，不在此层直接放测试类。


`GameProgressTest` 使用 Robolectric API 33 验证原生 View 的数独返回/重建、暂停计时、预选与填写恢复、成功清档，以及 24 点触摸合并后的自动选择、切换、待执行运算与菜单提示恢复、失败终局重置。`model/ProgressCodecTest` 验证全部宫格和难度的快照往返及损坏/完成快照拒绝。Robolectric 测试使用独立测试存储，不能替代设备截图验收。
