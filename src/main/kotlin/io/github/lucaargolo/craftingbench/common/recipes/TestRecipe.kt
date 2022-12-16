package io.github.lucaargolo.craftingbench.common.recipes

import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.Recipe
import net.minecraft.recipe.RecipeType
import net.minecraft.util.Identifier
import net.minecraft.util.collection.DefaultedList
import net.minecraft.world.World


abstract class TestRecipe(
    idIn: Identifier, ingredientIn: DefaultedList<Ingredient>,
    resultIn: ItemStack, experienceIn: Float, time: Int
) : Recipe<SimpleInventory> {
    private val id: Identifier
    private val ingredient: DefaultedList<Ingredient>
    val result: ItemStack
    private val experience: Float
    private val time: Int

    init {
        id = idIn
        ingredient = ingredientIn
        result = resultIn
        experience = experienceIn
        this.time = time
    }

    override fun matches(pContainer: SimpleInventory?, pLevel: World?): Boolean {
        for (i in 0..pContainer?.size()!!) {
            if (ingredient[i].test(pContainer.getStack(i))) {
                return true
            }
        }
        return false
    }


    fun assemble(pContainer: SimpleInventory?): ItemStack {
        return result.copy()
    }

    fun canCraftInDimensions(pWidth: Int, pHeight: Int): Boolean {
        return true
    }

    val resultItem: ItemStack
        get() = result.copy()

    override fun getIngredients(): DefaultedList<Ingredient> {
        return ingredient
    }

    override fun getId(): Identifier {
        return id
    }

    override fun getType(): RecipeType<*> {
        return type
    }

    override fun toString(): String {
        return "TestRecipe{" +
                "type=" + type +
                ", id=" + id +
                ", ingredient=" + ingredient +
                ", result=" + result +
                ", experience=" + experience +
                ", time=" + time +
                '}'
    }
}