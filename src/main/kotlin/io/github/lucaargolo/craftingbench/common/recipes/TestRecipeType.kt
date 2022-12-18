package io.github.lucaargolo.craftingbench.common.recipes

import io.github.lucaargolo.craftingbench.CraftingBench
import net.minecraft.recipe.RecipeType

class TestRecipeType<T : TestRecipe?> : RecipeType<T> {
    private val modId: String
    private val name: String

    constructor(modId: String, name: String) {
        this.modId = modId
        this.name = name
    }

    constructor(name: String) {
        modId = ""
        this.name = name
    }

    override fun toString(): String {
        return if (modId.isEmpty() || modId == CraftingBench.MOD_ID) {
            CraftingBench.MOD_ID + ":" + name
        } else "$modId:$name"
    }
}