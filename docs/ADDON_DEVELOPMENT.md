# RetroPolymorph Addon & Integration Development Guide

This guide explains how third-party Minecraft 1.12.2 Forge mods and modpack addons can integrate with RetroPolymorph using the public API.

Public API Version: `RetroPolymorphAPI.API_VERSION = 1`

---

## 1. Core Architecture Overview

RetroPolymorph is **server-authoritative**. When a player opens a container with recipe conflicts:
1. The server detects the appropriate `SelectionContext` using registered `RecipeSelectionAdapter` instances.
2. The context queries matching `RecipeOption`s from the world/recipes.
3. If an explicit choice is made or restored, the context applies it to the underlying inventory/machine.
4. The active options and selected recipe are synchronized to the client GUI with a `SelectionReason`.

---

## 2. Implementing a `RecipeSelectionAdapter`

An adapter checks if an open `Container` belongs to your mod.

```java
public class MyModAdapter implements RecipeSelectionAdapter {

    @Override
    public AdapterDetectionResult probe(Container container) {
        if (container instanceof ContainerMyWorkbench) {
            return AdapterDetectionResult.match(new MyWorkbenchContext((ContainerMyWorkbench) container));
        }
        return AdapterDetectionResult.miss();
    }
}
```

### Registration & Priority

Register your adapter during FML `FMLInitializationEvent` or `FMLPreInitializationEvent`:

```java
ResourceLocation id = new ResourceLocation("mymod", "workbench");
// Defaults to RetroPolymorphAPI.PRIORITY_NORMAL (500)
RetroPolymorphAPI.registerAdapter(id, new MyModAdapter());

// Or specify priority explicitly:
RetroPolymorphAPI.registerAdapter(id, RetroPolymorphAPI.PRIORITY_NORMAL, new MyModAdapter());
```

### Priority Tiers

| Constant | Value | When to use |
|---|---|---|
| `PRIORITY_OVERRIDE` | 2000 | Intentionally override any built-in adapter or guard. Probed first. |
| `PRIORITY_NORMAL` | 500 | **Recommended for third-party addon adapters.** Safely evaluated after built-ins and before generic fallback scanners. Default for `registerAdapter(id, adapter)`. |
| `PRIORITY_FALLBACK` | 0 | Last-resort generic slot scanner fallback, probed after everything else. |

- Higher numbers are probed first.
- Equal-priority adapters are queried in registration order.
- Built-in integrations use internal priorities from 800 to 1200; those are not public API constants.

### Guard Adapters & Blocking Fallback

If a container belongs to your mod but recipe selection is disabled or unsupported (e.g., non-standard grid), return `AdapterDetectionResult.blockFallback()`. This prevents RetroPolymorph from guessing generic 3x3 or furnace slots.

---

## 3. Implementing a `SelectionContext`

A `SelectionContext` bridges RetroPolymorph to the inventory or machine.

Key methods to implement:
- `findOptions(World world)`: returns all valid `RecipeOption` instances matching current inputs.
- `select(String recipeKey, World world)`: applies the chosen recipe to the container/inventory so that the native craft/take result matches the choice.
- `clearSelection()`: resets to default/automatic recipe resolution.
- `getSelectedRecipeKey()`: returns the currently selected recipe key, or `null`.
- `getInputCount()` and `getInputStack(int index)`: returns input snapshot for change tracking.

### Metadata Methods

1. **`SelectionScope getSelectionScope()`**:
   - `SelectionScope.local()`: (default) the selection belongs only to this player's open container instance.
   - `SelectionScope.shared(Object ownerIdentity)`: the container views a shared multi-player inventory (e.g., Tinkers' Crafting Station). When one player changes the selection, all concurrent viewers are live-synced.

2. **`SelectionPersistencePolicy getPersistencePolicy()`**:
   - `SelectionPersistencePolicy.PLAYER_PERSISTENT`: (default) player choices are saved in player NBT across sessions for this input conflict.
   - `SelectionPersistencePolicy.PLAYER_PERSISTENT_OVERRIDE`: stored player choices may replace an already-active selection when the container opens.
   - `SelectionPersistencePolicy.OWNER_ONLY`: selection belongs to the machine tile entity and should not update player preference NBT.

3. **`SelectorPlacement getSelectorPlacement()`**:
   - `SelectorPlacement.resultSlot(anchorX, anchorY[, offsetX, offsetY])`: anchors the selector button near the result slot.
   - `SelectorPlacement.absolute(x, y[, offsetX, offsetY])`: anchors the button at absolute GUI coordinates.
   - `SelectorPlacement.guiTopRight(rightInset, topInset[, offsetX, offsetY])`: anchors the button to the GUI's top-right corner.
   - `SelectorPlacement.hidden()`: suppresses the selector for this context.

---

## 4. Recipe Key Guidelines

Recipe keys must be **wire-safe**:
- Must be a valid UTF-8 string up to 256 bytes without ISO control characters (`\u0000`–`\u001F`, `\u007F`–`\u009F`). Non-ASCII characters (e.g. accented characters or UTF-8 symbols) and standard punctuation are allowed across the network protocol.
- For standard Forge recipes, the key MUST match the recipe's registry name (e.g., `minecraft:iron_block` or `mymod:gear_iron`), which is a valid `ResourceLocation`.
- For custom/synthetic machine recipes, use namespaced identifiers (e.g., `thermalexpansion:transposer/bucket` or `extrautils2:crafter/42`).

---

## 5. Modpack & Addon Policy Registration

Addons can register exact recipe priorities via the API:

```java
// Higher positive numbers win. 0 removes the override.
RetroPolymorphAPI.setRecipePriority(new ResourceLocation("mymod", "special_recipe"), 500);
```

- API registrations survive modpack config reloads.
- Player explicit choices always take precedence over modpack/API defaults.

---

## 6. Safety & Threading Constraints

1. **Server Thread Only**: All recipe matching, inventory reading, and selection mutation MUST run on the main Minecraft server thread. Do not run `IRecipe.matches()` on background threads.
2. **Fail-Open / Fail-Safe**: Catch expected third-party exceptions inside `findOptions()` and `select()`. Do not throw `RuntimeException` out of the adapter.
