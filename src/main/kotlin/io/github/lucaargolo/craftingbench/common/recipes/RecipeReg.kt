package io.github.lucaargolo.craftingbench.common.recipes

import io.github.lucaargolo.craftingbench.CraftingBench.MOD_ID
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.RecipeType
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

object RecipeReg {
    fun registerRecipes() {
        Registry.register<RecipeSerializer<*>, RecipeSerializer<*>>(
            Registry.RECIPE_SERIALIZER, Identifier(MOD_ID, TestRecipe.Serializer.ID),
            TestRecipe.Serializer.INSTANCE
        )
        Registry.register<RecipeType<*>, RecipeType<*>>(
            Registry.RECIPE_TYPE, Identifier(MOD_ID, TestRecipe.Type.ID),
            TestRecipe.Type.INSTANCE
        )
    }
}