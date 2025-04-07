package com.example.crud2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment

class AddContactDialogFragment(
    private val onAddContact: (Contact) -> Unit
) : DialogFragment() {

    private lateinit var nameEditText: EditText
    private lateinit var phonePrefixEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var addButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.dialog_add_contact, container, false)
        nameEditText = view.findViewById(R.id.contactNameEditText)
        phonePrefixEditText = view.findViewById(R.id.contactPhonePrefixEditText)
        phoneEditText = view.findViewById(R.id.contactPhoneEditText)
        addButton = view.findViewById(R.id.addContactButton)

        // Set the prefix for the phone number
        phonePrefixEditText.setText("+63")

        addButton.setOnClickListener {
            val name = nameEditText.text.toString().trim()
            val phone = phoneEditText.text.toString().trim()

            // Ensure the phone number part is not empty
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                // Combine the prefix with the phone number
                val fullPhoneNumber = "+63$phone"

                // Validate phone number length (should be 13 digits, including +63)
                if (fullPhoneNumber.length == 13) {  // Valid phone number length for Philippines
                    onAddContact(Contact(name, fullPhoneNumber))
                    dismiss()
                } else {
                    Toast.makeText(context, "Please enter a valid phone number.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(context, "Please fill in both name and phone number.", Toast.LENGTH_SHORT).show()
            }
        }

        return view
    }
}
