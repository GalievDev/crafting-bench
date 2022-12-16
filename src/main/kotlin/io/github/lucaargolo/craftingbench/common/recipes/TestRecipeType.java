package io.github.lucaargolo.craftingbench.common.recipes;

import io.github.lucaargolo.craftingbench.CraftingBench;
import net.minecraft.recipe.RecipeType;

public class TestRecipeType<T extends TestRecipe> implements RecipeType<T> {
    private final String modId;
    private final String name;

    public TestRecipeType(String modId, String name) {
        this.modId = modId;
        this.name = name;
    }

    public TestRecipeType(String name) {
        this.modId = "";
        this.name = name;
    }

    @Override
    public String toString() {
        if (modId.isEmpty() || modId.equals(CraftingBench.MOD_ID)) {
            return CraftingBench.MOD_ID + ":" + name;
        }
        return modId + ":" + name;
    }
}
