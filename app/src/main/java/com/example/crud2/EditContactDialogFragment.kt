package com.example.crud2

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.fragment.app.DialogFragment

class EditContactDialogFragment(
    private val originalContact: Contact,
    private val onContactUpdated: (Contact) -> Unit  // This expects only one parameter
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = requireActivity().layoutInflater.inflate(R.layout.dialog_edit_contact, null)

        val nameEditText: EditText = view.findViewById(R.id.editName)
        val phoneEditText: EditText = view.findViewById(R.id.editPhone)

        // Pre-fill the EditText fields with the existing contact details
        nameEditText.setText(originalContact.name)
        phoneEditText.setText(originalContact.phone)

        builder.setView(view)
            .setPositiveButton("Update") { dialog, id ->
                val updatedName = nameEditText.text.toString()
                val updatedPhone = phoneEditText.text.toString()
                val updatedContact = Contact(updatedName, updatedPhone)
                onContactUpdated(updatedContact)  // Just pass the updated contact
            }
            .setNegativeButton("Cancel", null)

        return builder.create()
    }
}