package com.example.recipecook

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class SelectContactsViewModel : ViewModel() {
  private val mutableContacts = mutableStateListOf<Contact>()
  val contacts: List<Contact>
    get() = mutableContacts

  var isLoadingContacts by mutableStateOf(false)
    private set

  private val mutableSelectedContactIds = mutableStateListOf<Int>()
  val selectedContactIds: List<Int>
    get() = mutableSelectedContactIds

  fun toggleContactSelection(contactId: Int) {
    if (mutableSelectedContactIds.contains(contactId)) {
      mutableSelectedContactIds.remove(contactId)
    } else {
      mutableSelectedContactIds.add(contactId)
    }
  }

  fun isContactSelected(contactId: Int): Boolean {
    return mutableSelectedContactIds.contains(contactId)
  }

  fun loadContacts(repository: ContactsRepository) {
    if (isLoadingContacts || mutableContacts.isNotEmpty()) return

    viewModelScope.launch {
      isLoadingContacts = true
      try {
        val phoneContacts = repository.getContacts()
        mutableContacts.clear()
        mutableContacts.addAll(phoneContacts)
      } catch (e: Exception) {
        // Handle error - contacts couldn't be loaded
      } finally {
        isLoadingContacts = false
      }
    }
  }
}

