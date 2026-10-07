package com.sosea1.retropolymorph.mixin.compat.rftools;

import com.sosea1.retropolymorph.api.RecipeKey;
import com.sosea1.retropolymorph.compat.IntegrationHealthRegistry;
import com.sosea1.retropolymorph.compat.rftools.RFToolsPacketSelectionAccess;
import com.sosea1.retropolymorph.compat.rftools.RFToolsRecipeSelectionAccess;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import javax.annotation.Nullable;

@Pseudo
@Mixin(targets = "mcjty.rftools.blocks.crafter.PacketCrafter", remap = false)
public abstract class RFToolsPacketCrafterMixin implements RFToolsPacketSelectionAccess {
    @Shadow private int recipeIndex;
    @Unique private ResourceLocation retropolymorph$recipeId;

    public void retropolymorph$setRecipeId(@Nullable ResourceLocation id) { this.retropolymorph$recipeId = id; }

    @Inject(method = "toBytes", at = @At("RETURN"), require = 1)
    private void retropolymorph$writeSelection(ByteBuf buf, CallbackInfo ci) {
        new PacketBuffer(buf).writeString(this.retropolymorph$recipeId == null ? "" : this.retropolymorph$recipeId.toString());
    }

    @Inject(method = "fromBytes", at = @At("RETURN"), require = 1)
    private void retropolymorph$readSelection(ByteBuf buf, CallbackInfo ci) {
        this.retropolymorph$recipeId = buf.isReadable()
                ? RecipeKey.parseForgeId(new PacketBuffer(buf).readString(256)) : null;
    }

    @Inject(method = "updateRecipe", at = @At(value = "INVOKE",
            target = "Lmcjty/rftools/blocks/crafter/CrafterBaseTE;selectRecipe(I)V"), require = 1)
    private void retropolymorph$commitSelection(@Coerce Object owner, CallbackInfo ci) {
        if (!(owner instanceof TileEntity)) { return; }
        TileEntity tile = (TileEntity) owner;
        try {
            Object saved = owner.getClass().getMethod("getRecipe", int.class).invoke(owner, this.recipeIndex);
            if (saved instanceof RFToolsRecipeSelectionAccess) {
                ((RFToolsRecipeSelectionAccess) saved)
                        .retropolymorph$setSelectedRecipeId(this.retropolymorph$recipeId, tile.getWorld());
            }
        } catch (ReflectiveOperationException exception) {
            IntegrationHealthRegistry.recordBindingFailure("rftools", "commitRecipe", exception);
        }
    }
}
