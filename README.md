# Retro Polymorph

Retro Polymorph is an unofficial Minecraft 1.12.2 / Cleanroom port of
[Polymorph](https://github.com/illusivesoulworks/polymorph), created by
[Illusive Soulworks (TheIllusiveC4)](https://github.com/illusivesoulworks).

When multiple crafting or smelting recipes share the exact same ingredients, Retro Polymorph detects the conflict and displays a compact selection button above the output slot, allowing players to choose which output they want.

## Supported Mods & Integrations

- **Crafting Tables**: Vanilla 2x2 and 3x3 grids, and modded crafting tables using Forge crafting logic.
- **Furnaces**: Vanilla furnaces and modded furnaces that reuse the vanilla
  `TileEntityFurnace` smelting machinery. Furnaces with custom tile entities,
  recipe maps, processing ticks, or XP logic require a dedicated adapter.
- **RFTools**: Crafter (Tier 1 / 2 / 3) ghost matrix recipe selection.
- **Applied Energistics 2**: Crafting Terminal, Wireless Crafting Terminal, and Pattern Terminal.
- **Extended Crafting**: Basic, Advanced, Elite, and Ultimate crafting tables.
- **Just Enough Items (JEI / HEI)**: Recipe transfer support and dynamic GUI exclusion areas (so JEI item lists do not overlap open selection panels).

## How It Works

1. Place items into a crafting grid or furnace.
2. If multiple recipes match, a small icon appears next to the result slot.
3. Click the button to open the recipe selection ribbon.
4. Click your desired recipe (or scroll through options using the mouse wheel).
5. The chosen recipe is remembered for subsequent crafting.

## Configuration

Client settings can be configured in `config/retropolymorph.cfg`:

```ini
selector {
    # Show the recipe selector when a GUI has multiple recipe matches.
    B:enabled=true

    # Number of recipe cells per selector row.
    I:columns=5

    # Maximum number of recipe rows shown on one page.
    I:rows=1

    # Horizontal and vertical button offsets (in pixels) for modded GUI compatibility.
    I:buttonOffsetX=0
    I:buttonOffsetY=0

    # Right-click the selector button to reset to the default recipe.
    B:rightClickClears=true

    # Close the selector panel immediately after clicking a recipe.
    B:closeAfterSelection=true
}
```

## Building from Source

```bash
git clone https://github.com/Sosea1/RetroPolymorph.git
cd RetroPolymorph
./gradlew build
```

The compiled mod jar will be in `build/libs/`.

## Credits & License

- **Illusive Soulworks / TheIllusiveC4** — author of the original Polymorph;
  original concept, implementation, and UI assets.
- **Sosea1** — Retro Polymorph's Minecraft 1.12.2 port, integrations, and
  maintenance.

Retro Polymorph is an independent, unofficial port and is not affiliated with
or endorsed by Illusive Soulworks. Source code and inherited assets are
distributed under **LGPL-3.0-or-later**; see `LICENSE`, `COPYING`, and
`COPYING.LESSER`.
