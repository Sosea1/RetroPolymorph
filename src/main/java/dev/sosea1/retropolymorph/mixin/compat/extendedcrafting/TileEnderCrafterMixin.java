package dev.sosea1.retropolymorph.mixin.compat.extendedcrafting;

import dev.sosea1.retropolymorph.compat.extendedcrafting.EnderCrafterSelectionAccess;
import dev.sosea1.retropolymorph.config.PolymorphConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;

@Pseudo
@Mixin(targets = "com.blakebr0.extendedcrafting.tile.TileEnderCrafter", remap = false)
public abstract class TileEnderCrafterMixin implements EnderCrafterSelectionAccess {

    @Unique
    private static final String RETROPOLYMORPH_INDEX_TAG = "RetroPolymorphIndex";

    @Unique
    private static final String RETROPOLYMORPH_OUTPUT_TAG = "RetroPolymorphOutput";

    @Unique
    private int retropolymorph$enderCrafterRecipeIndex = -1;

    @Unique
    @Nullable
    private ItemStack retropolymorph$expectedOutput = null;

    @Override
    public int retropolymorph$getEnderCrafterRecipeIndex() {
        return this.retropolymorph$enderCrafterRecipeIndex;
    }

    @Override
    @Nullable
    public ItemStack retropolymorph$getExpectedOutput() {
        return this.retropolymorph$expectedOutput;
    }

    @Override
    public void retropolymorph$setEnderCrafterRecipe(int index, @Nullable ItemStack expectedOutput) {
        this.retropolymorph$enderCrafterRecipeIndex = index;
        this.retropolymorph$expectedOutput = expectedOutput == null || expectedOutput.isEmpty()
                ? null
                : expectedOutput.copy();
        if (((Object) this) instanceof TileEntity) {
            ((TileEntity) (Object) this).markDirty();
        }
    }

    @Inject(
            method = {"writeToNBT", "func_189515_b"},
            at = @At("RETURN"),
            remap = false,
            require = 0)
    private void retropolymorph$writeEnderRecipeSelection(
            NBTTagCompound tag,
            CallbackInfoReturnable<NBTTagCompound> cir) {
        if (!PolymorphConfig.isIntegrationExtendedCraftingEnabled()) {
            return;
        }
        NBTTagCompound target = cir.getReturnValue() == null ? tag : cir.getReturnValue();
        if (target == null) {
            return;
        }
        if (this.retropolymorph$enderCrafterRecipeIndex >= 0) {
            target.setInteger(RETROPOLYMORPH_INDEX_TAG, this.retropolymorph$enderCrafterRecipeIndex);
            if (this.retropolymorph$expectedOutput != null && !this.retropolymorph$expectedOutput.isEmpty()) {
                NBTTagCompound itemTag = new NBTTagCompound();
                this.retropolymorph$expectedOutput.writeToNBT(itemTag);
                target.setTag(RETROPOLYMORPH_OUTPUT_TAG, itemTag);
            } else {
                target.removeTag(RETROPOLYMORPH_OUTPUT_TAG);
            }
        } else {
            target.removeTag(RETROPOLYMORPH_INDEX_TAG);
            target.removeTag(RETROPOLYMORPH_OUTPUT_TAG);
        }
    }

    @Inject(
            method = {"readFromNBT", "func_145839_a"},
            at = @At("RETURN"),
            remap = false,
            require = 0)
    private void retropolymorph$readEnderRecipeSelection(NBTTagCompound tag, CallbackInfo ci) {
        if (!PolymorphConfig.isIntegrationExtendedCraftingEnabled()) {
            return;
        }
        if (tag != null && tag.hasKey(RETROPOLYMORPH_INDEX_TAG, 99)) {
            this.retropolymorph$enderCrafterRecipeIndex = tag.getInteger(RETROPOLYMORPH_INDEX_TAG);
            if (tag.hasKey(RETROPOLYMORPH_OUTPUT_TAG, 10)) {
                this.retropolymorph$expectedOutput = new ItemStack(tag.getCompoundTag(RETROPOLYMORPH_OUTPUT_TAG));
            } else {
                this.retropolymorph$expectedOutput = null;
            }
        } else {
            this.retropolymorph$enderCrafterRecipeIndex = -1;
            this.retropolymorph$expectedOutput = null;
        }
    }
}
