# RetroPolymorph — Матрица совместимости (Compatibility Matrix)

В данном документе зафиксированы поддерживаемые моды, типы интеграций, механизмы изоляции сбоев и параметры отключения в конфигурации (`retropolymorph.cfg`).

Целевая платформа: **Minecraft 1.12.2** / **CleanroomMC 0.6.10-alpha+** / **Forge 14.23.5.2860**.

---

## Сводная таблица поддерживаемых модов

| Интеграция | Протестированная версия | Тип интеграции | Поведение при сбое / отсутствии | Параметр отключения |
| :--- | :--- | :--- | :--- | :--- |
| **Vanilla Crafting & Player 2x2** | 1.12.2 | Core Mixin (`CraftingManagerMixin`) | Нативный выбор рецепта | `selector.enabled = false` |
| **Vanilla Furnace** | 1.12.2 | Core Mixin (`TileEntityFurnaceMixin`, `SlotFurnaceOutputMixin`) | Нативная переплавка | `selector.enabled = false` |
| **Applied Energistics 2 (AE2)** | AE2 rv6-stable-7 / AE2 Extended Life v0.55.28+ | Mixin + `Ae2CraftingTermAdapter` + `Ae2PatternTermAdapter` | Откат на нативный `SlotCraftingTerm` | `integrations.ae2 = false` |
| **Refined Storage** | 1.6.16+ | Mixin + `RefinedStorageCraftingGridAdapter` | Откат на нативный Grid Node | `integrations.refinedstorage = false` |
| **Retro Sophisticated Backpacks** | 1.1.4 | Reflection + ModularUI bridge (`RetroSophisticatedBackpackAdapter`) | Откат на нативную матрицу RSB | `integrations.retro_sophisticated_backpacks = false` |
| **Thermal Expansion** | 5.5.7.1 | Mixin + `ThermalSequentialFabricatorAdapter` | Откат на нативный Fabricator | `integrations.thermal = false` |
| **Mekanism** | 9.8.3.390 / CE Unofficial | Mixin + `MekanismFormulaicAssemblicatorAdapter` | Откат на нативный Formulaic Assemblicator | `integrations.mekanism = false` |
| **Thaumcraft 6** | 6.1.BETA26 | Mixin + `ThaumcraftArcaneWorkbenchAdapter` | Откат на нативный `TileArcaneWorkbench` | `integrations.thaumcraft = false` |
| **Extra Utilities 2** | 1.9.9 | Mixin + `ExtraUtilities2CrafterAdapter` | Откат на нативный Crafter | `integrations.extrautils2 = false` |
| **Ender IO** | 5.3.72 | Mixin + `EnderIoCrafterAdapter` | Откат на нативный Crafter | `integrations.enderio = false` |
| **Forestry** | 5.8.2.422 | Native Guard (`forestry_worktable_guard`) | Приоритет нативного селектора Forestry (стрелки под сеткой) | Автоматический guard |
| **IndustrialCraft 2 (IC2)** | 2.8.222-ex112 | Mixin + `Ic2BatchCrafterAdapter` + `Ic2IndustrialWorkbenchAdapter` | Откат на нативный Batch Crafter | `integrations.ic2 = false` |
| **RFTools / RFTools Control** | RFTools 7.73 / RFTools Control 2.0.2 | Mixin + `RFToolsWorkbenchAdapter` + `RFToolsCrafterAdapter` | Откат на нативный Workbench | `integrations.rftools = false` |
| **Extended Crafting** | 1.5.6 | Mixin + `ExtendedTableAdapter` (3x3..9x9) + `ExtendedEnderCrafterAdapter` (Эндер-верстак) | Откат на нативный Extended Table / Ender Crafter | `integrations.extendedCrafting = false` |
| **Cyclic** | 1.20.14 | Mixin + `CyclicWorkbenchAdapter` (Верстак) + `CyclicCrafterAdapter` (Авто-верстак) | Откат на нативный Workbench / Crafter | `integrations.cyclic = false` |
| **Tinkers' Construct** | 2.13.0.183 | Mixin + `TinkersCraftingStationAdapter` (FastWorkbench cache invalidation, MultiModule-якорь, runtime shared-selection между всеми открытыми окнами одной станции) | Откат на Crafting Station | `integrations.tconstruct = false` |
| **Just Enough Items (JEI)** | 4.16.1.1013+ | `PolymorphJeiPlugin` + `JeiRecipeTransferMixin` | Нативный JEI transfer | `integrations.jei = false` |
| **FastSuite** | Любая версия FastSuite | `FastSuiteInterop` | Полный скан Forge Registries | Автоматический fallback |

---

## Архитектурные принципы изоляции сбоев

1. **Failure Isolation**: Ошибки сторонних обработчиков рецептов изолируются через `RecipeProbe` и `SelectionContextGuard`. Если рецепт стороннего мода падает с исключением в `matches()` или `getCraftingResult()`, он исключается из списка вариантов, а сервер и игра продолжают работу без краша.
2. **Контроль через конфигурацию**: Любая интеграция может быть полностью отключена в `retropolymorph.cfg`. При отключении интеграции регистрируется безопасный Guard-адаптер (`CustomRecipeEngineGuardAdapter`), предотвращающий случайный захват контейнера generic-детектором.
3. **Безопасность Mixin-инъекций**: Все интеграционные Mixin-конфигурации (`mixins.retropolymorph.<mod>.json`) загружаются динамически через `RetroPolymorphMixinConnector` только при фактическом присутствии целевого мода в среде.
