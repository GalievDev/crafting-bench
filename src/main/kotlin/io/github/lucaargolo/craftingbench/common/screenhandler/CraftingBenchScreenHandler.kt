package io.github.lucaargolo.craftingbench.common.screenhandler

import io.github.lucaargolo.craftingbench.CraftingBench
import io.github.lucaargolo.craftingbench.client.CraftingBenchClient
import io.github.lucaargolo.craftingbench.client.screen.CraftingBenchScreen
import io.github.lucaargolo.craftingbench.common.recipes.TestRecipe
import io.github.lucaargolo.craftingbench.utils.RecipeTree
import it.unimi.dsi.fastutil.ints.Int2IntArrayMap
import it.unimi.dsi.fastutil.ints.IntList
import net.minecraft.client.MinecraftClient
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.inventory.CraftingInventory
import net.minecraft.inventory.CraftingResultInventory
import net.minecraft.inventory.Inventory
import net.minecraft.inventory.SimpleInventory
import net.minecraft.item.ItemStack
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket
import net.minecraft.screen.ScreenHandler
import net.minecraft.screen.ScreenHandlerContext
import net.minecraft.screen.ScreenHandlerListener
import net.minecraft.screen.slot.Slot
import net.minecraft.server.network.ServerPlayerEntity
import net.minecraft.util.registry.Registry
import net.minecraft.world.World
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

class CraftingBenchScreenHandler(syncId: Int, private val playerInventory: PlayerInventory, val inventory: SimpleInventory, private val context: ScreenHandlerContext) : ScreenHandler(ScreenHandlerCompendium.CRAFTING_BENCH, syncId) {

    constructor(syncId: Int, playerInventory: PlayerInventory, context: ScreenHandlerContext): this(syncId, playerInventory, SimpleInventory(28), context)

    private val recipeFinder = RecipeFinder()

    val craftableRecipes = mutableMapOf<TestRecipe, Pair<List<TestRecipe>, List<IntList>>>()
    val combinedInventory = object: Inventory {
        override fun clear() {
            playerInventory.clear()
            inventory.clear()
        }

        override fun size(): Int {
            return playerInventory.size()+inventory.size()
        }

        override fun isEmpty(): Boolean {
            return playerInventory.isEmpty && inventory.isEmpty
        }

        override fun getStack(slot: Int): ItemStack {
            return when {
                slot < playerInventory.size() -> playerInventory.getStack(slot)
                else -> inventory.getStack(slot-playerInventory.size())
            }
        }

        override fun removeStack(slot: Int, amount: Int): ItemStack {
            return when {
                slot < playerInventory.size() -> playerInventory.removeStack(slot, amount)
                else -> inventory.removeStack(slot-playerInventory.size(), amount)
            }
        }

        override fun removeStack(slot: Int): ItemStack {
            return when {
                slot < playerInventory.size() -> playerInventory.removeStack(slot)
                else -> inventory.removeStack(slot-playerInventory.size())
            }
        }

        override fun setStack(slot: Int, stack: ItemStack) {
            when {
                slot < playerInventory.size() -> playerInventory.setStack(slot, stack)
                else -> inventory.setStack(slot-playerInventory.size(), stack)
            }
        }

        override fun markDirty() {
            playerInventory.markDirty()
            inventory.markDirty()
        }

        override fun canPlayerUse(player: PlayerEntity): Boolean {
            return playerInventory.canPlayerUse(player) && inventory.canPlayerUse(player)
        }

    }

    //??????
    fun getTier(): Int {
        return 1
    }

    init {
        addListener(object: ScreenHandlerListener {
            override fun onSlotUpdate(handler: ScreenHandler, slotId: Int, stack: ItemStack) {
                if(slotId in (1..9)) onContentChanged(null)
            }
            override fun onPropertyUpdate(handler: ScreenHandler?, property: Int, value: Int) = Unit
        })

        repeat(4) { n ->
            repeat(7) { m ->
                addSlot(Slot(inventory, m + n * 7, 184 + 105 + m * 18, 84 + n * 18))
            }
        }

        repeat(3) { n ->
            repeat(9) { m ->
                addSlot(Slot(playerInventory, m + n * 9 + 9, 8 + 105 + m * 18, 84 + n * 18))
            }
        }

        repeat(9) { n ->
            addSlot(Slot(playerInventory, n, 8 + 105 + n * 18, 142))
        }

        onContentChanged(null)
    }

    override fun updateSlotStacks(revision: Int, stacks: MutableList<ItemStack>?, cursorStack: ItemStack?) {
        super.updateSlotStacks(revision, stacks, cursorStack)
        val player = playerInventory.player
        if(player.world.isClient) {
            (playerInventory.player as? ClientPlayerEntity)?.also(::populateRecipes)
        }
    }

    override fun transferSlot(player: PlayerEntity?, index: Int): ItemStack {
        var itemStack = ItemStack.EMPTY
        val slot = slots[index]
        if (slot.hasStack()) {
            val itemStack2 = slot.stack
            itemStack = itemStack2.copy()
            if (index == 0) {
                context.run { world, _ -> itemStack2.item.onCraft(itemStack2, world, player) }
                if (!insertItem(itemStack2, 10, slots.size, true)) {
                    return ItemStack.EMPTY
                }
                slot.onQuickTransfer(itemStack2, itemStack)
            }else if(index < 10) {
                if (!insertItem(itemStack2, 10, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            }else if (index < 38) {
                if (!insertItem(itemStack2, 38, slots.size, true)) {
                    return ItemStack.EMPTY
                }
            } else if (!insertItem(itemStack2, 10, 38, false)) {
                return ItemStack.EMPTY
            }
            if (itemStack2.isEmpty) {
                slot.stack = ItemStack.EMPTY
            } else {
                slot.markDirty()
            }

            if (itemStack2.count == itemStack.count) {
                return ItemStack.EMPTY
            }

            slot.onTakeItem(player, itemStack2)
            if (index == 0) {
                player?.dropItem(itemStack2, false)
            }
        }
        return itemStack
    }

    override fun canUse(player: PlayerEntity): Boolean {
        return context.get({ _, pos ->
            player.squaredDistanceTo(pos.x.toDouble() + 0.5, pos.y.toDouble() + 0.5, pos.z.toDouble() + 0.5) <= 64.0
       }, true)
    }

    override fun close(player: PlayerEntity) {
        super.close(player)
        recipeFinder.stop()
    }

    private fun populateRecipes(player: ClientPlayerEntity) {
        recipeFinder.rawItemInv.clear()
        recipeFinder.rawItemInv.defaultReturnValue(0)
        repeat(player.inventory.size()) { slot ->
            val stack = player.inventory.getStack(slot)
            val itemId = Registry.ITEM.getRawId(stack.item)
            recipeFinder.rawItemInv.put(itemId, recipeFinder.rawItemInv.get(itemId)+stack.count)
            CraftingBench.LOGGER.info("Population recipe $stack $itemId")
        }
        repeat(inventory.size()) { slot ->
            val stack = inventory.getStack(slot)
            val itemId = Registry.ITEM.getRawId(stack.item)
            recipeFinder.rawItemInv.put(itemId, recipeFinder.rawItemInv.get(itemId)+stack.count)
        }
        recipeFinder.start()
    }

    class RecipeFinder: Runnable {

        private var worker: Thread? = null
        val rawItemInv = Int2IntArrayMap()

        private val stateIndex = AtomicInteger(0)
        private var state: State
            get() = State.values()[stateIndex.get()]
            set(value) = stateIndex.set(value.ordinal)

        fun start() {
            if(worker == null) {
                CraftingBench.LOGGER.info("[Crafting Bench] Starting Recipe Finder")
                state = State.RUNNING
                worker = Thread(this, "Recipe finder").also(Thread::start)
            }else{
                CraftingBench.LOGGER.info("[Crafting Bench] Restarting Recipe Finder")
                state = State.RESTARTING
            }
        }

        fun stop() {
            if(worker != null) {
                CraftingBench.LOGGER.info("[Crafting Bench] Stopping Recipe Finder")
                state = State.STOPPING
                worker?.interrupt()
                worker = null
            }
        }

        override fun run() {
            val newCraftableRecipes = mutableMapOf<TestRecipe, Pair<List<TestRecipe>, List<IntList>>>()
            val possibleRecipeTrees = mutableSetOf<RecipeTree>()

            if(state == State.RESTARTING) {
                state = State.RUNNING
            }

            val time = measureTimeMillis {
                rawItemInv.keys.forEach { item ->
                    CraftingBenchClient.itemToRecipeTrees[item]?.forEach { nextRecipeTree ->
                        val requiredIngredients = nextRecipeTree.recipe.ingredients.toMutableList()
                        val requiredIngredientsIterator = requiredIngredients.iterator()
                        while (requiredIngredientsIterator.hasNext()) {
                            val requiredIngredient = requiredIngredientsIterator.next()
                            var found = requiredIngredient.isEmpty
                            requiredIngredient.matchingItemIds.forEach testIngredient@{ item ->
                                if(rawItemInv.containsKey(item)) {
                                    found = true
                                    return@testIngredient
                                }
                            }
                            if(found) {
                                requiredIngredientsIterator.remove()
                            }
                        }
                        if(requiredIngredients.isEmpty()) {
                            possibleRecipeTrees.add(nextRecipeTree)
                        }
                    }
                }
                val iterator = possibleRecipeTrees.toMutableList().listIterator()
                while(state == State.RUNNING && (iterator.hasNext() || iterator.hasPrevious())) {
                    val recipeTree = if(iterator.hasNext()) iterator.next() else iterator.previous()
                    iterator.remove()
                    val recipe = recipeTree.recipe
                    recipeTree.getBranches(possibleRecipeTrees, 0).forEach { branch ->
                        val recipeHistory = branch.recipeHistory
                        val recipeIngredients = branch.ingredients
                        val rawItemInvCopy = rawItemInv.clone()
                        val ingredientsClone = recipeIngredients.toMutableList()
                        val ingredientsIterator = ingredientsClone.iterator()
                        while (ingredientsIterator.hasNext()) {
                            val ingredient = ingredientsIterator.next()
                            var found = ingredient.isEmpty()
                            ingredient.forEach ingredientTest@{ itemId ->
                                val itemQnt = rawItemInvCopy.get(itemId)
                                if (itemQnt > 0) {
                                    rawItemInvCopy.put(itemId, itemQnt - 1)
                                    found = true
                                    return@ingredientTest
                                }
                            }
                            if (found) {
                                ingredientsIterator.remove()
                            } else {
                                break
                            }
                        }
                        if (ingredientsClone.isEmpty()) {
                            val nextRecipeTrees = CraftingBenchClient.itemToRecipeTrees[Registry.ITEM.getRawId(recipe.output.item)] ?: emptyList()
                            nextRecipeTrees.forEach { nextRecipeTree ->
                                val requiredIngredients = nextRecipeTree.recipe.ingredients.toMutableList()
                                val requiredIngredientsIterator = requiredIngredients.iterator()
                                while (requiredIngredientsIterator.hasNext()) {
                                    val requiredIngredient = requiredIngredientsIterator.next()
                                    var found = requiredIngredient.isEmpty
                                    if (requiredIngredient.test(recipe.output)) {
                                        found = true
                                    }
                                    if (!found) {
                                        requiredIngredient.matchingItemIds.forEach testIngredient@{ item ->
                                            if (rawItemInvCopy.containsKey(item)) {
                                                found = true
                                                return@testIngredient
                                            }
                                        }
                                    }
                                    if (!found) {
                                        possibleRecipeTrees.forEach testRecipeTrees@{ recipeTree ->
                                            if (requiredIngredient.test(recipeTree.recipe.output)) {
                                                found = true
                                                return@testRecipeTrees
                                            }
                                        }
                                    }
                                    if (!found) {
                                        nextRecipeTrees.forEach testRecipeTrees@{ recipeTree ->
                                            if (requiredIngredient.test(recipeTree.recipe.output)) {
                                                found = true
                                                return@testRecipeTrees
                                            }
                                        }
                                    }
                                    if (found) {
                                        requiredIngredientsIterator.remove()
                                    }
                                }
                                if (requiredIngredients.isEmpty() && possibleRecipeTrees.add(nextRecipeTree)) {
                                    iterator.add(nextRecipeTree)
                                }

                            }
                            if (!newCraftableRecipes.contains(recipe) || newCraftableRecipes[recipe]!!.first.size > recipeHistory.size) {
                                newCraftableRecipes[recipe] = Pair(recipeHistory, recipeIngredients)
                            }
                        }

                    }
                }
            }

            if(state == State.RESTARTING) {
                state = State.RUNNING
                return run()
            }else if(state != State.STOPPING) {
                CraftingBench.LOGGER.info("[Crafting Bench] Populated recipes in $time ms")
            }


            val client = MinecraftClient.getInstance()
            client.execute {
                val handler = client.player?.currentScreenHandler as? CraftingBenchScreenHandler ?: return@execute
                if(state == State.RUNNING) {
                    handler.craftableRecipes.clear()
                    newCraftableRecipes.forEach { (recipe, recipeHistory) ->
                        handler.craftableRecipes[recipe] = recipeHistory
                    }
                    (client.currentScreen as? CraftingBenchScreen)?.init(client, client.window.scaledWidth, client.window.scaledHeight)
                }
                handler.recipeFinder.stop()
            }
        }

        private enum class State {
            RUNNING,
            RESTARTING,
            STOPPING
        }

    }

}