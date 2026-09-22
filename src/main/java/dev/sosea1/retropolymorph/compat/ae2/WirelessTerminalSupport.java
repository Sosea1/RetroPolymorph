package dev.sosea1.retropolymorph.compat.ae2;

import net.minecraft.inventory.Container;

/** Small dependency-free classifier for the AE2WTLib wireless-terminal family. */
final class WirelessTerminalSupport {

    static final String BASE_CONTAINER = "p455w0rd.ae2wtlib.api.container.ContainerWT";

    private WirelessTerminalSupport() {
    }

    static boolean isWirelessContainer(Container container) {
        return container != null && hasClassInHierarchy(container.getClass(), BASE_CONTAINER);
    }

    private static boolean hasClassInHierarchy(Class<?> type, String target) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            if (target.equals(current.getName())) {
                return true;
            }
        }
        return false;
    }
}
