package com.example.crud2

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.DialogFragment

class AddContactDialogFragment(val onAddContact: (Contact) -> Unit) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // Inflate the layout for the dialog
        val dialogView = requireActivity().layoutInflater.inflate(R.layout.dialog_add_contact, null)

        // Initialize the EditText fields and Button
        val nameEditText: EditText = dialogView.findViewById(R.id.contactNameEditText)
        val phoneEditText: EditText = dialogView.findViewById(R.id.contactPhoneEditText)
        val addButton: Button = dialogView.findViewById(R.id.addContactButton)

        // Set the button click listener
        addButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val phone = phoneEditText.text.toString()

            // Ensure both fields are not empty before adding the contact
            if (name.isNotEmpty() && phone.isNotEmpty()) {
                onAddContact(Contact(name, phone))
                dismiss()  // Close the dialog
            }
        }

        return Dialog(requireActivity()).apply {
            setContentView(dialogView)
        }
    }
}