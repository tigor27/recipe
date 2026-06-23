package com.example.recipecook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlin.random.Random

data class Recipe(
  val title: String,
  val notes: String
)

data class Contact(
  val id: Int,
  val name: String,
  val phone: String
)

class RecipeViewModel(
  private val random: Random = Random.Default,
  private val selectedContactIdsStorage: SelectedContactIdsStorage = NoOpSelectedContactIdsStorage
) : ViewModel() {

  private val mutableRecipes = mutableStateListOf(
    Recipe("Spaghetti Carbonara", "Pasta, eggs, parmesan, pancetta"),
    Recipe("Veggie Stir Fry", "Mixed veggies, soy sauce, ginger, garlic"),
    Recipe("Chicken Tacos", "Chicken, tortillas, salsa, avocado")
  )

  val recipes: List<Recipe>
    get() = mutableRecipes

  var selectedRecipe by mutableStateOf<Recipe?>(null)
    private set

  // Contacts for sharing recipes
  private val mutableContacts = mutableStateListOf<Contact>()

  val contacts: List<Contact>
    get() = mutableContacts

  var isLoadingContacts by mutableStateOf(false)
    private set

  var contactsPermissionGranted by mutableStateOf(false)
    private set

  private val mutableSelectedContactIds = mutableStateListOf<Int>()

  init {
    mutableSelectedContactIds.addAll(selectedContactIdsStorage.load())
  }

  val selectedContactIds: List<Int>
    get() = mutableSelectedContactIds

  fun toggleContactSelection(contactId: Int) {
    if (mutableSelectedContactIds.contains(contactId)) {
      mutableSelectedContactIds.remove(contactId)
    } else {
      mutableSelectedContactIds.add(contactId)
    }
    persistSelectedContactIds()
  }

  fun isContactSelected(contactId: Int): Boolean {
    return mutableSelectedContactIds.contains(contactId)
  }

  fun getSelectedContacts(): List<Contact> {
    return mutableContacts.filter { mutableSelectedContactIds.contains(it.id) }
  }

  fun clearContactSelection() {
    mutableSelectedContactIds.clear()
    selectedContactIdsStorage.clear()
  }

  fun setSelectedContactIds(contactIds: List<Int>) {
    mutableSelectedContactIds.clear()
    mutableSelectedContactIds.addAll(contactIds.distinct())
    persistSelectedContactIds()
    // Debug log to assist end-to-end testing: print selected ids and names
    try {
      android.util.Log.d("RecipeViewModel", "Selected contact ids: $contactIds")
    } catch (_: Exception) {
      // ignore logging errors in non-Android test environments
    }
  }

  fun loadContacts(repository: ContactsRepository) {
    if (isLoadingContacts || mutableContacts.isNotEmpty()) return

    viewModelScope.launch {
      isLoadingContacts = true
      try {
        val phoneContacts = repository.getContacts()
        mutableContacts.clear()
        mutableContacts.addAll(phoneContacts)
        contactsPermissionGranted = true
      } catch (e: Exception) {
        // Handle error - contacts couldn't be loaded
        contactsPermissionGranted = false
      } finally {
        isLoadingContacts = false
      }
    }
  }

  fun onPermissionGranted() {
    contactsPermissionGranted = true
  }

  fun onPermissionDenied() {
    contactsPermissionGranted = false
  }

  // Test helper: add a fake contact to the in-memory list. This is used by the
  // automated end-to-end flow when the device/emulator has no real contacts.
  fun addTestContact(contact: Contact) {
    mutableContacts.add(contact)
    try {
      android.util.Log.d("RecipeViewModel", "Added test contact: ${contact.name} (${contact.id})")
    } catch (_: Exception) {
    }
  }

  private fun persistSelectedContactIds() {
    selectedContactIdsStorage.save(mutableSelectedContactIds.toList())
  }

  private object NoOpSelectedContactIdsStorage : SelectedContactIdsStorage {
    override fun load(): List<Int> = emptyList()

    override fun save(contactIds: List<Int>) {
      // No-op default for tests/previews that do not provide persistence.
    }

    override fun clear() {
      // No-op default for tests/previews that do not provide persistence.
    }
  }

  fun addRecipe(title: String, notes: String): Boolean {
    val cleanTitle = title.trim()
    val cleanNotes = notes.trim()

    if (cleanTitle.isEmpty()) {
      return false
    }

    mutableRecipes.add(Recipe(cleanTitle, cleanNotes))
    return true
  }

  fun chooseRandomRecipe(): Recipe? {
    if (mutableRecipes.isEmpty()) {
      selectedRecipe = null
      return null
    }

    selectedRecipe = mutableRecipes[random.nextInt(mutableRecipes.size)]
    return selectedRecipe
  }

  fun selectRecipe(recipe: Recipe) {
    selectedRecipe = recipe
  }

  fun showRecipeFromDeepLink(recipe: Recipe) {
    val normalizedTitle = recipe.title.trim()
    if (normalizedTitle.isBlank()) return

    val normalizedRecipe = recipe.copy(
      title = normalizedTitle,
      notes = recipe.notes.trim()
    )

    val existingRecipe = mutableRecipes.firstOrNull {
      it.title == normalizedRecipe.title && it.notes == normalizedRecipe.notes
    }

    val recipeToDisplay = existingRecipe ?: normalizedRecipe.also { mutableRecipes.add(it) }
    selectedRecipe = recipeToDisplay
  }
}
