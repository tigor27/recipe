package com.example.recipecook

import android.content.ContentResolver
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class IncomingRecipeMessage(
  val title: String,
  val ingredients: String
)

object RecipeSmsParser {
  private const val OPEN_RECIPE_PREFIX = "Open this recipe"

  fun parse(body: String): IncomingRecipeMessage? {
    val normalized = body.replace("\r\n", "\n").trim()
    if (normalized.isBlank()) return null

    val lines = normalized.lines().map { it.trim() }
    if (lines.size < 3) return null
    if (!lines.first().startsWith(OPEN_RECIPE_PREFIX, ignoreCase = true)) return null

    val title = lines[1].removePrefixCaseInsensitive("Name:").trim()
    if (title.isBlank()) return null

    val ingredients = lines[2].removePrefixCaseInsensitive("Ingredients:").trim()
    return IncomingRecipeMessage(title = title, ingredients = ingredients)
  }

  private fun String.removePrefixCaseInsensitive(prefix: String): String {
    return if (startsWith(prefix, ignoreCase = true)) substring(prefix.length) else this
  }
}

class SmsInboxRepository(private val contentResolver: ContentResolver) {

  suspend fun getLatestUnreadRecipeMessage(): IncomingRecipeMessage? = withContext(Dispatchers.IO) {
    val cursor = contentResolver.query(
      Uri.parse("content://sms/inbox"),
      arrayOf("body", "read", "date"),
      "read = 0",
      null,
      "date DESC"
    )

    cursor?.use {
      val bodyIndex = it.getColumnIndex("body")
      while (it.moveToNext()) {
        val body = if (bodyIndex >= 0) it.getString(bodyIndex).orEmpty() else ""
        val parsed = RecipeSmsParser.parse(body)
        if (parsed != null) return@withContext parsed
      }
    }

    null
  }
}

