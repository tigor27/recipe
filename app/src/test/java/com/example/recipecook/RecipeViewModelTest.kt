package com.example.recipecook

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeViewModelTest {

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
}

