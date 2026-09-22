package dev.sosea1.retropolymorph.network;

import dev.sosea1.retropolymorph.api.RecipeKey;
import io.netty.buffer.ByteBuf;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;

/**
 * C2S request for selecting, clearing, or querying the current recipe choice.
 *
 * inputRevision is a client-local generation number for the visible input
 * snapshot. It is echoed by the server so late replies for an older matrix can
 * be ignored without trusting the client for any recipe calculation.
 */
public final class RecipeSelectionMessage implements IMessage {

    private static final byte OP_SELECT = 0;
    private static final byte OP_CLEAR = 1;
    private static final byte OP_QUERY = 2;

    private int windowId;
    private int sessionToken;
    private int inputRevision;
    private byte operation;
    private boolean valid = true;

    @Nullable
    private String recipeKey;

    public RecipeSelectionMessage() {
    }

    private RecipeSelectionMessage(
            int windowId,
            int sessionToken,
            int inputRevision,
            byte operation,
            @Nullable String recipeKey) {
        this.windowId = windowId;
        this.sessionToken = sessionToken;
        this.inputRevision = inputRevision;
        this.operation = operation;
        this.recipeKey = recipeKey;
    }

    public static RecipeSelectionMessage select(
            int windowId,
            int sessionToken,
            int inputRevision,
            String recipeKey) {
        if (recipeKey == null) {
            throw new NullPointerException("recipeKey");
        }
        if (!RecipeKey.isWireSafe(recipeKey)) {
            throw new IllegalArgumentException("recipeKey is not wire-safe");
        }
        return new RecipeSelectionMessage(
                windowId, sessionToken, inputRevision, OP_SELECT, recipeKey);
    }

    public static RecipeSelectionMessage clear(
            int windowId,
            int sessionToken,
            int inputRevision) {
        return new RecipeSelectionMessage(
                windowId, sessionToken, inputRevision, OP_CLEAR, null);
    }

    public static RecipeSelectionMessage query(
            int windowId,
            int sessionToken,
            int inputRevision) {
        return new RecipeSelectionMessage(
                windowId, sessionToken, inputRevision, OP_QUERY, null);
    }

    public int getWindowId() {
        return this.windowId;
    }

    public int getSessionToken() {
        return this.sessionToken;
    }

    public int getInputRevision() {
        return this.inputRevision;
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
        if (buf.readableBytes() < 13) {
            invalidate();
            return;
        }

        this.windowId = buf.readInt();
        this.sessionToken = buf.readInt();
        this.inputRevision = buf.readInt();
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
        buf.writeInt(this.inputRevision);
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
