package dev.sosea1.retropolymorph.network;

import dev.sosea1.retropolymorph.api.RecipeKey;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;

/**
 * C2S request for selecting, clearing, or querying the current recipe choice.
 * Recipe keys are intentionally opaque to the transport layer.
 *
 * The client session token distinguishes consecutive GUI lifetimes that may
 * reuse the same vanilla window id (notably the player inventory at window 0).
 */
public final class RecipeSelectionMessage implements IMessage {

    private static final byte OP_SELECT = 0;
    private static final byte OP_CLEAR = 1;
    private static final byte OP_QUERY = 2;

    private int windowId;
    private int sessionToken;
    private byte operation;
    private boolean valid = true;

    @Nullable
    private String recipeKey;

    public RecipeSelectionMessage() {
    }

    private RecipeSelectionMessage(
            int windowId,
            int sessionToken,
            byte operation,
            @Nullable String recipeKey) {
        this.windowId = windowId;
        this.sessionToken = sessionToken;
        this.operation = operation;
        this.recipeKey = recipeKey;
    }

    public static RecipeSelectionMessage select(
            int windowId,
            int sessionToken,
            String recipeKey) {
        if (recipeKey == null) {
            throw new NullPointerException("recipeKey");
        }
        if (!RecipeKey.isWireSafe(recipeKey)) {
            throw new IllegalArgumentException("recipeKey is not wire-safe");
        }
        return new RecipeSelectionMessage(windowId, sessionToken, OP_SELECT, recipeKey);
    }

    public static RecipeSelectionMessage clear(int windowId, int sessionToken) {
        return new RecipeSelectionMessage(windowId, sessionToken, OP_CLEAR, null);
    }

    public static RecipeSelectionMessage query(int windowId, int sessionToken) {
        return new RecipeSelectionMessage(windowId, sessionToken, OP_QUERY, null);
    }

    public int getWindowId() {
        return this.windowId;
    }

    public int getSessionToken() {
        return this.sessionToken;
    }

    public boolean isSelect() {
        return this.operation == OP_SELECT;
    }

    public boolean isClear() {
        return this.operation == OP_CLEAR;
    }

    public boolean isQuery() {
        return this.operation == OP_QUERY;
    }

    public boolean isValid() {
        return this.valid;
    }

    @Nullable
    public String getRecipeKey() {
        return this.recipeKey;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        if (buf.readableBytes() < 9) {
            invalidate();
            return;
        }

        this.windowId = buf.readInt();
        this.sessionToken = buf.readInt();
        this.operation = buf.readByte();
        if (this.operation < OP_SELECT || this.operation > OP_QUERY) {
            invalidate();
            return;
        }

        this.valid = true;
        if (this.operation != OP_SELECT) {
            this.recipeKey = null;
            if (buf.isReadable()) {
                this.valid = false;
            }
            return;
        }

        this.recipeKey = NetworkStringCodec.read(buf, RecipeKey.MAX_UTF8_BYTES);
        if (!RecipeKey.isWireSafe(this.recipeKey) || buf.isReadable()) {
            this.valid = false;
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeInt(this.sessionToken);
        buf.writeByte(this.operation);
        if (this.operation != OP_SELECT) {
            return;
        }

        String encoded = this.recipeKey;
        if (encoded == null || encoded.isEmpty()) {
            throw new IllegalStateException("Select request is missing recipe key");
        }
        NetworkStringCodec.write(buf, encoded, RecipeKey.MAX_UTF8_BYTES);
    }

    private void invalidate() {
        this.valid = false;
        this.recipeKey = null;
    }
}
