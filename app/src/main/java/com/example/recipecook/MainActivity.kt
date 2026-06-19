
package com.example.recipecook

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import android.telephony.SmsManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

private const val RECIPE_CUSTOM_SCHEME = "recipecook"
private const val RECIPE_CUSTOM_HOST = "recipe"
private const val RECIPE_HTTPS_SCHEME = "https"
private const val RECIPE_HTTPS_HOST = "iicloud.tech"
private const val RECIPE_OPEN_PATH = "/open"

class MainActivity : ComponentActivity() {
  private var incomingDeepLinkRecipe by mutableStateOf<Recipe?>(null)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    // Test hook: allow auto-selecting first contact when launching activity with
    // intent extra "test_auto_select_first" (boolean). This is used for automated
    // end-to-end testing where we want to simulate choosing a contact in the
    // full-screen selector.
    val testAutoSelectFirst = intent?.getBooleanExtra("test_auto_select_first", false) ?: false
    val testAutoPickRandom = intent?.getBooleanExtra("test_auto_pick_random", false) ?: false
    incomingDeepLinkRecipe = parseRecipeFromIntent(intent)

    setContent {
      MaterialTheme {
        RecipeApp(
          initialAutoSelectFirst = testAutoSelectFirst,
          initialAutoPickRandom = testAutoPickRandom,
          deepLinkRecipe = incomingDeepLinkRecipe
        )
      }
    }
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    setIntent(intent)
    incomingDeepLinkRecipe = parseRecipeFromIntent(intent)
  }
}

@Composable
private fun RecipeApp(
  recipeViewModel: RecipeViewModel = viewModel(),
  initialAutoSelectFirst: Boolean = false,
  initialAutoPickRandom: Boolean = false,
  deepLinkRecipe: Recipe? = null
) {
  var newTitle by rememberSaveable { mutableStateOf("") }
  var newNotes by rememberSaveable { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  val scope = rememberCoroutineScope()
  val context = LocalContext.current

  // Contacts repository
  val contactsRepository = remember { ContactsRepository(context.contentResolver) }

  // Check if permission is already granted
  var hasContactsPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_CONTACTS
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  // Check if SEND_SMS permission is already granted
  var hasSendSmsPermission by remember {
    mutableStateOf(
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.SEND_SMS
      ) == PackageManager.PERMISSION_GRANTED
    )
  }

  // Pending send data stored while requesting SEND_SMS permission
  var pendingSendContacts by remember { mutableStateOf<List<Contact>?>(null) }
  var pendingSendBody by remember { mutableStateOf<String?>(null) }

  // Track which contact IDs have been sent to (for highlighting their chips)
  var sentContactIds by remember { mutableStateOf<Set<Int>>(emptySet()) }

  // Permission launcher for READ_CONTACTS
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasContactsPermission = isGranted
    if (isGranted) {
      recipeViewModel.onPermissionGranted()
      recipeViewModel.loadContacts(contactsRepository)
    } else {
      recipeViewModel.onPermissionDenied()
    }
  }

  // SMS permission launcher: if granted, send programmatically; if denied, fallback to composer
  val smsPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    hasSendSmsPermission = isGranted
    if (isGranted) {
      val contactsToSend = pendingSendContacts
      val bodyToSend = pendingSendBody
      if (!contactsToSend.isNullOrEmpty() && !bodyToSend.isNullOrBlank()) {
        try {
          val smsManager = SmsManager.getDefault()
          for (c in contactsToSend) {
            val phone = c.phone.ifBlank { null } ?: continue
            val parts = smsManager.divideMessage(bodyToSend)
            smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
          }
          scope.launch {
            val names = contactsToSend.joinToString(", ") { it.name }
            snackbarHostState.showSnackbar("Sent \"${recipeViewModel.selectedRecipe?.title}\" to $names")
            // Mark these contacts' chips as sent (highlight them)
            sentContactIds = sentContactIds + contactsToSend.map { it.id }.toSet()
          }
        } catch (e: Exception) {
          scope.launch {
            snackbarHostState.showSnackbar("Failed to send SMS: ${e.message}")
          }
        }
      }
    } else {
      // Permission denied: fallback to opening SMS composer with prefilled body
      val contactsToSend = pendingSendContacts
      val bodyToSend = pendingSendBody ?: ""
      val recipients = contactsToSend?.mapNotNull { it.phone.ifBlank { null } }?.joinToString(";") ?: ""
      val smsIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("smsto:${Uri.encode(recipients)}")
        putExtra("sms_body", bodyToSend)
      }
      try {
        context.startActivity(smsIntent)
        scope.launch {
          val names = contactsToSend?.joinToString(", ") { it.name } ?: ""
          snackbarHostState.showSnackbar("Opened SMS composer to share \"${recipeViewModel.selectedRecipe?.title}\" with $names")
          // Mark these contacts' chips as sent (highlight them)
          sentContactIds = sentContactIds + (contactsToSend?.map { it.id }?.toSet() ?: emptySet())
        }
      } catch (e: Exception) {
        scope.launch {
          snackbarHostState.showSnackbar("No SMS app available to send messages")
        }
      }
    }

    // Clear pending data
    pendingSendContacts = null
    pendingSendBody = null
  }

  // Activity launcher for SelectContactsActivity
  val selectContactsLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult()
  ) { result ->
    if (result.resultCode == android.app.Activity.RESULT_OK) {
      val selectedIds = result.data?.getIntegerArrayListExtra("selectedContactIds") ?: emptyList()
      recipeViewModel.setSelectedContactIds(selectedIds)
    }
  }

  // Load contacts if permission is already granted
  LaunchedEffect(hasContactsPermission) {
    if (hasContactsPermission) {
      recipeViewModel.onPermissionGranted()
      recipeViewModel.loadContacts(contactsRepository)
    }
  }

  // If running with the test flag, as soon as contacts are loaded select the first
  // contact automatically. This makes it possible to run an automated end-to-end
  // flow without manual UI interaction.
  LaunchedEffect(initialAutoSelectFirst, recipeViewModel.contacts) {
    if (initialAutoSelectFirst) {
      if (recipeViewModel.contacts.isNotEmpty()) {
        recipeViewModel.setSelectedContactIds(listOf(recipeViewModel.contacts.first().id))
      } else {
        // If there are no real contacts on the device/emulator, inject a test
        // contact so the automated flow can proceed and the UI shows a selected
        // first-name chip.
        val testContact = Contact(id = -1, name = "Test User", phone = "+15550001111")
        recipeViewModel.addTestContact(testContact)
        recipeViewModel.setSelectedContactIds(listOf(testContact.id))
      }
    }
  }

  // Test hook: optionally auto pick a random recipe on startup
  LaunchedEffect(initialAutoPickRandom) {
    if (initialAutoPickRandom) {
      recipeViewModel.chooseRandomRecipe()
    }
  }

  // If app is launched from a recipe link, show that recipe immediately.
  LaunchedEffect(deepLinkRecipe) {
    deepLinkRecipe?.let { recipeViewModel.showRecipeFromDeepLink(it) }
  }

  Scaffold(
    snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp)
    ) {
      Text(
        text = "Recipe Cook",
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = newTitle,
        onValueChange = { newTitle = it },
        label = { Text("Recipe title") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(8.dp))

      OutlinedTextField(
        value = newNotes,
        onValueChange = { newNotes = it },
        label = { Text("Ingredients / notes") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2
      )

      Spacer(modifier = Modifier.height(12.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = {
            val added = recipeViewModel.addRecipe(newTitle, newNotes)
            if (added) {
              newTitle = ""
              newNotes = ""
            } else {
              scope.launch {
                snackbarHostState.showSnackbar("Please enter a recipe title")
              }
            }
          },
          modifier = Modifier.weight(1f)
        ) {
          Text("Add recipe")
        }

        TextButton(
          onClick = {
            val selected = recipeViewModel.chooseRandomRecipe()
            if (selected == null) {
              scope.launch {
                snackbarHostState.showSnackbar("Add recipes first")
              }
            }
          },
          modifier = Modifier.weight(1f)
        ) {
          Text("Pick random")
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      recipeViewModel.selectedRecipe?.let { recipe ->
        Card(modifier = Modifier.fillMaxWidth()) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Tonight's pick",
              style = MaterialTheme.typography.titleSmall,
              color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = recipe.title,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold
            )
            if (recipe.notes.isNotBlank()) {
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = recipe.notes,
                style = MaterialTheme.typography.bodyMedium
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Select Contacts Button
        Button(
          onClick = {
            if (!hasContactsPermission) {
              permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            } else {
              val intent = android.content.Intent(context, SelectContactsActivity::class.java)
              selectContactsLauncher.launch(intent)
            }
          },
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Select Contacts")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Build the share handler here so it can capture 'recipe'
        val shareClickHandler: () -> Unit = {
          val selectedContacts = recipeViewModel.getSelectedContacts()
          if (selectedContacts.isEmpty()) {
            scope.launch {
              snackbarHostState.showSnackbar("Select contacts to share with")
            }
          } else {
            val phoneNumbers = selectedContacts.mapNotNull { it.phone.ifBlank { null } }
            val body = buildString {
              append("Recipe: ")
              append(recipe.title)
              if (recipe.notes.isNotBlank()) {
                append("\n")
                append(recipe.notes)
              }
              append("\n\n")
              append(buildRecipeDeepLink(recipe))
              append("\n\nTap link above to open in Recipe app. (Select Recipe Cook if prompted.)")
            }

            // If we already have SEND_SMS permission, send programmatically
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
              try {
                val smsManager = SmsManager.getDefault()
                for (phone in phoneNumbers) {
                  val parts = smsManager.divideMessage(body)
                  smsManager.sendMultipartTextMessage(phone, null, parts, null, null)
                }
                scope.launch {
                  val names = selectedContacts.joinToString(", ") { it.name }
                  snackbarHostState.showSnackbar("Sent \"${recipe.title}\" to $names")
                  // Mark these contacts' chips as sent (highlight them)
                  sentContactIds = sentContactIds + selectedContacts.map { it.id }.toSet()
                }
              } catch (e: Exception) {
                scope.launch {
                  snackbarHostState.showSnackbar("Failed to send SMS: ${e.message}")
                }
              }
            } else {
              // Save pending data and request SEND_SMS permission. The launcher will
              // handle sending on grant or falling back to composer on denial.
              pendingSendContacts = selectedContacts
              pendingSendBody = body
              smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
            }
          }
        }

        // Contacts sharing section
        ContactsShareSection(
          contacts = recipeViewModel.contacts,
          isContactSelected = { recipeViewModel.isContactSelected(it) },
          onContactToggle = { recipeViewModel.toggleContactSelection(it) },
          isLoading = recipeViewModel.isLoadingContacts,
          hasPermission = hasContactsPermission,
          onRequestPermission = {
            permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
          },
          onShareClick = shareClickHandler,
          sentContactIds = sentContactIds,
          onClearContacts = {
            recipeViewModel.clearContactSelection()
            sentContactIds = emptySet()
          }
        )

        Spacer(modifier = Modifier.height(12.dp))
      }

      Text(
        text = "Recipe list (${recipeViewModel.recipes.size})",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(8.dp))

      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        itemsIndexed(recipeViewModel.recipes) { index, recipe ->
          Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(12.dp)) {
              Text(
                text = "${index + 1}. ${recipe.title}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
              )
              if (recipe.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = recipe.notes,
                  style = MaterialTheme.typography.bodyMedium
                )
              }
            }
          }
        }
      }
    }
  }
}

private fun parseRecipeFromIntent(intent: Intent?): Recipe? {
  val data = intent?.data ?: return null
  if (intent.action != Intent.ACTION_VIEW) return null

  val isCustomSchemeLink =
    data.scheme == RECIPE_CUSTOM_SCHEME &&
      data.host == RECIPE_CUSTOM_HOST &&
      data.path?.startsWith(RECIPE_OPEN_PATH) == true
  val isHttpsLink =
    data.scheme == RECIPE_HTTPS_SCHEME &&
      data.host == RECIPE_HTTPS_HOST &&
      data.path?.startsWith(RECIPE_OPEN_PATH) == true

  if (!isCustomSchemeLink && !isHttpsLink) return null

  val title = data.getQueryParameter("title")?.trim().orEmpty()
  if (title.isBlank()) return null

  val notes = data.getQueryParameter("notes")?.trim().orEmpty()
  return Recipe(title = title, notes = notes)
}

private fun buildRecipeDeepLink(recipe: Recipe): String {
  return Uri.Builder()
    .scheme(RECIPE_HTTPS_SCHEME)
    .authority(RECIPE_HTTPS_HOST)
    .appendPath("open")
    .appendQueryParameter("title", recipe.title)
    .appendQueryParameter("notes", recipe.notes)
    .build()
    .toString()
}

@Composable
private fun ContactsShareSection(
  contacts: List<Contact>,
  isContactSelected: (Int) -> Boolean,
  onContactToggle: (Int) -> Unit,
  isLoading: Boolean,
  hasPermission: Boolean,
  onRequestPermission: () -> Unit,
  onShareClick: () -> Unit,
  sentContactIds: Set<Int>,
  onClearContacts: () -> Unit
) {
  Card(
    modifier = Modifier.fillMaxWidth(),
    colors = CardDefaults.cardColors(
      containerColor = MaterialTheme.colorScheme.secondaryContainer
    )
  ) {
    Column(modifier = Modifier.padding(12.dp)) {
      Text(
        text = "Share with contacts",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Medium
      )

      Spacer(modifier = Modifier.height(8.dp))

      when {
        !hasPermission -> {
          // Show permission request UI
          Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Text(
              text = "Grant access to your contacts to share recipes with friends and family",
              style = MaterialTheme.typography.bodyMedium,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = onRequestPermission) {
              Text("Allow Contacts Access")
            }
          }
        }
        isLoading -> {
          // Show loading indicator
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
          ) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Loading contacts...")
          }
        }
        contacts.isEmpty() -> {
          // No contacts found
          Text(
            text = "No contacts found on this device",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer
          )
        }
        else -> {
          // Show only selected contacts (first names)
          val selected = contacts.filter { isContactSelected(it.id) }

          if (selected.isEmpty()) {
            Text(
              text = "No contacts selected",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSecondaryContainer
            )
          } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
              items(selected) { contact ->
                val firstName = contact.name.split(Regex("\\s+")).firstOrNull().orEmpty()
                val isSent = contact.id in sentContactIds
                SelectedContactChip(firstName = firstName, isSent = isSent)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
              onClick = onShareClick,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Share Recipe")
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
              onClick = onClearContacts,
              modifier = Modifier.fillMaxWidth()
            ) {
              Text("Clear contacts")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SelectedContactChip(firstName: String, isSent: Boolean = false) {
  Card(
    colors = CardDefaults.cardColors(
      containerColor = if (isSent) Color.Yellow else MaterialTheme.colorScheme.surface
    ),
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = firstName.ifBlank { "Unnamed" },
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
      )
    }
  }
}

@Composable
private fun ContactChip(
  contact: Contact,
  isSelected: Boolean,
  onToggle: () -> Unit
) {
  Card(
    modifier = Modifier.clickable { onToggle() },
    colors = CardDefaults.cardColors(
      containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
      } else {
        MaterialTheme.colorScheme.surface
      }
    )
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Checkbox(
        checked = isSelected,
        onCheckedChange = { onToggle() },
        modifier = Modifier.size(20.dp)
      )
      Spacer(modifier = Modifier.width(4.dp))
      Column {
        Text(
          text = contact.name,
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = contact.phone,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}


