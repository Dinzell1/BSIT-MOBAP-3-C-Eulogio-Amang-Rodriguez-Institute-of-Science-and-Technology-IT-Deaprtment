package com.thesis.coinbox;

import static com.thesis.coinbox.utilities.Constants.TRANSACTIONS_COLLECTION;
import static com.thesis.coinbox.utilities.Constants.USERS_COLLECTION;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.WriteBatch;
import com.thesis.coinbox.placeholder.data.model.Transaction;
import com.thesis.coinbox.databinding.FragmentCashInBinding;

import java.util.Date;

public class CashInFragment extends RequireLoginFragment {

    private FragmentCashInBinding binding;
    private AlertDialog dialog;
    private NavController navController;
    private String overTheCounterId = "ILGpdV8Ecv0MgheYXu8D";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentCashInBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(requireActivity(), R.id.navHostFragment);
    }

    private void createInputDialog() {
        // Create an AlertDialog.Builder
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Entered cash in amount");

// Inflate a custom layout for the dialog
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogView = inflater.inflate(R.layout.dialog_input_number, null);
        builder.setView(dialogView);

// Get a reference to the EditText field in the custom layout
        EditText numberEditText = dialogView.findViewById(R.id.numberEditText);

// Set the positive button and its click listener
        builder.setPositiveButton("OK", (dialog, which) -> {
            // Retrieve the entered number
            String numberString = numberEditText.getText().toString().trim();
            if (!numberString.isEmpty()) {
                // Parse the entered number as a float
                float number = Float.parseFloat(numberString);
                if (number > 0) {
                    // Number is positive, do something with it
                    // e.g., display a toast message with the entered number
                    proceedCashIn(number);
                } else {
                    // Number is not positive, show an error message
                    Toast.makeText(requireContext(), "Please enter a positive number", Toast.LENGTH_SHORT).show();
                }
            } else {
                // No number entered, show an error message
                Toast.makeText(requireContext(), "Please enter a number", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which)->{
            dialog.dismiss();
        });

        dialog = builder.create();
    }

    private void proceedCashIn(float amount) {
        binding.progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        batch.update(loggedInUser.getSavingsRef(), "balance", FieldValue.increment(amount));

        DocumentReference documentReference = db.collection(TRANSACTIONS_COLLECTION).document();
        Transaction transaction = new Transaction();
        transaction.setType("money");
        transaction.setAmount(amount);
        transaction.setDate(new Date());
        transaction.setSender(db.collection(USERS_COLLECTION).document(overTheCounterId));
        transaction.setReceiver(db.collection(USERS_COLLECTION).document(loggedInUser.getUserId()));
        transaction.setId(documentReference.getId());

        batch.set(documentReference, transaction);

        batch.commit()
                .addOnSuccessListener(aVoid -> {
                    binding.progressBar.setVisibility(View.GONE);
                    // Batch write succeeded
                    // Handle success case
                    // ...
                    AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
                    builder.setTitle("Success")
                            .setMessage("Cash in successfully")
                            .setPositiveButton("OK", (dialog, which) -> {
                                // Action to perform when the "OK" button is clicked
                                // ...
                                dialog.dismiss();
                                navController.navigate(R.id.homeFragment);
                            })
                            .setCancelable(false) // Prevent dismissing the dialog by tapping outside or pressing the back button
                            .show();
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        binding.progressBar.setVisibility(View.GONE);
                        // Batch write failed
                        // Handle failure case
                        // ...
                        Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onFetchedUser() {
        createInputDialog();

        binding.overTheCounterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.show();
            }
        });
    }
}