package com.example.crud2

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, "UserDB", null, 1) {

    // Database creation SQL statement
    private val DATABASE_CREATE = ("create table users ("
            + "id integer primary key autoincrement,"
            + "username text,"
            + "password text,"
            + "email text,"
            + "number text,"
            + "isAdmin integer default 0"  // Default to 0 (not an admin)
            + ");")

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DATABASE_CREATE)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    // Insert a new user into the database
    fun insertUser(username: String, password: String, email: String): Long {
        val db = writableDatabase
        val values = ContentValues()
        values.put("username", username)
        values.put("password", password)
        values.put("email", email)
        return db.insert("users", null, values)
    }

    // Get a user by username
    fun getUser(username: String): User? {
        val db = readableDatabase
        val cursor = db.query("users", null, "username=?", arrayOf(username), null, null, null)
        if (cursor.moveToFirst()) {
            return User(
                cursor.getInt(cursor.getColumnIndexOrThrow("id")),
                cursor.getString(cursor.getColumnIndexOrThrow("username")),
                cursor.getString(cursor.getColumnIndexOrThrow("password")),
                cursor.getString(cursor.getColumnIndexOrThrow("email")),
                cursor.getString(cursor.getColumnIndexOrThrow("number")),
                cursor.getInt(cursor.getColumnIndexOrThrow("isAdmin"))  // Retrieve isAdmin value
            )
        }
        return null
    }

    // Check if a user with the given email already exists
    fun isEmailExists(email: String): Boolean {
        val db = readableDatabase
        val cursor = db.query("users", null, "email=?", arrayOf(email), null, null, null)
        val exists = cursor.moveToFirst()
        cursor.close()
        return exists
    }

    // Update the user's number
    fun updateUserNumber(username: String, number: String): Int {
        val db = writableDatabase
        val values = ContentValues()
        values.put("number", number)
        return db.update("users", values, "username=?", arrayOf(username))
    }

    // Get the user's number by username
    fun getUserNumber(username: String): String? {
        val db = readableDatabase
        val cursor = db.query("users", arrayOf("number"), "username=?", arrayOf(username), null, null, null)
        if (cursor.moveToFirst()) {
            val number = cursor.getString(cursor.getColumnIndexOrThrow("number"))
            cursor.close()
            return number
        }
        cursor.close()
        return null
    }

    // Add admin account if it doesn't already exist
    fun addAdminAccount() {
        val db = this.writableDatabase
        // Check if the admin account already exists
        val query = "SELECT * FROM users WHERE username = ?"
        val cursor = db.rawQuery(query, arrayOf("admin"))

        if (cursor.count == 0) {  // If admin doesn't exist, insert it
            val values = ContentValues()
            values.put("username", "admin")
            values.put("password", "admin123")  // Default password for the admin (you should hash it in a real app)
            values.put("email", "admin@example.com")  // Admin email
            values.put("isAdmin", 1)  // 1 indicates admin
            db.insert("users", null, values)
        }
        cursor.close()
    }
}

// User data class with added 'isAdmin' field
data class User(
    val id: Int,
    val username: String,
    val password: String,
    val email: String,
    val number: String,
    val isAdmin: Int  // 0 for regular user, 1 for admin
)
