package io.github.lucaargolo.craftingbench.common.recipes

import io.github.lucaargolo.craftingbench.CraftingBench.MOD_ID
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry
import java.util.function.Supplier

class RecipeReg {
    fun <T : TestRecipe> recipe(
        id: String?,
        factorySupplier: Supplier<TestRecipeSerializer.SerializerFactory<T>>
    ): TestRecipeSerializer<T> {
        val serializer: TestRecipeSerializer<T> = TestRecipeSerializer(factorySupplier.get())
        return Registry.register(Registry.RECIPE_SERIALIZER, Identifier(MOD_ID, id), serializer)
    }
}