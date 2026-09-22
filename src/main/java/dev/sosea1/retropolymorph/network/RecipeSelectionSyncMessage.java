package dev.sosea1.retropolymorph.network;

import dev.sosea1.retropolymorph.api.RecipeKey;
import dev.sosea1.retropolymorph.api.RecipeOption;
import dev.sosea1.retropolymorph.api.RecipeOptions;
import dev.sosea1.retropolymorph.api.SelectionReason;
import io.netty.buffer.ByteBuf;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Server-authoritative selection state and visible recipe options. */
public final class RecipeSelectionSyncMessage implements IMessage {

    private int windowId;
    private int sessionToken;
    private int inputRevision;
    private boolean accepted;
    private boolean valid = true;
    private SelectionReason reason = SelectionReason.NATIVE_DEFAULT;

    @Nullable
    private String selectedRecipeKey;

    private List<RecipeOption> options = Collections.emptyList();

    public RecipeSelectionSyncMessage() {
    }

    RecipeSelectionSyncMessage(
            int windowId,
            int sessionToken,
            int inputRevision,
            boolean accepted,
            @Nullable String selectedRecipeKey,
            List<RecipeOption> options,
            SelectionReason reason) {
        if (selectedRecipeKey != null && !RecipeKey.isWireSafe(selectedRecipeKey)) {
            throw new IllegalArgumentException("selectedRecipeKey is not wire-safe");
        }
        this.windowId = windowId;
        this.sessionToken = sessionToken;
        this.inputRevision = inputRevision;
        this.accepted = accepted;
        this.selectedRecipeKey = selectedRecipeKey;
        this.options = RecipeOptions.sanitizeAndLimit(options, selectedRecipeKey);
        this.reason = reason == null ? SelectionReason.NATIVE_DEFAULT : reason;
    }

    RecipeSelectionSyncMessage(
            int windowId,
            int sessionToken,
            int inputRevision,
            boolean accepted,
            @Nullable String selectedRecipeKey,
            List<RecipeOption> options) {
        this(windowId, sessionToken, inputRevision, accepted, selectedRecipeKey, options, SelectionReason.NATIVE_DEFAULT);
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

    public List<RecipeOption> getOptions() {
        return this.options;
    }

    public SelectionReason getReason() {
        return this.reason;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        try {
            if (buf.readableBytes() < 16) {
                invalidate();
                return;
            }

            this.windowId = buf.readInt();
            this.sessionToken = buf.readInt();
            this.inputRevision = buf.readInt();
            this.accepted = buf.readBoolean();
            this.reason = SelectionReason.fromWireId(buf.readUnsignedByte());
            this.valid = true;

            boolean hasSelection = buf.readBoolean();
            if (hasSelection) {
                this.selectedRecipeKey = NetworkStringCodec.read(
                        buf, RecipeKey.MAX_UTF8_BYTES);
                if (!RecipeKey.isWireSafe(this.selectedRecipeKey)) {
                    invalidate();
                    return;
                }
            } else {
                this.selectedRecipeKey = null;
            }

            int optionCount = buf.readUnsignedByte();
            if (optionCount > RecipeOptions.MAX_SERVER_OPTIONS) {
                invalidate();
                return;
            }

            if (optionCount == 0) {
                this.options = Collections.emptyList();
            } else {
                ArrayList<RecipeOption> decoded = new ArrayList<RecipeOption>(optionCount);
                for (int index = 0; index < optionCount; index++) {
                    String key = NetworkStringCodec.read(buf, RecipeKey.MAX_UTF8_BYTES);
                    ItemStack output = ByteBufUtils.readItemStack(buf);
                    if (!RecipeKey.isWireSafe(key) || output == null || output.isEmpty()) {
                        invalidate();
                        return;
                    }
                    decoded.add(new RecipeOption(key, output));
                }
                this.options = Collections.unmodifiableList(decoded);
            }

            if (buf.isReadable()) {
                invalidate();
            }
        } catch (RuntimeException exception) {
            invalidate();
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(this.windowId);
        buf.writeInt(this.sessionToken);
        buf.writeInt(this.inputRevision);
        buf.writeBoolean(this.accepted);
        buf.writeByte(this.reason != null ? this.reason.getWireId() : SelectionReason.NATIVE_DEFAULT.getWireId());

        String encoded = this.selectedRecipeKey;
        buf.writeBoolean(encoded != null);
        if (encoded != null) {
            if (encoded.isEmpty()) {
                throw new IllegalStateException("Selected recipe key must not be empty");
            }
            NetworkStringCodec.write(buf, encoded, RecipeKey.MAX_UTF8_BYTES);
        }

        List<RecipeOption> safe = RecipeOptions.sanitizeAndLimit(this.options, encoded);
        buf.writeByte(safe.size());
        for (RecipeOption option : safe) {
            NetworkStringCodec.write(buf, option.getRecipeKey(), RecipeKey.MAX_UTF8_BYTES);
            ByteBufUtils.writeItemStack(buf, option.getOutput());
        }
    }

    private void invalidate() {
        this.valid = false;
        this.selectedRecipeKey = null;
        this.options = Collections.emptyList();
        this.reason = SelectionReason.NATIVE_DEFAULT;
    }
}
