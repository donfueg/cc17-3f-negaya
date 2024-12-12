package com.example.crud2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView

class ContactActivity : AppCompatActivity(), ContactAdapter.OnContactClickListener {

    private lateinit var contactsRecyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contactList: MutableList<Contact>
    private lateinit var dbHelper: FirebaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        // Initialize RecyclerView
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView)
        contactsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize FirebaseHelper
        dbHelper = FirebaseHelper(this)

        // Load existing contacts from Firestore
        dbHelper.getAllContacts { contacts ->
            contactList = contacts.toMutableList()

            // Set up adapter and pass the listener to it
            contactAdapter = ContactAdapter(contactList, this) { contact ->
                // Delete the contact from the list and update RecyclerView
                contactList.remove(contact)
                contactAdapter.notifyDataSetChanged()

                // Delete the contact from Firestore
                dbHelper.deleteContactByPhone(contact.phoneNumber)
            }
            contactsRecyclerView.adapter = contactAdapter
        }

        // Add contact button
        val addContactButton: ImageButton = findViewById(R.id.imageButton)
        addContactButton.setOnClickListener {
            // Show dialog to add contact
            val dialog = AddContactDialogFragment { newContact ->
                // Add new contact to the list and update RecyclerView
                contactList.add(newContact)
                contactAdapter.notifyItemInserted(contactList.size - 1)

                // Save the new contact to Firestore
                dbHelper.addContact(newContact.name, newContact.phoneNumber)
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

    // Method to make a phone call
    private fun makePhoneCall(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNumber")
        startActivity(intent)
    }

    // Implement the onContactClick method from the listener
    override fun onContactClick(phoneNumber: String) {
        makePhoneCall(phoneNumber)  // Initiate the phone call
    }
}
