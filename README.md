# Advanced Sorter

[![Release](https://img.shields.io/github/v/release/snakex21/AdvancedSorter)](https://github.com/snakex21/AdvancedSorter/releases/latest) [![License: MIT](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE) [![Minecraft](https://img.shields.io/badge/Minecraft-1.12.2-62B47A)](#requirements) [![Forge](https://img.shields.io/badge/Forge-14.23.5.2768-E04E14)](#requirements) [![Java](https://img.shields.io/badge/Java-8-007396)](#building)

A Minecraft 1.12.2 (Forge) mod focused on item, fluid and gas logistics: teleport pipes, sorting, a pump controller, an auto crafter with custom recipes, fluid tanks and, since 2.1.0, a set of RF-powered automation machines.

[PL] Mod do Minecrafta 1.12.2 (Forge) skupiony na logistyce przedmiotów, płynów i gazów: rury teleportacyjne, sortowanie, kontroler pomp, auto crafter z własnymi recepturami, zbiorniki na ciecze, a od wersji 2.1.0 także zestaw maszyn do automatyzacji zasilanych RF. Opis po polsku: [Po polsku](#po-polsku).

## Contents

- [Features](#features)
- [Machines (new in 2.1.0)](#machines-new-in-210)
- [Crafting recipes](#crafting-recipes)
- [Requirements](#requirements)
- [Installation](#installation)
- [Building](#building)
- [Changelog](#changelog)
- [Third-party code](#third-party-code)
- [Po polsku](#po-polsku)
- [License](#license)

## Features

- **Transport pipes**: stone, iron, gold and diamond item pipes, matching extraction pipes and a directional pipe.
- **Fluid pipes**: stone, iron, gold and diamond fluid pipes, an extraction fluid pipe and a directional fluid pipe.
- **Teleport pipes**: send items, fluids and Mekanism gases over any distance by frequency, with no physical connection.
- **Advanced Sorter**: precise filtering and routing of items around your base.
- **Smart Router**: routes items to teleport frequencies by per-item rules; items without a matching rule stay in its buffer.
- **Pump Controller with presets**: manage several pump frequencies from one block, name and save up to 6 presets. Works with the Advanced Pump and Fluid Outlet.
- **Auto Crafter**: custom recipes (ingredient mapping to result), chain crafting of missing ingredients, Priority or Round-Robin automation, pipe integration (top = input, sides and bottom = output).
- **Inventory Indexer**: browse the contents of many chests from one place.
- **Fluid tanks**: 4 tiers (Basic 16 B, Advanced 64 B, Elite 256 B, Ultimate 1024 B), configurable INPUT/OUTPUT sides, bucket support, visible fluid, connected-pipe indicator in the GUI, the fluid is kept when the block is picked up.
- **Network Tool** and **Pipe Wrench**: link chests to an Inventory Indexer, toggle pipe connections.

## Machines (new in 2.1.0)

Most machines run on RF/FE. The Energy Converter and the Universal Charger Plate also handle Mekanism Joules and, when IndustrialCraft 2 is installed, IC2 EU.

| Machine | What it does |
|---|---|
| Cobblestone Generator | Generates cobblestone; needs water and lava. |
| Obsidian Generator | Generates obsidian safely; needs water and lava. |
| Crucible | Melts cobblestone into lava (1000 RF per cobblestone, 4 cobblestone = 1 bucket). |
| Crusher | Doubles ore output; cobblestone to gravel, gravel to sand. |
| Washer | Sand to clay, concrete powder to concrete (200 RF and 250 mB water per operation). |
| Flint Maker | Gravel to flint (400 RF per operation). |
| De-constructor | Reverse-crafts items back into their ingredients. |
| Auto Repairer | Repairs tools and armor with RF. |
| Enchantment Extractor | Moves enchantments from tools and armor onto books. |
| XP Vacuum | Collects XP orbs in a wide radius and stores them as liquid XP. |
| Block Breaker | Breaks the block in front of it like a pickaxe. |
| Block Placer | Places blocks from its inventory into the world. |
| Mob Grinder | Damages mobs in an area in front of it and collects the drops. |
| Auto Fisher | Catches fish and treasure; needs a water source and energy. |
| Void Miner | Generates ores from nothing (1000 RF/t). |
| Ammo Press | Recipe-based crafter for automating ammunition (meant for Immersive Engineering bullets). |
| Atmospheric Stabilizer | Controls weather in the area; high energy use. |
| Energy Converter | Lossless conversion between RF/FE, Mekanism Joules and IC2 EU (1 RF = 2.5 J = 0.25 EU). |
| Universal Charger Plate | Stand on it to charge items in your inventory (RF/FE, Mekanism Joules, fluids and gases). |
| Trash Can | Voids items, fluids and energy. |

## Crafting recipes

Most blocks and tools have crafting recipes (JEI shows them in game). Fluid tanks:

| Tier | Recipe |
|---|---|
| Basic | 8x Iron Ingot + Glass |
| Advanced | 8x Gold Ingot + Basic Tank |
| Elite | 8x Diamond + Advanced Tank |
| Ultimate | 8x Emerald + Elite Tank |

Recipe files: [`src/main/resources/assets/advancedsorter/recipes`](src/main/resources/assets/advancedsorter/recipes).

## Requirements

- Minecraft 1.12.2 with Forge 14.23.5.2768 or newer.
- [Mekanism](https://www.curseforge.com/minecraft/mc-mods/mekanism) for 1.12.2 (required).
- IndustrialCraft 2 (optional, enables EU support).

## Installation

1. Install Forge for Minecraft 1.12.2 and Mekanism.
2. Download `advancedsorter-<version>.jar` from [Releases](https://github.com/snakex21/AdvancedSorter/releases/latest) or [build it yourself](#building).
3. Put the jar in your `mods` folder.

Back up your world before updating from an older version.

## Building

Requires **JDK 8** (ForgeGradle 2.3 does not run on newer Java). The Gradle wrapper (4.10.3) downloads everything else, including Minecraft, Forge and the Mekanism/IC2 APIs from CurseMaven.

```bash
git clone https://github.com/snakex21/AdvancedSorter.git
cd AdvancedSorter
# Windows: set JAVA_HOME to a JDK 8 install, then
gradlew.bat build
# Linux/macOS
./gradlew build
```

The jar is written to `build/libs/advancedsorter-<version>.jar`. To start a development client: `gradlew runClient`.

The project has no automated tests; verify changes in a development client.

`generate_gui_textures.py` (Python 3 + Pillow) regenerates the machine GUI textures in `src/main/resources/assets/advancedsorter/textures/gui`.

## Changelog

### v2.1.0
- 20 new machines (see [Machines](#machines-new-in-210)) and the Smart Router.
- Energy support: RF/FE, Mekanism Joules and optional IC2 EU.
- Pending-teleport queues for items, fluids and gases: anything sent to an unloaded chunk is stored and delivered when that chunk loads.
- Item models for directional and teleport pipes, more recipes, updated textures and translations (EN, PL).
- The version shown in the Forge mod list now matches the jar version.

### v2.0.0
- Fluid tanks in 4 tiers with configurable INPUT/OUTPUT sides, bucket support and a connected-pipe indicator.
- Smart Sync: frequent fluid sync only while the GUI is open; tick skipping and neighbour caching to reduce lag.
- Fixed teleport pipes not working after loading a world.

### v1.0.0 to v1.3.0
- Teleport pipes, pump controller, auto crafter, inventory index, Mekanism gas support, fixes.

## Third-party code

`src/main/java/buildcraft/api` contains the BuildCraft API (Copyright SpaceToad and the BuildCraft Team), distributed under the MIT License as stated in its file headers.

## Po polsku

**Advanced Sorter** to mod do Minecrafta 1.12.2 (Forge) do logistyki przedmiotów, płynów i gazów.

- **Rury**: transportowe, wyciągające i kierunkowe dla przedmiotów i płynów (kamień, żelazo, złoto, diament).
- **Rury teleportacyjne**: przedmioty, płyny i gazy z Mekanism na dowolną odległość, po częstotliwości.
- **Advanced Sorter** i **Smart Router**: filtrowanie i kierowanie przedmiotów według reguł.
- **Kontroler pomp** z 6 nazwanymi presetami.
- **Auto Crafter**: własne receptury, craftowanie łańcuchowe, tryb Priorytet / Round-Robin, integracja z rurami.
- **Inventory Indexer**: podgląd wielu skrzynek z jednego miejsca.
- **Zbiorniki na ciecze**: 4 tiery, konfigurowalne strony, obsługa wiader.
- **Maszyny (nowość w 2.1.0)**: generator bruku i obsydianu, tygiel, kruszarka, płuczka, wytwarzacz krzemienia, dekonstruktor, auto-naprawiacz, ekstraktor zaklęć, odkurzacz XP, niszczarka i stawiacz bloków, mob grinder, auto-wędkarz, void miner, prasa amunicji, stabilizator atmosfery, konwerter energii (RF/FE, J, EU), uniwersalna płyta ładująca, kosz.

Wymagania: Forge 1.12.2 i Mekanism; IC2 opcjonalnie. Budowanie: JDK 8, `gradlew build`, plik w `build/libs`.

## License

[MIT](LICENSE)
