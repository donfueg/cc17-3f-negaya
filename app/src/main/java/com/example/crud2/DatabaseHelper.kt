package com.example.crud2

import android.content.Context
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class FirebaseHelper(context: Context) {

    private val db = Firebase.firestore
    private val auth = FirebaseAuth.getInstance()

    // Firebase collections
    private val usersCollection = db.collection("users")
    private val contactsCollection = db.collection("contacts")

    // Method to add a user to Firestore
    fun addUser(
        username: String,
        password: String,
        email: String,
        phone: String,
        callback: (Boolean) -> Unit
    ) {
        val user = hashMapOf(
            "username" to username,
            "password" to password,
            "email" to email,
            "phone" to phone
        )

        // Add a new document with a generated ID
        usersCollection.add(user)
            .addOnSuccessListener { documentReference ->
                println("User added with ID: ${documentReference.id}")
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
                println("Error getting documents: $e")
                callback(false)
            }
    }

    // Method to add a contact to Firestore
    fun addContact(name: String, phone: String) {
        val contact = hashMapOf(
            "name" to name,
            "phone" to phone
        )

        // Add a new document with a generated ID
        contactsCollection.add(contact)
            .addOnSuccessListener { documentReference ->
                println("Contact added with ID: ${documentReference.id}")
            }
            .addOnFailureListener { e ->
                println("Error adding contact: $e")
            }
    }

    // Method to get all contacts from Firestore
    fun getAllContacts(callback: (List<Contact>) -> Unit) {
        contactsCollection.get()
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
        contactsCollection
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

    // Method to delete all contacts
    fun deleteAllContacts() {
        contactsCollection
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
}