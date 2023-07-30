package com.thesis.coinbox.adapters;

import androidx.annotation.NonNull;
import androidx.navigation.NavController;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.thesis.coinbox.R;
import com.thesis.coinbox.placeholder.data.model.Contact;
import com.thesis.coinbox.placeholder.PlaceholderContent.PlaceholderItem;

import java.util.List;

/**
 * {@link RecyclerView.Adapter} that can display a {@link PlaceholderItem}.
 * TODO: Replace the implementation with code for your data type.
 */
public class ContactRecyclerViewAdapter extends RecyclerView.Adapter<ContactRecyclerViewAdapter.ContactViewHolder> {

    private final List<Contact> contactList;
    private NavController navController;
    private int previousFragmentId;

    public ContactRecyclerViewAdapter(List<Contact> contactList, NavController navController, int previousFragmentId) {
        this.contactList = contactList;
        this.navController = navController;
        this.previousFragmentId = previousFragmentId;
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_contact, parent, false);
        return new ContactViewHolder(itemView);
    }


    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        Contact contact = contactList.get(position);
        holder.textViewContactName.setText(contact.getName());
        holder.textViewContactNumber.setText(contact.getNumber());
        holder.parent.setTag(contact);
        holder.parent.setOnClickListener(contactClickListener);
    }

    private final View.OnClickListener contactClickListener = v -> {
        if(navController == null)
            return;


        Contact contact = (Contact)v.getTag();
        Bundle bundle = new Bundle();
        bundle.putString("contactName", contact.getName());
        bundle.putString("contactNumber", contact.getNumber());

        navController.navigate(previousFragmentId, bundle);
    };

    @Override
    public int getItemCount() {
        return contactList.size();
    }

    public static class ContactViewHolder extends RecyclerView.ViewHolder {
        public TextView textViewContactName;
        public TextView textViewContactNumber;
        public View parent;

        public ContactViewHolder(View itemView) {
            super(itemView);
            textViewContactName = itemView.findViewById(R.id.textViewContactName);
            textViewContactNumber = itemView.findViewById(R.id.textViewContactNumber);
            parent = itemView.findViewById(R.id.parent);
        }
    }
}