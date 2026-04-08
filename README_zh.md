# World Preview NH

**[English](README.md)**

一个 Minecraft 1.7.10 模组，用于在创建世界前预览生物群系。本模组是 [World Preview](https://github.com/caeruleusDraconis/world-preview) 到 1.7.10 的移植实现，面向 [GTNH](https://github.com/GTNewHorizons) 模组包生态，移除了结构预览功能。

## 功能特性

- **生物群系地图预览** — 根据种子在创建世界前生成全屏交互式生物群系地图
- **高度图视图** — 切换彩色高度图叠加显示
- **Y轴交点视图** — 查看特定 Y 层的方块分布
- **洞穴模式** — 切换洞穴生物群系显示
- **缩放与平移** — 滚轮缩放（1–2048 方块/像素），拖拽平移地图
- **坐标跳转** — 跳转到指定 X/Z 坐标
- **群系搜索** — 按名称过滤和高亮生物群系
- **种子库** — 保存和加载最多 50 个常用种子
- **游戏内预览** — 从单人游戏世界选择界面打开预览
- **模组兼容** — 支持模组世界生成器，包括超多生态群系（Biomes O' Plenty）和真实世界生成（RWG/RTG）

## 安装

1. 安装 Minecraft 1.7.10 对应的 [Minecraft Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html)
2. 从 [Releases](../../releases) 下载最新的 World Preview NH JAR 文件
3. 将 JAR 文件放入 `mods` 文件夹

## 使用方法

### 创建世界界面

在创建世界界面中，世界类型按钮旁会出现一个小型预览按钮（16×16 图标）。点击即可使用当前种子和世界设置打开完整的生物群系预览。

### 单人游戏菜单

世界选择界面上提供了"世界预览"按钮，可以预览已有的世界。

### 预览操作

| 操作     | 功能                 |
|--------|--------------------|
| 拖拽     | 平移地图               |
| 滚轮     | 缩放                 |
| 左上角工具栏 | 设置、切换高度图、洞穴模式、Y轴交点 |
| 种子选项卡  | 保存、加载或删除种子         |

## 与原版的区别

本模组是 [caeruleusDraconis/world-preview](https://github.com/caeruleusDraconis/world-preview)（1.20+）到 Minecraft 1.7.10 的移植版本，主要变更如下：

- **移除：** 结构预览（在 1.7.10 上不可行）
- **新增：** 支持 GTNH 生态世界生成器（RWG、BOP 变体等）
- **新增：** 种子库，用于保存/加载种子
- **目标版本：** Minecraft 1.7.10 + Forge 10.13.4.1614

## 构建

```bash
./gradlew build
```

输出的 JAR 文件位于 `build/libs/` 目录。

## 许可协议

本项目基于 [GNU 通用公共许可证 v3.0](LICENSE.txt) 发布。

## 致谢

- 原版 [World Preview](https://github.com/caeruleusDraconis/world-preview) 由 [caeruleusDraconis](https://github.com/caeruleusDraconis) 开发
- 移植者 [HFstudio](https://github.com/HuanFengYeh)
