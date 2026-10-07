package com.sosea1.retropolymorph.network;

import com.sosea1.retropolymorph.compat.IntegrationHealthRegistry;
import com.sosea1.retropolymorph.config.PolymorphConfig;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/** Opens a server-owned saved template in the RFTools ghost editor. */
public final class RFToolsEditorMessage implements IMessage {
    private int windowId;
    private int recipeIndex;

    public RFToolsEditorMessage() { }
    public RFToolsEditorMessage(int windowId, int recipeIndex) {
        this.windowId = windowId;
        this.recipeIndex = recipeIndex;
    }
    public void toBytes(ByteBuf buf) { buf.writeInt(this.windowId); buf.writeInt(this.recipeIndex); }
    public void fromBytes(ByteBuf buf) { this.windowId = buf.readInt(); this.recipeIndex = buf.readInt(); }

    public static final class Handler implements IMessageHandler<RFToolsEditorMessage, IMessage> {
        public IMessage onMessage(RFToolsEditorMessage message, MessageContext context) {
            EntityPlayerMP player = context.getServerHandler().player;
            if (!PolymorphConfig.isIntegrationRftoolsEnabled()
                    || message.recipeIndex < 0 || message.recipeIndex >= 64
                    || !RecipeSelectionHandler.allowRequest(player, false)) { return null; }
            player.getServerWorld().addScheduledTask(() -> {
                Container container = player.openContainer;
                if (container == null || container.windowId != message.windowId
                        || !container.canInteractWith(player)
                        || !container.getClass().getName().equals("mcjty.rftools.blocks.crafter.CrafterContainer")) { return; }
                try {
                    Object tile = container.getClass().getMethod("getCrafterTE").invoke(container);
                    int size = (Integer) tile.getClass().getMethod("getSupportedRecipes").invoke(tile);
                    if (message.recipeIndex >= size) { return; }
                    tile.getClass().getMethod("selectRecipe", int.class).invoke(tile, message.recipeIndex);
                    ((net.minecraft.tileentity.TileEntity) tile).markDirty();
                    container.detectAndSendChanges();
                } catch (ReflectiveOperationException exception) {
                    IntegrationHealthRegistry.recordBindingFailure("rftools", "openSavedTemplate", exception);
                }
            });
            return null;
        }
    }
}
