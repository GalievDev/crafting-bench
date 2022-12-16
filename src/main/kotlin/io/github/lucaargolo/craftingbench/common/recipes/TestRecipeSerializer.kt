package io.github.lucaargolo.craftingbench.common.recipes
/*
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

    fun fromJson(pRecipeId: Identifier?, jsonObject: JsonObject): T {
        val jsonIngredient: JsonElement =
            if (JsonHelper.hasArray(jsonObject, "ingredients")) JsonHelper.asArray(
                jsonObject,
                "ingredients"
            ) else JsonHelper.asArray(jsonObject, "ingredients")
        val ingredient: Ingredient = IngredientHelper.fromJson(jsonIngredient)
        val itemStack: ItemStack
        if (!jsonObject.has("result")) throw JsonSyntaxException("com.google.gson.JsonSyntaxException; " + TestRecipeSerializer::class.java + "; - have a message: \"RECIPE CANT BEEN CREATED! MISSING THIS ARGUMENT: 'result'\"")
        if (jsonObject["result"].isJsonObject) itemStack =
            ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(jsonObject, "result")) else {
            val string: String = GsonHelper.getAsString(jsonObject, "result")
            val location = Identifier(string)
            itemStack = ItemStack(Registry.ITEM.get(location))
        }
        val xp: Float = GsonHelper.getAsFloat(jsonObject, "xp", 0.0f)
        val time: Int = GsonHelper.getAsInt(jsonObject, "time", 0)
        return serializerFactory.create(pRecipeId, ingredient, itemStack, xp, time)
    }

    fun fromNetwork(pRecipeId: Identifier?, pBuffer: FriendlyByteBuf): T {
        val ingredient: Ingredient = Ingredient.fromNetwork(pBuffer)
        val itemStack: ItemStack = pBuffer.readItem()
        val xp: Float = pBuffer.readFloat()
        val time: Int = pBuffer.readInt()
        return serializerFactory.create(pRecipeId, ingredient, itemStack, xp, time)
    }

    fun toNetwork(pBuffer: FriendlyByteBuf, pRecipe: T) {
        pRecipe.ingredient.toNetwork(pBuffer)
        pBuffer.writeItem(pRecipe.result)
        pBuffer.writeFloat(pRecipe.experience)
        pBuffer.writeVarInt(pRecipe.time)
    }

    override fun read(id: Identifier?, json: JsonObject?): T {
        TODO("Not yet implemented")
    }

    override fun read(id: Identifier?, buf: PacketByteBuf?): T {
        TODO("Not yet implemented")
    }

    override fun write(buf: PacketByteBuf?, recipe: T) {
        TODO("Not yet implemented")
    }

    interface SerializerFactory<T : TestRecipe?> {
        fun create(id: Identifier?, ingredient: Ingredient?, result: ItemStack?, xp: Float, time: Int): T
    }
}*/
