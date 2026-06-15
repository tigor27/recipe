package com.example.recipecook

import android.content.ContentResolver
import android.provider.ContactsContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ContactsRepository(private val contentResolver: ContentResolver) {

  suspend fun getContacts(): List<Contact> = withContext(Dispatchers.IO) {
    val contacts = mutableListOf<Contact>()
    val projection = arrayOf(
      ContactsContract.CommonDataKinds.Phone._ID,
      ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
      ContactsContract.CommonDataKinds.Phone.NUMBER
    )

    val cursor = contentResolver.query(
      ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
      projection,
      null,
      null,
      ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
    )

    cursor?.use {
      val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone._ID)
      val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
      val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)

      val seenNames = mutableSetOf<String>()

      while (it.moveToNext()) {
        val id = if (idIndex >= 0) it.getLong(idIndex).toInt() else 0
        val name = if (nameIndex >= 0) it.getString(nameIndex) ?: "" else ""
        val phone = if (phoneIndex >= 0) it.getString(phoneIndex) ?: "" else ""

        // Skip duplicates by name
        if (name.isNotBlank() && !seenNames.contains(name)) {
          seenNames.add(name)
          contacts.add(Contact(id, name, phone))
        }
      }
    }

    contacts
  }
}

