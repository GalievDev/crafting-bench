package io.github.lucaargolo.craftingbench.common.recipes

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonSyntaxException
import net.minecraft.item.ItemStack
import net.minecraft.network.PacketByteBuf
import net.minecraft.recipe.Ingredient
import net.minecraft.recipe.RecipeSerializer
import net.minecraft.recipe.ShapedRecipe
import net.minecraft.util.Identifier
import net.minecraft.util.JsonHelper
import net.minecraft.util.registry.Registry


class TestRecipeSerializer<T : TestRecipe?>(private val serializerFactory: SerializerFactory<T>) :
    RecipeSerializer<T> {

    override fun read(pRecipeId: Identifier?, jsonObject: JsonObject): T {
        val jsonIngredient: JsonElement =
            if (JsonHelper.hasArray(jsonObject, "ingredients")) JsonHelper.asArray(
                jsonObject,
                "ingredients"
            ) else JsonHelper.asArray(jsonObject, "ingredients")
        val ingredient: Ingredient = Ingredient.fromJson(jsonIngredient)
        val itemStack: ItemStack
        if (!jsonObject.has("result")) throw JsonSyntaxException("com.google.gson.JsonSyntaxException; " + TestRecipeSerializer::class.java + "; - have a message: \"RECIPE CANT BEEN CREATED! MISSING THIS ARGUMENT: 'result'\"")
        if (jsonObject["result"].isJsonObject) itemStack =
            ShapedRecipe.outputFromJson(JsonHelper.asObject(jsonObject, "result")) else {
            val string: String = JsonHelper.asString(jsonObject, "result")
            val location = Identifier(string)
            itemStack = ItemStack(Registry.ITEM.get(location))
        }
        val xp: Float = JsonHelper.asFloat(jsonObject, "xp")
        val time: Int = JsonHelper.asInt(jsonObject, "time")
        return serializerFactory.create(pRecipeId, ingredient, itemStack, xp, time)
    }

    override fun read(pRecipeId: Identifier?, pBuffer: PacketByteBuf): T {
        val ingredient: Ingredient = Ingredient.fromPacket(pBuffer)
        val itemStack: ItemStack = pBuffer.readItemStack()
        val xp: Float = pBuffer.readFloat()
        val time: Int = pBuffer.readInt()
        return serializerFactory.create(pRecipeId, ingredient, itemStack, xp, time)
    }

    override fun write(pBuffer: PacketByteBuf, pRecipe: T) {
        for (ing in pRecipe?.ingredients!!) {
            ing.write(pBuffer)
        }
        pBuffer.writeItemStack(pRecipe.result)
        pBuffer.writeFloat(pRecipe.experience)
        pBuffer.writeVarInt(pRecipe.time)
    }

    interface SerializerFactory<T : TestRecipe?> {
        fun create(id: Identifier?, ingredient: Ingredient?, result: ItemStack?, xp: Float, time: Int): T
    }
}
