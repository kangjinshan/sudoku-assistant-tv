# 24 点 imagegen 素材

> 生成日期：2026-10-21

- 服务：Azure 0932，部署 `gpt-image-2`，`high`，1024×1536；通过个人技能 `~/.codex/skills/imagegen/scripts/image_gen.py generate` 生成。
- `twenty-four-workshop-prompt.txt`：本次实际发送的完整提示词，使用 `--no-augment`，没有参考图片。
- `twenty-four-workshop.png`：模型原始输出；森林绿背景、纸艺小狐狸和四则运算积木。属于 AI 生成插画。
- 应用素材：`../../app/src/main/res/drawable-nodpi/twenty_four_workshop.webp`，512×1373，质量 90。原图顶部 1024×460 的空白背景放大到 1024×2746 并做半径 10 的高斯模糊；原图在 y=652 处叠加，上下 100/120 像素羽化；最后按比例缩小并导出 WebP。延展背景用于容纳原生标题及底部文案。
- 页面引用由 `TwentyFourRenderer` 实现，所有动态文字、数字与交互卡片使用 Canvas；不能把整张概念界面当成可操作 UI。
- 截图验收保存在 `../../captures/`，与模型原图区分。复现命令不写入或输出 API 密钥。

```bash
python3 ~/.codex/skills/imagegen/scripts/image_gen.py generate --prompt-file output/imagegen/twenty-four-workshop-prompt.txt --no-augment --size 1024x1536 --quality high --out output/imagegen/twenty-four-workshop.png
```

复现时使用新输出文件名，保留已有原图。
