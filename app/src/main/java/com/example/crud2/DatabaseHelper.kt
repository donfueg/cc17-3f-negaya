package com.example.crud2

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirebaseHelper(context: Context) {
    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()
    private var currentUserId: String? = null

    // Validate user by username and password
    fun validateUserByUsernameAndPassword(
        username: String,
        password: String,
        callback: (Boolean) -> Unit
    ) {
        usersCollection
            .whereEqualTo("username", username)
            .whereEqualTo("password", password)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val document = result.documents[0]
                    currentUserId = document.id
                    callback(true)
                } else {
                    currentUserId = null
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseHelper", "Error validating user: ${exception.message}")
                currentUserId = null
                callback(false)
            }
    }

    // Set current user ID
    fun setCurrentUser(username: String, callback: (Boolean) -> Unit) {
        Log.d("FirebaseHelper", "Setting current user for username: $username")

        if (currentUserId != null) {
            Log.d("FirebaseHelper", "Current user already set: $currentUserId")
            callback(true)
            return
        }

        usersCollection
            .whereEqualTo("username", username)
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.e("FirebaseHelper", "No user found for username: $username")
                    currentUserId = null
                    callback(false)
                } else {
                    val document = result.documents[0]
                    currentUserId = document.id
                    Log.d("FirebaseHelper", "User found, currentUserId set: $currentUserId")
                    callback(true)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseHelper", "Error setting current user: ${exception.message}")
                currentUserId = null
                callback(false)
            }
    }

    // Get the current user ID
    private fun getCurrentUserId(): String? {
        return currentUserId
    }

    // Add a new contact
    fun addContact(name: String, phone: String, callback: (Boolean) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.e("FirebaseHelper", "currentUserId is null. Cannot add contact.")
            callback(false)
            return
        }

        val contact = hashMapOf(
            "name" to name,
            "phone" to phone,
            "timestamp" to System.currentTimeMillis()
        )

        usersCollection.document(userId)
            .collection("contacts")
            .add(contact)
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseHelper", "Error adding contact: ${exception.message}")
                callback(false)
            }
    }

    // Retrieve all contacts
    fun getAllContacts(callback: (List<Contact>) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.e("FirebaseHelper", "currentUserId is null. Cannot retrieve contacts.")
            callback(emptyList())
            return
        }

        usersCollection.document(userId)
            .collection("contacts")
            .orderBy("timestamp")
            .get()
            .addOnSuccessListener { result ->
                val contactList = mutableListOf<Contact>()
                for (document in result) {
                    val name = document.getString("name") ?: continue
                    val phone = document.getString("phone") ?: continue
                    contactList.add(Contact(name, phone))
                }
                callback(contactList)
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseHelper", "Error retrieving contacts: ${exception.message}")
                callback(emptyList())
            }
    }

    // Delete a contact by phone number
    fun deleteContactByPhone(phoneNumber: String, callback: (Boolean) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.e("FirebaseHelper", "currentUserId is null. Cannot delete contact.")
            callback(false)
            return
        }

        usersCollection.document(userId)
            .collection("contacts")
            .whereEqualTo("phone", phoneNumber)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    for (document in result.documents) {
                        document.reference.delete()
                            .addOnSuccessListener {
                                callback(true)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("FirebaseHelper", "Error deleting contact: ${exception.message}")
                                callback(false)
                            }
                    }
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseHelper", "Error finding contact to delete: ${exception.message}")
                callback(false)
            }
    }

    // Update a contact by phone number
    fun updateContact(updatedContact: Contact, callback: (Boolean) -> Unit) {
        val userId = getCurrentUserId()
        if (userId == null) {
            Log.e("FirebaseHelper", "currentUserId is null. Cannot update contact.")
            callback(false)
            return
        }

        // Get the contact reference by phone number
        usersCollection.document(userId)
            .collection("contacts")
            .whereEqualTo("phone", updatedContact.phone)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    for (document in result.documents) {
                        // Update the contact
                        document.reference.update(
                            "name", updatedContact.name,
                            "phone", updatedContact.phone,
                            "timestamp", System.currentTimeMillis()
                        )
                            .addOnSuccessListener {
                                callback(true)
                            }
                            .addOnFailureListener { exception ->
                                Log.e("FirebaseHelper", "Error updating contact: ${exception.message}")
                                callback(false)
                            }
                    }
                } else {
                    callback(false)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("FirebaseHelper", "Error finding contact to update: ${exception.message}")
                callback(false)
            }
    }

    // Sign out the current user
    fun signOut() {
        currentUserId = null
    }

    companion object {
        private val usersCollection = Firebase.firestore.collection("users")
    }
}

// Data class for Contact
data class Contact(val name: String, val phone: String)
