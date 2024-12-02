package com.example.crud2

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView

class ContactActivity : AppCompatActivity() {

    private lateinit var contactsRecyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contactList: MutableList<Contact>
    private lateinit var dbHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        // Initialize RecyclerView
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView)
        contactsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize DatabaseHelper
        dbHelper = DatabaseHelper(this)

        // Load existing contacts from the database
        contactList = dbHelper.getAllContacts().toMutableList()

        // Set up adapter
        contactAdapter = ContactAdapter(contactList)
        contactsRecyclerView.adapter = contactAdapter

        // Add contact button
        val addContactButton: ImageButton = findViewById(R.id.imageButton)
        addContactButton.setOnClickListener {
            // Show dialog to add contact
            val dialog = AddContactDialogFragment { newContact ->
                // Add new contact to the list and update RecyclerView
                contactList.add(newContact)
                contactAdapter.notifyItemInserted(contactList.size - 1)

                // Save the new contact to the database
                dbHelper.addContact(newContact.name, newContact.phoneNumber)  // Correct method call
            }
            dialog.show(supportFragmentManager, "AddContactDialog")
        }

        // SearchView functionality
        val searchView: SearchView = findViewById(R.id.contactSearchBar)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterContacts(newText)
                return true
            }
        })
    }

    private fun filterContacts(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            contactList // Show all contacts if no query
        } else {
            contactList.filter {
                it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query)
            }
        }
        contactAdapter.updateContacts(filteredList)
    }
}
