package com.example.crud2

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirebaseHelper(context: Context) {

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // Firebase collections
    private val usersCollection = db.collection("users")

    // Method to add a user to Firestore
    fun addUser(
        username: String,
        password: String,
        email: String,
        phone: String,
        callback: (Boolean) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return callback(false)

        val user = hashMapOf(
            "username" to username,
            "password" to password,
            "email" to email,
            "phone" to phone,
            "userId" to userId // Associate the account with a unique identifier
        )

        // Add a new document for the user
        usersCollection.document(userId).set(user)
            .addOnSuccessListener {
                println("User added with ID: $userId")
                callback(true) // Callback with success
            }
            .addOnFailureListener { e ->
                println("Error adding user: $e")
                callback(false) // Callback with failure
            }
    }

    // Method to validate user by username and password
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
                if (result.isEmpty) {
                    callback(false)
                } else {
                    callback(true)
                }
            }
            .addOnFailureListener { e ->
                println("Error validating user: $e")
                callback(false)
            }
    }

    // Method to add a contact for the logged-in user
    fun addContact(name: String, phone: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            println("Error: User not logged in")
            return
        }

        val contact = hashMapOf(
            "name" to name,
            "phone" to phone,
            "userId" to userId
        )

        usersCollection.document(userId).collection("contacts").add(contact)
            .addOnSuccessListener { documentReference ->
                println("Contact added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                println("Error adding contact: $e")
            }
    }

    // Method to get all contacts for the logged-in user
    fun getAllContacts(callback: (List<Contact>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            println("Error: User not logged in")
            callback(emptyList())
            return
        }

        usersCollection.document(userId).collection("contacts").get()
            .addOnSuccessListener { result ->
                val contactList = mutableListOf<Contact>()
                for (document in result) {
                    val name = document.getString("name") ?: ""
                    val phone = document.getString("phone") ?: ""
                    contactList.add(Contact(name, phone))
                }
                callback(contactList)
            }
            .addOnFailureListener { e ->
                println("Error getting contacts: $e")
            }
    }

    // Method to delete a contact by phone number
    fun deleteContactByPhone(phone: String) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            println("Error: User not logged in")
            return
        }

        usersCollection.document(userId).collection("contacts")
            .whereEqualTo("phone", phone)
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    document.reference.delete()
                        .addOnSuccessListener {
                            println("Contact deleted")
                        }
                        .addOnFailureListener { e ->
                            println("Error deleting contact: $e")
                        }
                }
            }
            .addOnFailureListener { e ->
                println("Error getting contacts: $e")
            }
    }

    // Method to delete all contacts for the logged-in user
    fun deleteAllContacts() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            println("Error: User not logged in")
            return
        }

        usersCollection.document(userId).collection("contacts").get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    document.reference.delete()
                        .addOnSuccessListener {
                            println("Contact deleted")
                        }
                        .addOnFailureListener { e ->
                            println("Error deleting contact: $e")
                        }
                }
            }
            .addOnFailureListener { e ->
                println("Error getting contacts: $e")
            }
    }
}

data class Contact(val name: String, val phoneNumber: String)
