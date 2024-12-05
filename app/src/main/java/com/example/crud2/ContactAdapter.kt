package com.example.crud2

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ContactAdapter(
    private var contactList: List<Contact>,
    private val onContactClickListener: OnContactClickListener,
    private val onDeleteClickListener: (Contact) -> Unit
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]
        holder.nameTextView.text = contact.name
        holder.phoneTextView.text = contact.phoneNumber

        // Set up the delete button click listener
        holder.deleteButton.setOnClickListener {
            onDeleteClickListener(contact)  // Notify the activity to delete the contact
        }

        holder.itemView.setOnClickListener {
            onContactClickListener.onContactClick(contact.phoneNumber)
        }
    }

    override fun getItemCount(): Int = contactList.size

    fun updateContacts(newContacts: List<Contact>) {
        contactList = newContacts
        notifyDataSetChanged()
    }

    inner class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.contactName)
        val phoneTextView: TextView = view.findViewById(R.id.contactPhone)
        val deleteButton: ImageButton = view.findViewById(R.id.deleteButton)  // Delete button
    }

    interface OnContactClickListener {
        fun onContactClick(phoneNumber: String)
    }
}
