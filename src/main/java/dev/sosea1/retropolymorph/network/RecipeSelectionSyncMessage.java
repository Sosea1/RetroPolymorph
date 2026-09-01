package dev.sosea1.retropolymorph.network;

import dev.sosea1.retropolymorph.api.RecipeKey;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;

/**
 * S2C acknowledgement containing the authoritative selected recipe key after a
 * request was processed. The session token is echoed unchanged so a response
 * from an old GUI lifetime cannot affect a newly-opened container with the same
 * vanilla window id.
 */
public final class RecipeSelectionSyncMessage implements IMessage {

    private int windowId;
    private int sessionToken;
    private boolean accepted;
    private boolean valid = true;

    @Nullable
    private String selectedRecipeKey;

    public RecipeSelectionSyncMessage() {
    }

    RecipeSelectionSyncMessage(
            int windowId,
            int sessionToken,
            boolean accepted,
            @Nullable String selectedRecipeKey) {
        if (selectedRecipeKey != null && !RecipeKey.isWireSafe(selectedRecipeKey)) {
            throw new IllegalArgumentException("selectedRecipeKey is not wire-safe");
        }
        this.windowId = windowId;
        this.sessionToken = sessionToken;
        this.accepted = accepted;
        this.selectedRecipeKey = selectedRecipeKey;
    }

    public int getWindowId() {
        return this.windowId;
    }

    public int getSessionToken() {
        return this.sessionToken;
    }

    public boolean isAccepted() {
        return this.accepted;
    }

    public boolean isValid() {
        return this.valid;
    }

    @Nullable
    public String getSelectedRecipeKey() {
        return this.selectedRecipeKey;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf.readableBytes() < 10) {
            invalidate();
            return;
        }

        this.windowId = buf.readInt();
        this.sessionToken = buf.readInt();
        this.accepted = buf.readBoolean();
        this.valid = true;

        boolean hasSelection = buf.readBoolean();
        if (!hasSelection) {
            this.selectedRecipeKey = null;
            if (buf.isReadable()) {
                this.valid = false;
            }
            return;
        }

        this.selectedRecipeKey = NetworkStringCodec.read(buf, RecipeKey.MAX_UTF8_BYTES);
        if (!RecipeKey.isWireSafe(this.selectedRecipeKey) || buf.isReadable()) {
            this.valid = false;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeInt(this.sessionToken);
        buf.writeBoolean(this.accepted);

        String encoded = this.selectedRecipeKey;
        buf.writeBoolean(encoded != null);
        if (encoded != null) {
            if (encoded.isEmpty()) {
                throw new IllegalStateException("Selected recipe key must not be empty");
            }
            NetworkStringCodec.write(buf, encoded, RecipeKey.MAX_UTF8_BYTES);
        }
    }

    private void invalidate() {
        this.valid = false;
        this.selectedRecipeKey = null;
    }
}
