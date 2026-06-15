package com.example.recipecook

import android.Manifest
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MaterialTheme {
        RecipeApp()
      }
    }
  }
}

@Composable
private fun RecipeApp(recipeViewModel: RecipeViewModel = viewModel()) {
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

  // Permission launcher
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

  // Load contacts if permission is already granted
  LaunchedEffect(hasContactsPermission) {
    if (hasContactsPermission) {
      recipeViewModel.onPermissionGranted()
      recipeViewModel.loadContacts(contactsRepository)
    }
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
          onShareClick = {
            val selectedContacts = recipeViewModel.getSelectedContacts()
            if (selectedContacts.isNotEmpty()) {
              scope.launch {
                val names = selectedContacts.joinToString(", ") { it.name }
                snackbarHostState.showSnackbar("Shared \"${recipe.title}\" with $names")
                recipeViewModel.clearContactSelection()
              }
            } else {
              scope.launch {
                snackbarHostState.showSnackbar("Select contacts to share with")
              }
            }
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

@Composable
private fun ContactsShareSection(
  contacts: List<Contact>,
  isContactSelected: (Int) -> Boolean,
  onContactToggle: (Int) -> Unit,
  isLoading: Boolean,
  hasPermission: Boolean,
  onRequestPermission: () -> Unit,
  onShareClick: () -> Unit
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
          // Show contacts list
          LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(contacts) { contact ->
              ContactChip(
                contact = contact,
                isSelected = isContactSelected(contact.id),
                onToggle = { onContactToggle(contact.id) }
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Button(
            onClick = onShareClick,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("Share Recipe")
          }
        }
      }
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



