package com.example.recipecook

import android.content.Context

private const val RECIPE_COOK_PREFS = "recipe_cook_prefs"
private const val SELECTED_CONTACT_IDS_KEY = "selected_contact_ids"

interface SelectedContactIdsStorage {
  fun load(): List<Int>
  fun save(contactIds: List<Int>)
  fun clear()
}

class SharedPrefsSelectedContactIdsStorage(context: Context) : SelectedContactIdsStorage {
  private val prefs = context.getSharedPreferences(RECIPE_COOK_PREFS, Context.MODE_PRIVATE)

  override fun load(): List<Int> {
    val rawIds = prefs.getString(SELECTED_CONTACT_IDS_KEY, null).orEmpty()
    if (rawIds.isBlank()) return emptyList()

    return rawIds
      .split(",")
      .mapNotNull { it.toIntOrNull() }
      .distinct()
  }

  override fun save(contactIds: List<Int>) {
    if (contactIds.isEmpty()) {
      clear()
      return
    }
    val serializedIds = contactIds.distinct().joinToString(",")
    prefs.edit().putString(SELECTED_CONTACT_IDS_KEY, serializedIds).apply()
  }

  override fun clear() {
    prefs.edit().remove(SELECTED_CONTACT_IDS_KEY).apply()
  }
}

