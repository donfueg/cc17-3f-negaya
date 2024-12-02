package com.example.crud2

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "appDatabase"
        private const val DATABASE_VERSION = 1

        // User table
        private const val TABLE_USERS = "users"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PHONE = "phone"

        // Contact table
        private const val TABLE_CONTACTS = "contacts"
        private const val COLUMN_CONTACT_ID = "id"
        private const val COLUMN_CONTACT_NAME = "name"
        private const val COLUMN_CONTACT_PHONE = "phone"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        // Create User table
        val createUserTableQuery = "CREATE TABLE $TABLE_USERS (" +
                "$COLUMN_USERNAME TEXT PRIMARY KEY, " +
                "$COLUMN_PASSWORD TEXT, " +
                "$COLUMN_EMAIL TEXT, " +
                "$COLUMN_PHONE TEXT)"
        db?.execSQL(createUserTableQuery)

        // Create Contact table
        val createContactTableQuery = "CREATE TABLE $TABLE_CONTACTS (" +
                "$COLUMN_CONTACT_ID INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "$COLUMN_CONTACT_NAME TEXT NOT NULL, " +
                "$COLUMN_CONTACT_PHONE TEXT NOT NULL)"
        db?.execSQL(createContactTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_CONTACTS")
        onCreate(db)
    }

    // Method to insert a user into the users table
    fun insertUser(username: String, password: String, email: String, phone: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PHONE, phone)
        }
        return db.insert(TABLE_USERS, null, values)
    }

    // Method to validate a user by username and password
    fun validateUserByUsernameAndPassword(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(username, password)
        )

        val userExists = cursor.count > 0
        cursor.close()
        return userExists
    }

    // Methods for managing contacts
    fun addContact(name: String, phone: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_CONTACT_NAME, name)
            put(COLUMN_CONTACT_PHONE, phone)
        }
        return db.insert(TABLE_CONTACTS, null, values)
    }

    fun getAllContacts(): MutableList<Contact> {
        val contactList = mutableListOf<Contact>()
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM $TABLE_CONTACTS", null)

        while (cursor.moveToNext()) {
            val name = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_NAME))
            val phone = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_CONTACT_PHONE))
            contactList.add(Contact(name, phone))
        }
        cursor.close()
        return contactList
    }

    fun deleteAllContacts(): Int {
        val db = this.writableDatabase
        return db.delete(TABLE_CONTACTS, null, null)
    }
}
