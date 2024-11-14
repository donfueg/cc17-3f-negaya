package com.example.crud2

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.SearchView

class ContactActivity : AppCompatActivity() {

    private lateinit var contactsRecyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contactList: List<Contact>
    private lateinit var filteredContactList: MutableList<Contact>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact) // Your contact layout XML

        // Set up the RecyclerView
        contactsRecyclerView = findViewById(R.id.contactsRecyclerView)
        contactsRecyclerView.layoutManager = LinearLayoutManager(this)

        // Create some sample contacts
        contactList = generateSampleContacts()

        // Initialize filtered list
        filteredContactList = contactList.toMutableList()

        // Set up the adapter
        contactAdapter = ContactAdapter(filteredContactList)
        contactsRecyclerView.adapter = contactAdapter

        // Set up the SearchView
        val searchView: SearchView = findViewById(R.id.contactSearchBar)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                // Optional: Handle query submission (e.g., hide keyboard)
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                // Filter the contact list based on the query
                filterContacts(newText)
                return true
            }
        })
    }

    // Sample data generation for contacts
    private fun generateSampleContacts(): List<Contact> {
        val contacts = mutableListOf<Contact>()
        for (i in 1..10) {
            contacts.add(Contact("Contact $i", "123-456-789$i"))
        }
        return contacts
    }

    // Function to filter contacts based on search query
    private fun filterContacts(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            contactList // Show all contacts if no query
        } else {
            contactList.filter {
                it.name.contains(query, ignoreCase = true) || it.phoneNumber.contains(query)
            }
        }

        // Update the adapter with filtered contacts
        contactAdapter.updateContacts(filteredList)
    }
}
