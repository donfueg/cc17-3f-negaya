package com.example.crud2

import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Button

class AddContactDialogFragment(
    private val onAddContact: (Contact) -> Unit
) : DialogFragment() {

    private lateinit var nameEditText: EditText
    private lateinit var phoneEditText: EditText
    private lateinit var addButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_add_contact, container, false)
        nameEditText = view.findViewById(R.id.contactNameEditText)
        phoneEditText = view.findViewById(R.id.contactPhoneEditText)
        addButton = view.findViewById(R.id.addContactButton)

        addButton.setOnClickListener {
            val name = nameEditText.text.toString()
            val phone = phoneEditText.text.toString()

            if (name.isNotEmpty() && phone.isNotEmpty()) {
                onAddContact(Contact(name, phone))
                dismiss()
            }
        }

        return view
    }
}
