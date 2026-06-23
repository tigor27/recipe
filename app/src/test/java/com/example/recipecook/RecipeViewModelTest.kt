package com.example.recipecook

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeViewModelTest {

  private class FakeSelectedContactIdsStorage(
    initialIds: List<Int> = emptyList()
  ) : SelectedContactIdsStorage {
    private var storedIds: List<Int> = initialIds
    var clearCalled = false
      private set

    override fun load(): List<Int> = storedIds

    override fun save(contactIds: List<Int>) {
      storedIds = contactIds
    }

    override fun clear() {
      clearCalled = true
      storedIds = emptyList()
    }

    fun snapshot(): List<Int> = storedIds
  }

  @Test
  fun addRecipe_requiresNonBlankTitle() {
    val viewModel = RecipeViewModel()

    val added = viewModel.addRecipe("   ", "Anything")

    assertFalse(added)
  }

  @Test
  fun addRecipe_appendsRecipeToList() {
    val viewModel = RecipeViewModel()
    val initialCount = viewModel.recipes.size

    val added = viewModel.addRecipe("Omelette", "Eggs, butter, salt")

    assertTrue(added)
    assertEquals(initialCount + 1, viewModel.recipes.size)
    assertEquals("Omelette", viewModel.recipes.last().title)
  }

  @Test
  fun chooseRandomRecipe_returnsItemFromExistingList() {
    val viewModel = RecipeViewModel(random = Random(42))

    val chosen = viewModel.chooseRandomRecipe()

    assertNotNull(chosen)
    assertTrue(viewModel.recipes.contains(chosen))
  }

  @Test
  fun showRecipeFromDeepLink_addsAndSelectsRecipeWhenMissing() {
    val viewModel = RecipeViewModel()
    val initialCount = viewModel.recipes.size

    viewModel.showRecipeFromDeepLink(Recipe("Shakshuka", "Eggs, tomatoes, peppers"))

    assertEquals(initialCount + 1, viewModel.recipes.size)
    assertEquals("Shakshuka", viewModel.selectedRecipe?.title)
  }

  @Test
  fun showRecipeFromDeepLink_reusesExistingRecipeWithoutDuplicate() {
    val viewModel = RecipeViewModel()
    val initialCount = viewModel.recipes.size

    viewModel.showRecipeFromDeepLink(Recipe("Spaghetti Carbonara", "Pasta, eggs, parmesan, pancetta"))

    assertEquals(initialCount, viewModel.recipes.size)
    assertEquals("Spaghetti Carbonara", viewModel.selectedRecipe?.title)
  }

  @Test
  fun selectRecipe_setsSelectedRecipe() {
    val viewModel = RecipeViewModel()
    val recipe = viewModel.recipes.first()

    viewModel.selectRecipe(recipe)

    assertEquals(recipe, viewModel.selectedRecipe)
  }

  @Test
  fun selectedContacts_loadsPersistedIdsOnInit() {
    val storage = FakeSelectedContactIdsStorage(initialIds = listOf(4, 9))

    val viewModel = RecipeViewModel(selectedContactIdsStorage = storage)

    assertEquals(listOf(4, 9), viewModel.selectedContactIds)
  }

  @Test
  fun selectedContacts_persistsOnToggleAndClear() {
    val storage = FakeSelectedContactIdsStorage()
    val viewModel = RecipeViewModel(selectedContactIdsStorage = storage)

    viewModel.toggleContactSelection(15)
    assertEquals(listOf(15), storage.snapshot())

    viewModel.toggleContactSelection(28)
    assertEquals(listOf(15, 28), storage.snapshot())

    viewModel.clearContactSelection()
    assertTrue(storage.clearCalled)
    assertTrue(storage.snapshot().isEmpty())
  }
}

