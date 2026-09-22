# RetroPolymorph Architecture Overview

RetroPolymorph is a server-authoritative recipe conflict resolution mod for Minecraft 1.12.2 and Cleanroom. It detects when multiple crafting or machine recipes share identical inputs and allows players to explicitly select their desired output.

---

## 1. Core Principles

1. **Server-Authoritative**: The server determines matching recipes, maintains active selection state, and validates all recipe selections. The client is a presentation layer that renders the selector widget and sends player choices.
2. **Zero-Guessing Guard Rails**: Third-party containers that use non-standard crafting systems are guarded against generic slot-scanning to prevent accidental item duplication or ghost craft execution.
3. **No Cosmetic Selectors**: A selector is only displayed when RetroPolymorph can guarantee that the selected recipe controls the real craft or machine operation.
4. **Wire-Safe Protocol**: All network packets use bounded, UTF-8 strings (up to 256 bytes without control characters) and primitive types.

---

## 2. Selection Pipeline Flow

```
[ Container Opened / Matrix Modified ]
                  │
                  ▼
      [ RecipeSelectionAdapters ]
  Probes registered adapters in priority order
  (OVERRIDE -> SPECIALIZED -> STANDARD -> NORMAL -> FALLBACK)
                  │
        ┌─────────┴─────────┐
        ▼                   ▼
[ Adapter Matched ]    [ Block Fallback / Miss ]
        │                   │
        │                   └─► No selector (safe exit)
        ▼
[ SelectionContext Created ]
        │
        ├─► getInputCount() / getInputStack() (Input change tracking)
        ├─► findOptions(World) (Gathers valid RecipeOptions)
        │
        ▼
[ Option Count Check ]
        │
        ├─► Options <= 1: Clears active selector, restores default
        │
        └─► Options >= 2: Confirmed Conflict
                  │
                  ├─► Resolve active selection (Player NBT preference / Tile state)
                  ├─► Apply selection to inventory/machine
                  └─► Sync options + active choice to client (`RecipeSelectionSyncMessage`)
```

---

## 3. Subsystem Breakdown

### 3.1. Adapter System (`dev.sosea1.retropolymorph.api`)
- **`RecipeSelectionAdapter`**: Functional interface `probe(Container) -> AdapterDetectionResult`.
- **`AdapterDetectionResult`**: Atomic tri-state outcome: `MATCH` (with `SelectionContext`), `BLOCK_FALLBACK` (prevents generic detector), or `MISS`.
- **Priority Tiers**:
  - `PRIORITY_OVERRIDE` (2000): Preempts built-ins for modpack/addon overrides.
  - `PRIORITY_NORMAL` (500): Default for third-party addon registrations.
  - `PRIORITY_FALLBACK` (0): Last-resort generic slot scanner.
  - Built-in integrations use private priority values from 800 to 1200.

### 3.2. Context & Placement (`SelectionContext`, `SelectorPlacement`)
- **`SelectionContext`**: Provides input snapshots, finds matching options, applies selections, and defines metadata:
  - `getSelectorPlacement()`: Defines UI positioning via `SelectorPlacement` (result-slot relative, absolute GUI coordinates, or top-corner anchored).
  - `getPersistencePolicy()`: Governs whether choices are saved to player profile (`PLAYER_PERSISTENT`), may replace an active choice (`PLAYER_PERSISTENT_OVERRIDE`), or remain owned by the machine tile entity (`OWNER_ONLY`).
  - `getSelectionScope()`: Differentiates local single-player windows (`LOCAL`) from shared multi-viewer inventories (`SHARED`).

### 3.3. Machine Recipe API (`dev.sosea1.retropolymorph.machine`)
- **`MachineRecipeAdapter` & `MachineRecipeSurface`**: Specialized bridge for custom processing machines (e.g., Extra Utilities 2 Mechanical/Analog Crafter).
- Enforces `controlsActualOperation() == true` before creating selectors.
- Supports virtual result anchors (`getResultSlot() == null`) and machine-owned persistence models.

### 3.4. Multi-Viewer & Shared Station Synchronization
- **`SharedSelectionViewerRegistry`**: Tracks open viewers per shared inventory owner identity and broadcasts selection updates across all concurrent viewers.
- **`TinkersSharedSelectionRegistry`**: Coordinates Tinkers' Construct Crafting Stations, mirroring selections across individual wrapper inventories sharing the same parent tile.
- **Lifecycle Guarantees**: Complete reset on server stopping, world unload, and client disconnect to prevent cross-session state leakage.

### 3.5. Native Safety Guards
- **Forestry Worktable**: Defers unconditionally to Forestry's native recipe cycling buttons.
- **Immersive Engineering Engineer's Workbench**: Defers unconditionally to IE's blueprint selector.
- **Package & Engine Guards**: Blocks generic slot detection on known third-party crafters when dedicated adapters are disabled or absent.

### 3.6. Diagnostics
- **In-GUI F8 Key**: Instant server-side compatibility snapshot written to chat and logs.
- **CLI Commands**: `/retropolymorph (adapters|policy|stats|diagnose)`.
