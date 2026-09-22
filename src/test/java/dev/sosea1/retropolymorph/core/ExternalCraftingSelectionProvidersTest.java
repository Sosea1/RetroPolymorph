package dev.sosea1.retropolymorph.core;

import dev.sosea1.retropolymorph.core.ExternalCraftingSelectionProvider.Precedence;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExternalCraftingSelectionProvidersTest {

    private static final ResourceLocation RECIPE_A = new ResourceLocation("test", "recipe_a");
    private static final ResourceLocation RECIPE_B = new ResourceLocation("test", "recipe_b");
    private static final ResourceLocation RECIPE_STATE = new ResourceLocation("test", "recipe_state");

    @BeforeEach
    @AfterEach
    void cleanUp() {
        ExternalCraftingSelectionProviders.resetForTests();
    }

    private static final class StubProvider implements ExternalCraftingSelectionProvider {
        private final Precedence precedence;
        private final ResourceLocation selection;
        private final boolean shouldClear;
        private final AtomicInteger outputResolvedCount = new AtomicInteger();

        StubProvider(Precedence precedence, @Nullable ResourceLocation selection, boolean shouldClear) {
            this.precedence = precedence;
            this.selection = selection;
            this.shouldClear = shouldClear;
        }

        @Override
        public Precedence getPrecedence() {
            return this.precedence;
        }

        @Nullable
        @Override
        public ResourceLocation getSelectedRecipeId(InventoryCrafting matrix, @Nullable Container owner) {
            return this.selection;
        }

        @Override
        public void onRecipeOutputResolved(InventoryCrafting matrix, @Nullable Container owner, IRecipe recipe) {
            this.outputResolvedCount.incrementAndGet();
        }

        @Override
        public boolean shouldClearStateOnEmpty(InventoryCrafting matrix, @Nullable Container owner) {
            return this.shouldClear;
        }
    }

    @Test
    void testAuthoritativeProviderOverridesExistingSelection() {
        StubProvider authoritative = new StubProvider(Precedence.AUTHORITATIVE, RECIPE_A, false);
        ExternalCraftingSelectionProviders.register(authoritative);

        RecipeSelectionState state = new RecipeSelectionState();
        state.select(RECIPE_STATE);

        ResourceLocation result = ExternalCraftingSelectionProviders.getSelectedRecipeId(null, null, state);
        assertEquals(RECIPE_A, result, "Authoritative provider must override existing matrix state");
    }

    @Test
    void testFallbackProviderDoesNotOverrideExistingSelection() {
        StubProvider fallback = new StubProvider(Precedence.FALLBACK_WHEN_EMPTY, RECIPE_B, false);
        ExternalCraftingSelectionProviders.register(fallback);

        RecipeSelectionState state = new RecipeSelectionState();
        state.select(RECIPE_STATE);

        ResourceLocation result = ExternalCraftingSelectionProviders.getSelectedRecipeId(null, null, state);
        assertNull(result, "Fallback provider must yield when matrix already has an active selection");
    }

    @Test
    void testFallbackProviderAppliesWhenStateIsEmpty() {
        StubProvider fallback = new StubProvider(Precedence.FALLBACK_WHEN_EMPTY, RECIPE_B, false);
        ExternalCraftingSelectionProviders.register(fallback);

        RecipeSelectionState emptyState = new RecipeSelectionState();
        ResourceLocation resultWithEmptyState = ExternalCraftingSelectionProviders.getSelectedRecipeId(null, null, emptyState);
        assertEquals(RECIPE_B, resultWithEmptyState, "Fallback provider must apply when state has no selection");

        ResourceLocation resultWithNullState = ExternalCraftingSelectionProviders.getSelectedRecipeId(null, null, null);
        assertEquals(RECIPE_B, resultWithNullState, "Fallback provider must apply when state is null");
    }

    @Test
    void testAuthoritativeTakesPrecedenceOverFallbackRegardlessOfRegistrationOrder() {
        // Even if fallback provider is registered first (e.g. RSB before AE2 in bootstrap)
        StubProvider fallback = new StubProvider(Precedence.FALLBACK_WHEN_EMPTY, RECIPE_B, false);
        StubProvider authoritative = new StubProvider(Precedence.AUTHORITATIVE, RECIPE_A, false);

        ExternalCraftingSelectionProviders.register(fallback);
        ExternalCraftingSelectionProviders.register(authoritative);

        ResourceLocation result = ExternalCraftingSelectionProviders.getSelectedRecipeId(null, null, null);
        assertEquals(RECIPE_A, result, "Authoritative provider must win over fallback provider regardless of registration order");
    }

    @Test
    void testRegistrationOrderWithinSamePrecedenceTier() {
        StubProvider first = new StubProvider(Precedence.AUTHORITATIVE, RECIPE_A, false);
        StubProvider second = new StubProvider(Precedence.AUTHORITATIVE, RECIPE_B, false);

        ExternalCraftingSelectionProviders.register(first);
        ExternalCraftingSelectionProviders.register(second);

        ResourceLocation result = ExternalCraftingSelectionProviders.getSelectedRecipeId(null, null, null);
        assertEquals(RECIPE_A, result, "First registered provider in tier must win");
    }

    @Test
    void testNotifyOutputResolvedDispatchesToAllProviders() {
        StubProvider provider1 = new StubProvider(Precedence.AUTHORITATIVE, RECIPE_A, false);
        StubProvider provider2 = new StubProvider(Precedence.FALLBACK_WHEN_EMPTY, RECIPE_B, false);

        ExternalCraftingSelectionProviders.register(provider1);
        ExternalCraftingSelectionProviders.register(provider2);

        ExternalCraftingSelectionProviders.notifyOutputResolved(null, null, null);

        assertEquals(1, provider1.outputResolvedCount.get());
        assertEquals(1, provider2.outputResolvedCount.get());
    }

    @Test
    void testShouldClearStateOnEmptyDispatchesToAllProviders() {
        StubProvider provider1 = new StubProvider(Precedence.AUTHORITATIVE, null, false);
        StubProvider provider2 = new StubProvider(Precedence.FALLBACK_WHEN_EMPTY, null, true);

        ExternalCraftingSelectionProviders.register(provider1);
        ExternalCraftingSelectionProviders.register(provider2);

        assertTrue(ExternalCraftingSelectionProviders.shouldClearStateOnEmpty(null, null));
    }

    @Test
    void testDuplicateRegistrationIgnoredAndUnregisterWorks() {
        StubProvider provider = new StubProvider(Precedence.AUTHORITATIVE, RECIPE_A, false);
        ExternalCraftingSelectionProviders.register(provider);
        ExternalCraftingSelectionProviders.register(provider); // duplicate

        ExternalCraftingSelectionProviders.notifyOutputResolved(null, null, null);
        assertEquals(1, provider.outputResolvedCount.get());

        ExternalCraftingSelectionProviders.unregister(provider);
        assertNull(ExternalCraftingSelectionProviders.getSelectedRecipeId(null, null));
    }
}
