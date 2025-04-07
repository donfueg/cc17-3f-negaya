package com.example.crud2

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.SearchView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

        try {
            // Retrieve the username from the intent
            username = intent.getStringExtra("EXTRA_USERNAME")
            Log.d("ContactActivity", "Received username: $username")

            if (username.isNullOrEmpty()) {
                handleSessionError()
                return
            }

            // Initialize Firebase with proper error handling
            try {
                dbHelper = FirebaseHelper(this)
                Log.d("ContactActivity", "FirebaseHelper initialized")

                dbHelper.setCurrentUser(username!!) { success ->
                    if (success) {
                        Log.d("ContactActivity", "User session validated successfully")
                        runOnUiThread {
                            try {
                                initializeUI()
                            } catch (e: Exception) {
                                Log.e("ContactActivity", "Error initializing UI: ${e.message}")
                                e.printStackTrace()
                                showError("Error initializing UI components")
                            }
                        }
                    } else {
                        Log.e("ContactActivity", "Session validation failed")
                        runOnUiThread { handleSessionError() }
                    }
                }
            } catch (e: Exception) {
                Log.e("ContactActivity", "FirebaseHelper error: ${e.message}")
                e.printStackTrace()
                showError("Database initialization error")
            }
        } catch (e: Exception) {
            Log.e("ContactActivity", "General error in onCreate: ${e.message}")
            e.printStackTrace()
            showError("Application error")
        }
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun handleSessionError() {
        Log.e("ContactActivity", "Session error: Username or user ID not set")
        Toast.makeText(this, "Session error. Please login again", Toast.LENGTH_SHORT).show()
        navigateToLogin()
    }

    private fun initializeUI() {
        try {
            // Update the TextView to display the logged-in username
            val usernameTextView: TextView = findViewById(R.id.textViewUsername)
            usernameTextView.text = "Hello, $username"

            // Setup RecyclerView
            contactsRecyclerView = findViewById(R.id.contactsRecyclerView)
            contactsRecyclerView.layoutManager = LinearLayoutManager(this)

            contactList = mutableListOf()

            // Initialize adapter - FIXED: Matches exact parameter order of ContactAdapter
            contactAdapter = ContactAdapter(
                contactList,  // First parameter is the contact list
                this,  // Second parameter is the OnContactClickListener
                onDeleteClickListener = { contact ->  // Third parameter is the delete lambda
                    deleteContact(contact)
                },
                onEditClickListener = { contact ->  // Fourth parameter is the edit lambda
                    editContact(contact)
                }
            )

            contactsRecyclerView.adapter = contactAdapter

            // Load contacts
            loadContacts()

            // Setup add contact button
            val addContactButton: ImageButton = findViewById(R.id.imageButton)
            addContactButton.setOnClickListener {
                showAddContactDialog()
            }

            // Setup search
            setupSearchView()

        } catch (e: Exception) {
            Log.e("ContactActivity", "Error in initializeUI: ${e.message}")
            e.printStackTrace()
            showError("Error setting up interface")
        }
    }

    private fun deleteContact(contact: Contact) {
        try {
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
        } catch (e: Exception) {
            Log.e("ContactActivity", "Error deleting contact: ${e.message}")
            showError("Error processing delete request")
        }
    }

    private fun editContact(contact: Contact) {
        try {
            Toast.makeText(this, "Edit clicked for ${contact.name}", Toast.LENGTH_SHORT).show()
            showEditContactDialog(contact)
        } catch (e: Exception) {
            Log.e("ContactActivity", "Error editing contact: ${e.message}")
            showError("Error processing edit request")
        }
    }

    private fun navigateToLogin() {
        try {
            val intent = Intent(this, Login::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: Exception) {
            Log.e("ContactActivity", "Error navigating to login: ${e.message}")
            // Last resort - just finish this activity
            finish()
        }
    }

    private fun setupSearchView() {
        try {
            val searchView: SearchView = findViewById(R.id.contactSearchBar)
            searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean = false

                override fun onQueryTextChange(newText: String?): Boolean {
                    try {
                        val filteredList = if (newText.isNullOrEmpty()) {
                            contactList
                        } else {
                            contactList.filter {
                                it.name.contains(newText, ignoreCase = true) ||
                                        it.phone.contains(newText)
                            }
                        }
                        contactAdapter.updateContacts(filteredList)
                    } catch (e: Exception) {
                        Log.e("ContactActivity", "Error filtering contacts: ${e.message}")
                    }
                    return true
                }
            })
        } catch (e: Exception) {
            Log.e("ContactActivity", "Error setting up search: ${e.message}")
        }
    }

    private fun loadContacts() {
        try {
            Log.d("ContactActivity", "Loading contacts...")
            dbHelper.getAllContacts { contacts ->
                runOnUiThread {
                    try {
                        contactList.clear()
                        contactList.addAll(contacts)
                        contactAdapter.notifyDataSetChanged()
                        Log.d("ContactActivity", "Loaded ${contacts.size} contacts")
                    } catch (e: Exception) {
                        Log.e("ContactActivity", "Error updating contact list: ${e.message}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("ContactActivity", "Error loading contacts: ${e.message}")
            showError("Error loading contacts")
        }
    }

    private fun showAddContactDialog() {
        try {
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
        } catch (e: Exception) {
            Log.e("ContactActivity", "Error showing add dialog: ${e.message}")
            showError("Error opening add contact form")
        }
    }

    private fun showEditContactDialog(contact: Contact) {
        try {
            val dialog = EditContactDialogFragment(contact) { updatedContact ->
                dbHelper.updateContact(updatedContact) { success ->
                    runOnUiThread {
                        if (success) {
                            loadContacts()
                            Toast.makeText(this, "Contact updated successfully", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Error updating contact", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            dialog.show(supportFragmentManager, "EditContactDialog")
        } catch (e: Exception) {
            Log.e("ContactActivity", "Error showing edit dialog: ${e.message}")
            showError("Error opening edit contact form")
        }
    }

    override fun onContactClick(phoneNumber: String) {
        try {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:$phoneNumber")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e("ContactActivity", "Error dialing number: ${e.message}")
            showError("Error initiating call")
        }
    }
}