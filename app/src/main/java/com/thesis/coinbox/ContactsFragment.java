package com.thesis.coinbox;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.thesis.coinbox.adapters.ContactRecyclerViewAdapter;
import com.thesis.coinbox.placeholder.data.model.Contact;

import java.util.ArrayList;
import java.util.List;

/**
 * A fragment representing a list of Items.
 */
public class ContactsFragment extends RequireLoginFragment {
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private final String READ_CONTACTS_PERMISSION = android.Manifest.permission.READ_CONTACTS;
    private RecyclerView recyclerView;
    private AlertDialog alertDialog;
    private NavController navController;

    public ContactsFragment() {
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_contacts_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        navController = Navigation.findNavController(requireActivity(), R.id.navHostFragment);
        // Set the adapter
        if (view instanceof RecyclerView) {
            Context context = view.getContext();
            recyclerView = (RecyclerView) view;
            recyclerView.setLayoutManager(new LinearLayoutManager(context));

            requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Permission is granted, proceed with contact-related operations
                    // ...
                    List<Contact> contacts = getContacts();
                    int previousFragmentId = navController.getPreviousBackStackEntry().getDestination().getId();
                    recyclerView.setAdapter(new ContactRecyclerViewAdapter(contacts, navController, previousFragmentId));
                } else {
                    // Permission is denied, handle the situation (e.g., show a message or disable contact-related functionality)
                    // ...
                    showPermissionDeniedDialog();
                }
            });

            // Check if the permission is already granted
            if (ContextCompat.checkSelfPermission(requireContext(), READ_CONTACTS_PERMISSION) == PackageManager.PERMISSION_GRANTED) {
                // Permission is already granted, proceed with contact-related operations
                // ...
                List<Contact> contacts = getContacts();
                int previousFragmentId = navController.getPreviousBackStackEntry().getDestination().getId();
                recyclerView.setAdapter(new ContactRecyclerViewAdapter(contacts, navController, previousFragmentId));
            } else {
                // Permission is not granted, request it from the user
                requestPermissionLauncher.launch(READ_CONTACTS_PERMISSION);
            }
        }
    }

    private List<Contact> getContacts() {
        List<Contact> contacts = new ArrayList<>();

        Cursor cursor = getActivity().getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        );

        if (cursor != null && cursor.moveToFirst()) {
            do {
                int nameColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
                int phoneNumberColumnIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

                String contactName = cursor.getString(nameColumnIndex);
                String phoneNumber = cursor.getString(phoneNumberColumnIndex);
                Contact contact = new Contact(contactName, phoneNumber);
                contacts.add(contact);
            } while (cursor.moveToNext());

            cursor.close();
        }

        return contacts;
    }

    private void showPermissionDeniedDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Permission Required")
                .setMessage("This app requires the READ_CONTACTS permission to function properly. Please grant the permission in the app settings.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    // Open app settings screen
                    openAppSettings();
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    // Handle cancel button click if needed
                    // ...
                })
                .setCancelable(false);

        alertDialog = builder.create();
        alertDialog.show();
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getActivity().getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }
}