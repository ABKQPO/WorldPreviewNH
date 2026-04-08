# World Preview NH

**[中文文档](README_zh.md)**

A Minecraft 1.7.10 mod for previewing world biomes before creation. This is a backport of [World Preview](https://github.com/caeruleusDraconis/world-preview) to 1.7.10 for the [GTNH](https://github.com/GTNewHorizons) modpack ecosystem, with structure preview removed.

## Features

- **Biome Map Preview** — Full-screen interactive biome map generated from the world seed before world creation
- **Heightmap View** — Toggle a colorized heightmap overlay
- **Y-Layer Intersection** — View block layers at specific Y levels
- **Cave Mode** — Toggle cave biome display
- **Zoom & Pan** — Scroll to zoom (1–2048 blocks/pixel), drag to pan across the map
- **Coordinate Goto** — Jump to specific X/Z coordinates
- **Biome Search** — Filter and highlight biomes by name
- **Seed Library** — Save and load up to 50 favorite seeds
- **In-Game Preview** — Open the preview from the single-player world selection screen
- **Mod Compatibility** — Works with modded world generators including Biomes O' Plenty and Realistic World Gen (RWG/RTG)

## Installation

1. Install [Minecraft Forge](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.7.10.html) for Minecraft 1.7.10
2. Download the latest World Preview NH JAR from [Releases](../../releases)
3. Place the JAR file in your `mods` folder

## Usage

### From Create World Screen

A small preview button (16×16 icon) appears next to the World Type button on the Create World screen. Clicking it opens the full biome preview using the current seed and world settings.

### From Single-Player Menu

A "World Preview" button is available on the world selection screen to preview existing worlds.

### Preview Controls

| Control          | Action                                                |
|------------------|-------------------------------------------------------|
| Drag             | Pan the map                                           |
| Scroll           | Zoom in/out                                           |
| Top-left toolbar | Settings, toggle heightmap, cave mode, Y-intersection |
| Seeds tab        | Save, load, or delete seeds                           |

## Differences from Original

This mod is a backport of [caeruleusDraconis/world-preview](https://github.com/caeruleusDraconis/world-preview) (1.20+) to Minecraft 1.7.10 with the following changes:

- **Removed:** Structure preview (not feasible on 1.7.10)
- **Added:** Support for GTNH-ecosystem world generators (RWG, BOP variants)
- **Added:** Seed library for saving/loading seeds
- **Target:** Minecraft 1.7.10 + Forge 10.13.4.1614

## Building

```bash
./gradlew build
```

The output JAR will be in `build/libs/`.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE.txt).

## Credits

- Original [World Preview](https://github.com/caeruleusDraconis/world-preview) by [caeruleusDraconis](https://github.com/caeruleusDraconis)
- Backported by [HFstudio](https://github.com/HuanFengYeh)
