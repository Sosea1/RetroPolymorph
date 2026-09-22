package dev.sosea1.retropolymorph.compat.rsb;

import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;

import javax.annotation.Nullable;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Deque;
import java.util.IdentityHashMap;

/**
 * Handles client-side GUI introspection for ModularUI and Retro Sophisticated Backpacks.
 * Determines the crafting button position and tab expansion state.
 */
public final class RsbGuiAccess {

    private RsbGuiAccess() {
    }

    @Nullable
    public static Point findCraftingButtonPosition(Container container) {
        if (!RsbContainerAccess.recognizes(container)) {
            return null;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.currentScreen == null) {
            return null;
        }

        Object screen = RsbBindings.getModularScreen(mc.currentScreen);
        if (screen == null) {
            return null;
        }

        Deque<Object> queue = new ArrayDeque<Object>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<Object, Boolean>();

        // Root 1: screen.getMainPanel()
        Object mainPanel = RsbBindings.invokeNoArg(screen, "getMainPanel");
        if (mainPanel != null) {
            queue.add(mainPanel);
        }

        // Root 2: screen.getPanelManager().getOpenPanels()
        Object panelManager = RsbBindings.invokeNoArg(screen, "getPanelManager");
        if (panelManager != null) {
            Object openPanels = RsbBindings.invokeNoArg(panelManager, "getOpenPanels");
            if (openPanels instanceof Collection) {
                for (Object p : (Collection<?>) openPanels) {
                    if (p != null) {
                        queue.add(p);
                    }
                }
            }
        }

        int scanned = 0;
        while (!queue.isEmpty() && scanned < 256) {
            Object widget = queue.removeFirst();
            if (widget == null || visited.put(widget, Boolean.TRUE) != null) {
                continue;
            }
            scanned++;

            if (isCraftingTabWidget(widget)) {
                // Method 1: Find the actual result slot inside the CraftingUpgradeWidget
                Rectangle resultArea = findCraftingResultArea(widget);
                if (resultArea != null && resultArea.width > 10 && resultArea.height > 10) {
                    int x = resultArea.x + resultArea.width + 1;
                    int y = resultArea.y - 19;
                    return new Point(x, y);
                }

                // Method 2: Fallback to tab panel area
                Rectangle tabArea = extractWidgetArea(widget);
                if (tabArea != null && tabArea.width > 20 && tabArea.height > 20) {
                    int x = tabArea.x + tabArea.width - 16 - 12;
                    int y = tabArea.y + tabArea.height - 30;
                    return new Point(x, y);
                }
            }

            // Traverse children
            Object children = RsbBindings.invokeNoArg(widget, "getChildren");
            if (children instanceof Collection) {
                for (Object child : (Collection<?>) children) {
                    if (child != null) {
                        queue.addLast(child);
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    public static Boolean isCraftingTabExpanded(Container container) {
        if (!RsbContainerAccess.recognizes(container)) {
            return null;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.currentScreen == null) {
            return null;
        }

        Object screen = RsbBindings.getModularScreen(mc.currentScreen);
        if (screen == null) {
            return null;
        }

        Deque<Object> queue = new ArrayDeque<Object>();
        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<Object, Boolean>();

        Object mainPanel = RsbBindings.invokeNoArg(screen, "getMainPanel");
        if (mainPanel != null) {
            queue.add(mainPanel);
        }

        Object panelManager = RsbBindings.invokeNoArg(screen, "getPanelManager");
        if (panelManager != null) {
            Object openPanels = RsbBindings.invokeNoArg(panelManager, "getOpenPanels");
            if (openPanels instanceof Collection) {
                for (Object p : (Collection<?>) openPanels) {
                    if (p != null) {
                        queue.add(p);
                    }
                }
            }
        }

        int scanned = 0;
        while (!queue.isEmpty() && scanned < 256) {
            Object widget = queue.removeFirst();
            if (widget == null || visited.put(widget, Boolean.TRUE) != null) {
                continue;
            }
            scanned++;

            if (isCraftingTabWidget(widget)) {
                Rectangle area = extractWidgetArea(widget);
                return Boolean.valueOf(area != null && area.width > 20 && area.height > 20);
            }

            Object children = RsbBindings.invokeNoArg(widget, "getChildren");
            if (children instanceof Collection) {
                for (Object child : (Collection<?>) children) {
                    if (child != null) {
                        queue.addLast(child);
                    }
                }
            }
        }

        return null;
    }

    @Nullable
    private static Rectangle findCraftingResultArea(Object craftingTabWidget) {
        if (craftingTabWidget == null) {
            return null;
        }
        Field field = RsbBindings.findField(craftingTabWidget.getClass(), "craftingResult");
        if (field != null) {
            try {
                Object slot = field.get(craftingTabWidget);
                if (slot != null) {
                    Rectangle area = extractWidgetArea(slot);
                    if (area != null && area.width > 10 && area.height > 10) {
                        return area;
                    }
                }
            } catch (IllegalAccessException | RuntimeException | LinkageError ignored) {
            }
        }
        return null;
    }

    private static boolean isCraftingTabWidget(Object widget) {
        String name = widget.getClass().getName();
        if (name.contains("CraftingUpgradeWidget")) {
            return true;
        }
        Object val = RsbBindings.invokeNoArg(widget, "getName");
        if (val instanceof String && ((String) val).toLowerCase(java.util.Locale.ROOT).contains("crafting")) {
            return true;
        }
        return false;
    }

    @Nullable
    private static Rectangle extractWidgetArea(Object widget) {
        Object area = RsbBindings.invokeNoArg(widget, "getArea");
        return area instanceof Rectangle ? (Rectangle) area : null;
    }
}
