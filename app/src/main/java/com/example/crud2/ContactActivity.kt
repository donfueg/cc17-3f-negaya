package com.example.crud2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ContactActivity : AppCompatActivity(), ContactAdapter.OnContactClickListener {
    private lateinit var contactsRecyclerView: RecyclerView
    private lateinit var contactAdapter: ContactAdapter
    private lateinit var contactList: MutableList<Contact>
    private lateinit var dbHelper: FirebaseHelper
    private var username: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_contact)

        // Retrieve the username from the intent
        username = intent.getStringExtra("EXTRA_USERNAME")
        Log.d("ContactActivity", "Received username: $username")

        if (username == null) {
            handleSessionError()
            return
        }

        dbHelper = FirebaseHelper(this)
        dbHelper.setCurrentUser(username!!) { success ->
            if (success) {
                Log.d("ContactActivity", "User session validated successfully")
                initializeUI()
            } else {
                Log.e("ContactActivity", "Session validation failed")
                handleSessionError()
            }
        }
    }

    private fun handleSessionError() {
        Log.e("ContactActivity", "Session error: Username or user ID not set")
        Toast.makeText(this, "Session error. Please login again", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }

    private fun initializeUI() {
        // Update the TextView to display the logged-in username
        val usernameTextView: TextView = findViewById(R.id.textViewUsername)
        usernameTextView.text = "Hello, $username"

        contactsRecyclerView = findViewById(R.id.contactsRecyclerView)
        contactsRecyclerView.layoutManager = LinearLayoutManager(this)

        contactList = mutableListOf()
        contactAdapter = ContactAdapter(contactList, this) { contact ->
            dbHelper.deleteContactByPhone(contact.phone) { success ->
                runOnUiThread {
                    if (success) {
                        contactList.remove(contact)
                        contactAdapter.notifyDataSetChanged()
                        Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error deleting contact", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        contactsRecyclerView.adapter = contactAdapter

        loadContacts()

        val addContactButton: ImageButton = findViewById(R.id.imageButton)
        addContactButton.setOnClickListener {
            showAddContactDialog()
        }

        setupSearchView()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, Login::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupSearchView() {
        val searchView: SearchView = findViewById(R.id.contactSearchBar)
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val filteredList = if (newText.isNullOrEmpty()) {
                    contactList
                } else {
                    contactList.filter {
                        it.name.contains(newText, ignoreCase = true) || it.phone.contains(newText)
                    }
                }
                contactAdapter.updateContacts(filteredList)
                return true
            }
        })
    }

    private fun loadContacts() {
        dbHelper.getAllContacts { contacts ->
            runOnUiThread {
                contactList.clear()
                contactList.addAll(contacts)
                contactAdapter.notifyDataSetChanged()
            }
        }
    }

    private fun showAddContactDialog() {
        val dialog = AddContactDialogFragment { newContact ->
            dbHelper.addContact(newContact.name, newContact.phone) { success ->
                runOnUiThread {
                    if (success) {
                        loadContacts()
                        Toast.makeText(this, "Contact added successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error adding contact", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        dialog.show(supportFragmentManager, "AddContactDialog")
    }

    override fun onContactClick(phoneNumber: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phoneNumber")
        startActivity(intent)
    }
}
