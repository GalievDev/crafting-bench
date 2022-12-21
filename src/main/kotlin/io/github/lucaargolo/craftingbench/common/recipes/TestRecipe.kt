package io.github.lucaargolo.craftingbench.common.recipes

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.*
import net.minecraft.util.Identifier
import net.minecraft.util.JsonHelper
import net.minecraft.util.collection.DefaultedList
import net.minecraft.util.registry.Registry
import net.minecraft.world.World


class TestRecipe(
    idIn: Identifier, tier: Int, experienceIn: Float, time: Int, ingredientIn: DefaultedList<Ingredient>,
    resultIn: ItemStack
) : Recipe<SimpleInventory> {
    private val id: Identifier
    private val tier: Int
    private val experience: Float
    private val time: Int
    private val ingredient: DefaultedList<Ingredient>
    private val result: ItemStack

    init {
        id = idIn
        this.tier = tier
        experience = experienceIn
        this.time = time
        ingredient = ingredientIn
        result = resultIn
    }

    override fun matches(pContainer: SimpleInventory?, pLevel: World?): Boolean {
        for (i in 0..pContainer?.size()!!) {
            if (ingredient[i].test(pContainer.getStack(i))) {
                return true
            }
        }
        return false
    }


    override fun getIngredients(): DefaultedList<Ingredient> {
        return ingredient
    }

    override fun getId(): Identifier {
        return id
    }

    override fun fits(width: Int, height: Int): Boolean {
        return true
    }

    override fun craft(inventory: SimpleInventory?): ItemStack {
        return result
    }

    override fun getOutput(): ItemStack {
        return result.copy()
    }



    override fun getType(): RecipeType<*> {
        return Type.INSTANCE
    }

    override fun getSerializer(): RecipeSerializer<*> {
        return Serializer.INSTANCE
    }

    override fun toString(): String {
        return "TestRecipe{" +
                "type=" + type +
                ", id=" + id +
                ", experience=" + experience +
                ", time=" + time +
                ", tier=" + tier +
                ", ingredient=" + ingredient +
                ", result=" + result +
                '}'
    }

    object Type : RecipeType<TestRecipe?> {
        val INSTANCE: Type = Type
        const val ID = "test"
    }

    class Serializer : RecipeSerializer<TestRecipe> {

        override fun read(id: Identifier, jsonObject: JsonObject): TestRecipe {
            val tier: Int = JsonHelper.getInt(jsonObject, "tier")
            val experience: Float = JsonHelper.getFloat(jsonObject, "xp")
            val time: Int = JsonHelper.getInt(jsonObject, "time")
            val jsonIngredient: JsonElement =
                if (JsonHelper.hasArray(jsonObject, "ingredients"))
                    JsonHelper.getArray(jsonObject, "ingredients")
                else JsonHelper.getArray(jsonObject, "ingredients")
            val ingredient = DefaultedList.ofSize(9, Ingredient.fromJson(jsonIngredient))
            if (!jsonObject.has("result"))
                throw JsonSyntaxException("com.google.gson.JsonSyntaxException; " + Serializer::class + "; - have a message: \"RECIPE CANT BEEN CREATED! MISSING THIS ARGUMENT: 'result'\"")
            val result: ItemStack =
                if (jsonObject["result"].isJsonObject) ShapedRecipe.outputFromJson(JsonHelper.getObject(jsonObject, "result")) else {
                    val string: String = JsonHelper.getString(jsonObject, "result")
                    val location = Identifier(string)
                    ItemStack(Registry.ITEM.get(location))
                }

            return TestRecipe(id, tier, experience, time, ingredient, result)
        }

        override fun read(id: Identifier, pBuffer: PacketByteBuf): TestRecipe {
            val tier: Int = pBuffer.readInt()
            val xp: Float = pBuffer.readFloat()
            val time: Int = pBuffer.readInt()
            val ingredient = DefaultedList.ofSize(pBuffer.readInt(), Ingredient.fromPacket(pBuffer))
            val itemStack: ItemStack = pBuffer.readItemStack()
            return TestRecipe(id, tier,  xp, time, ingredient, itemStack)
        }

        override fun write(pBuffer: PacketByteBuf, pRecipe: TestRecipe) {
            pBuffer.writeVarInt(pRecipe.tier)
            pBuffer.writeFloat(pRecipe.experience)
            pBuffer.writeVarInt(pRecipe.time)
            for (ing in pRecipe.ingredients) {
                ing.write(pBuffer)
            }
            pBuffer.writeItemStack(pRecipe.result)
        }

        companion object {
            val INSTANCE = Serializer()
            const val ID = "test"
        }
    }
}