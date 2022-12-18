package io.github.lucaargolo.craftingbench.common.recipes

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import com.sun.org.apache.xml.internal.serializer.SerializerFactory
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
    idIn: Identifier, ingredientIn: DefaultedList<Ingredient>,
    resultIn: ItemStack, experienceIn: Float, time: Int
) : Recipe<SimpleInventory> {
    private val id: Identifier
    private val ingredient: DefaultedList<Ingredient>
    val result: ItemStack
    val experience: Float
    val time: Int

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
                ", ingredient=" + ingredient +
                ", result=" + result +
                ", experience=" + experience +
                ", time=" + time +
                '}'
    }

    object Type : RecipeType<TestRecipe?> {
        val INSTANCE: Type = Type
        const val ID = "test"
    }

    class Serializer : RecipeSerializer<TestRecipe> {

        override fun read(id: Identifier, jsonObject: JsonObject): TestRecipe {
            val jsonIngredient: JsonElement =
                if (JsonHelper.hasArray(jsonObject, "ingredients")) JsonHelper.asArray(
                    jsonObject,
                    "ingredients"
                ) else JsonHelper.asArray(jsonObject, "ingredients")
            val ingredient = DefaultedList.ofSize(9, Ingredient.fromJson(jsonIngredient))
            if (!jsonObject.has("result")) throw JsonSyntaxException("com.google.gson.JsonSyntaxException; " + TestRecipeSerializer::class.java + "; - have a message: \"RECIPE CANT BEEN CREATED! MISSING THIS ARGUMENT: 'result'\"")
            val result: ItemStack =
                if (jsonObject["result"].isJsonObject) ShapedRecipe.outputFromJson(JsonHelper.asObject(jsonObject, "result")) else {
                    val string: String = JsonHelper.asString(jsonObject, "result")
                    val location = Identifier(string)
                    ItemStack(Registry.ITEM.get(location))
                }
            val experience: Float = JsonHelper.asFloat(jsonObject, "xp")
            val time: Int = JsonHelper.asInt(jsonObject, "time")

            return TestRecipe(id, ingredient, result, experience, time)
        }

        override fun read(id: Identifier, pBuffer: PacketByteBuf): TestRecipe {
            val ingredient = DefaultedList.ofSize(pBuffer.readInt(), Ingredient.fromPacket(pBuffer))
            val itemStack: ItemStack = pBuffer.readItemStack()
            val xp: Float = pBuffer.readFloat()
            val time: Int = pBuffer.readInt()
            return TestRecipe(id, ingredient, itemStack, xp, time)
        }

        override fun write(pBuffer: PacketByteBuf, pRecipe: TestRecipe) {
            for (ing in pRecipe.ingredients) {
                ing.write(pBuffer)
            }
            pBuffer.writeItemStack(pRecipe.result)
            pBuffer.writeFloat(pRecipe.experience)
            pBuffer.writeVarInt(pRecipe.time)
        }

        companion object {
            val INSTANCE = Serializer()
            const val ID = "test"
        }
    }
}