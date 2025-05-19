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

        val backButton: ImageButton = findViewById(R.id.backButton)
        backButton.setOnClickListener {
            finish()
        }

        try {
            // Get username from intent or fallback to SharedPreferences
            username = intent.getStringExtra("EXTRA_USERNAME")
            if (username.isNullOrEmpty()) {
                val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
                username = sharedPreferences.getString("username", null)
            }

            Log.d("ContactActivity", "Received username: $username")

            // If username is still null or empty, redirect to login
            if (username.isNullOrEmpty()) {
                Log.e("ContactActivity", "Username is null or empty, redirecting to login")
                handleSessionError()
                return
            }

            try {
                dbHelper = FirebaseHelper(this)
                Log.d("ContactActivity", "FirebaseHelper initialized")

                // Verify user exists in Firebase before proceeding
                dbHelper.checkUserExists(username!!) { userExists ->
                    if (userExists) {
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
                    } else {
                        Log.e("ContactActivity", "User does not exist in database: $username")
                        runOnUiThread {
                            // Clear invalid user data
                            getSharedPreferences("user_prefs", MODE_PRIVATE).edit().clear().apply()
                            handleSessionError()
                        }
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
            val usernameTextView: TextView = findViewById(R.id.textViewUsername)
            usernameTextView.text = "Hello, $username"

            contactsRecyclerView = findViewById(R.id.contactsRecyclerView)
            contactsRecyclerView.layoutManager = LinearLayoutManager(this)

            contactList = mutableListOf()

            contactAdapter = ContactAdapter(
                contactList,
                this,
                onDeleteClickListener = { contact ->
                    deleteContact(contact)
                },
                onEditClickListener = { contact ->
                    editContact(contact)
                }
            )

            contactsRecyclerView.adapter = contactAdapter

            loadContacts()

            val addContactButton: ImageButton = findViewById(R.id.imageButton)
            addContactButton.setOnClickListener {
                showAddContactDialog()
            }

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
                dbHelper.updateContact(contact, updatedContact) { success ->
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