package com.example.recipecook

import android.app.Activity
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest

class SelectContactsActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MaterialTheme {
        SelectContactsScreen(activity = this)
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectContactsScreen(
  activity: SelectContactsActivity,
  viewModel: SelectContactsViewModel = viewModel()
) {
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
      viewModel.loadContacts(contactsRepository)
    }
  }

  // Load contacts if permission is already granted
  LaunchedEffect(hasContactsPermission) {
    if (hasContactsPermission && viewModel.contacts.isEmpty()) {
      viewModel.loadContacts(contactsRepository)
    }
  }

  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text("Select Contacts") },
        navigationIcon = {
          IconButton(onClick = { activity.finish() }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
          }
        }
      )
    }
  ) { innerPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(16.dp)
    ) {
      when {
        !hasContactsPermission -> {
          // Show permission request UI
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Text(
              text = "Grant access to your contacts to share recipes with friends and family",
              style = MaterialTheme.typography.bodyMedium,
              textAlign = TextAlign.Center,
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = {
              permissionLauncher.launch(Manifest.permission.READ_CONTACTS)
            }) {
              Text("Allow Contacts Access")
            }
          }
        }
        viewModel.isLoadingContacts -> {
          // Show loading indicator
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Loading contacts...")
          }
        }
        viewModel.contacts.isEmpty() -> {
          // No contacts found
          Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
          ) {
            Text(
              text = "No contacts found on this device",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }
        else -> {
          // Show contacts list
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            items(viewModel.contacts) { contact ->
              ContactSelectionRow(
                contact = contact,
                isSelected = viewModel.isContactSelected(contact.id),
                onToggle = { viewModel.toggleContactSelection(contact.id) }
              )
            }
          }

          Spacer(modifier = Modifier.height(16.dp))

          // Action buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            OutlinedButton(
              onClick = { activity.finish() },
              modifier = Modifier.weight(1f)
            ) {
              Text("Cancel")
            }

            Button(
              onClick = {
                // Return selected contact IDs back to MainActivity
                val intent = Intent()
                intent.putIntegerArrayListExtra(
                  "selectedContactIds",
                  ArrayList(viewModel.selectedContactIds)
                )
                activity.setResult(Activity.RESULT_OK, intent)
                activity.finish()
              },
              modifier = Modifier.weight(1f)
            ) {
              Text("Done")
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ContactSelectionRow(
  contact: Contact,
  isSelected: Boolean,
  onToggle: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable { onToggle() }
      .padding(12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Checkbox(
      checked = isSelected,
      onCheckedChange = { onToggle() }
    )
    Column(modifier = Modifier.weight(1f)) {
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


