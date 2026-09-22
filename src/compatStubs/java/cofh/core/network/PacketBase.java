package cofh.core.network;

import java.io.DataInputStream;

/** Compile-only CoFHCore 1.12 surface used by optional Thermal mixins. */
public abstract class PacketBase {
    public DataInputStream datain;

    public PacketBase addString(String value) {
        return this;
    }

    public String getString() {
        return null;
    }
}
