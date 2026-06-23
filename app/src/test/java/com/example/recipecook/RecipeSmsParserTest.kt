package com.example.recipecook

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RecipeSmsParserTest {

  @Test
  fun parse_returnsRecipeForExpectedFormatWithLabels() {
    val message = """
      Open this recipe
      Name: Pasta Primavera
      Ingredients: Pasta, tomatoes, basil

      recipecook://recipe/open
    """.trimIndent()

    val parsed = RecipeSmsParser.parse(message)

    assertEquals("Pasta Primavera", parsed?.title)
    assertEquals("Pasta, tomatoes, basil", parsed?.ingredients)
  }

  @Test
  fun parse_returnsRecipeForExpectedFormatWithoutLabels() {
    val message = """
      Open this recipe
      Pancakes
      Flour, eggs, milk
    """.trimIndent()

    val parsed = RecipeSmsParser.parse(message)

    assertEquals("Pancakes", parsed?.title)
    assertEquals("Flour, eggs, milk", parsed?.ingredients)
  }

  @Test
  fun parse_returnsNullWhenMessagePrefixDoesNotMatch() {
    val message = "Hello there\nAnything\nAnything"

    val parsed = RecipeSmsParser.parse(message)

    assertNull(parsed)
  }
}

