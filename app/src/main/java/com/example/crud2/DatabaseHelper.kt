package com.example.crud2

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "userDatabase"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "users"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_PASSWORD = "password"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PHONE = "phone"
    }

    override fun onCreate(db: SQLiteDatabase?) {
        val createTableQuery = "CREATE TABLE $TABLE_NAME (" +
                "$COLUMN_USERNAME TEXT PRIMARY KEY, " +
                "$COLUMN_PASSWORD TEXT, " +
                "$COLUMN_EMAIL TEXT, " +
                "$COLUMN_PHONE TEXT)"
        db?.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    // Insert user into the database
    fun insertUser(username: String, password: String, email: String, phone: String): Long {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_PASSWORD, password)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PHONE, phone)
        }
        return db.insert(TABLE_NAME, null, values)
    }

    // Validate user by username and password
    fun validateUserByUsernameAndPassword(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM $TABLE_NAME WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?",
            arrayOf(username, password)
        )

        return cursor.count > 0  // If cursor count is greater than 0, the user exists
    }

    // Method to retrieve the phone number by username
    fun getUserPhoneNumber(username: String): String? {
        val db = this.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT $COLUMN_PHONE FROM $TABLE_NAME WHERE $COLUMN_USERNAME = ?",
            arrayOf(username)
        )

        var phoneNumber: String? = null
        if (cursor.moveToFirst()) {
            phoneNumber = cursor.getString(cursor.getColumnIndex(COLUMN_PHONE))
        }
        cursor.close()
        return phoneNumber
    }

    // Method to update the user's phone number
    fun updateUserPhoneNumber(username: String, newPhoneNumber: String): Int {
        val db = this.writableDatabase
        val values = ContentValues().apply {
            put(COLUMN_PHONE, newPhoneNumber)
        }

        return db.update(TABLE_NAME, values, "$COLUMN_USERNAME = ?", arrayOf(username))
    }
}
