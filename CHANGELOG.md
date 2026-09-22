# Changelog

All notable changes to **RetroPolymorph** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.1.0-beta] - 2026-09-18

Initial feature-complete beta release of **RetroPolymorph** for Minecraft 1.12.2 and Cleanroom.

### Features
- **Server-Authoritative Conflict Resolution**: Detects matching crafting and smelting recipes sharing identical ingredients and presents an interactive selection widget above the output slot.
- **Selector Presentation Modes**:
  - `compact` (default): Up to 5 choices at once with navigation arrows, mouse wheel scrolling, and keyboard focus control (Left/Right, Home/End, PageUp/PageDown, Enter, Esc).
  - `classic`: Full horizontal strip (up to 15 choices) with automatic viewport scaling on narrow screens.
- **Player Preferences & Modpack Policies**: Remembers player recipe choices across sessions and supports modpack priority sorting (`preferredMods`, `preferredRecipes` in `config/retropolymorph.cfg`).
- **FastSuite Interoperability**: Opportunistic integration with FastSuite if installed, falling back to safe Forge registry scans if absent.
- **In-GUI Compatibility Diagnostic**: Press **F8** in any open container to print a detailed compatibility report to chat and game logs.

### Integrations
- **Vanilla & Modded Workbenches**: 2x2 player inventory grids, 3x3 crafting tables, and generic Forge workbenches.
- **Vanilla Furnaces**: Smelting conflict resolution for vanilla and compatible modded furnaces.
- **Applied Energistics 2**: Crafting Terminal, Wireless Crafting Terminal, and Pattern Terminal.
- **Extended Crafting**: Basic, Advanced, Elite, Ultimate tables, and Ender Crafter (with dynamic alternator output matching).
- **Tinkers' Construct**: Crafting Station with live multi-viewer selection synchronization across concurrent players.
- **Refined Storage 1.12**: Crafting Grid cache synchronization and Pattern Grid autocrafting NBT encoding.
- **Ender IO**: Crafter ghost recipe preview and automated crafting execution.
- **Thaumcraft 6**: Arcane Workbench dual-engine support (arcane-vs-arcane and arcane-vs-vanilla).
- **Cyclic**: Workbench and Auto-Crafter.
- **IndustrialCraft 2 (IC2)**: Batch Crafter and Industrial Workbench.
- **Retro Sophisticated Backpacks**: Crafting Upgrade.
- **Extra Utilities 2**: Mechanical Crafter and Analog Crafter via the new Machine Recipe API.
- **Mekanism**: Formulaic Assemblicator manual 3x3 crafting mode.
- **Thermal Expansion**: Sequential Fabricator ghost-grid preview and atomic Set Recipe packet synchronization.
- **JEI / HEI**: Recipe transfer support, exact recipe preservation, and dynamic GUI exclusion areas.
- **Native Safety Guards**: Forestry Worktable and Immersive Engineering Engineer's Workbench defer unconditionally to native UI buttons without generic slot interference.

### Public Addon API v1 (`dev.sosea1.retropolymorph.api`)
- **`RecipeSelectionAdapter`**: `@FunctionalInterface` contract with `probe(Container) -> AdapterDetectionResult`.
- **`AdapterDetectionResult`**: Atomic tri-state outcome: `MATCH`, `BLOCK_FALLBACK`, `MISS`.
- **Explicit Priority Tiers**: `PRIORITY_OVERRIDE` (2000), `PRIORITY_NORMAL` (500), and `PRIORITY_FALLBACK` (0). Built-in integrations use private internal tiers from 800 to 1200.
- **`SelectionContext`**: Streamlined metadata contracts (`getSelectorPlacement()`, `getPersistencePolicy()`, `getSelectionScope()`).
- **`MachineRecipeAdapter` & `MachineRecipeSurface`**: Dedicated opt-in API for custom processing machinery with machine-owned persistence and virtual UI anchors.
