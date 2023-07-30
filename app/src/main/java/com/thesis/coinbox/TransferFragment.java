package com.thesis.coinbox;

import static com.thesis.coinbox.utilities.Constants.TRANSACTIONS_COLLECTION;
import static com.thesis.coinbox.utilities.Constants.USERS_COLLECTION;
import static com.thesis.coinbox.utilities.PhoneUtilities.formatMobileNumber;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.thesis.coinbox.placeholder.data.model.LoggedInUser;
import com.thesis.coinbox.placeholder.data.model.Transaction;
import com.thesis.coinbox.databinding.FragmentTransferBinding;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

public class TransferFragment extends RequireLoginFragment{
    private FragmentTransferBinding binding;
    private NavController navController;
    private float maintainingBalance = 100;

    public TransferFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentTransferBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        navController = Navigation.findNavController(requireActivity(), R.id.navHostFragment);

        binding.contactsButton.setOnClickListener(v -> {
            navController.navigate(R.id.contactsFragment);
        });

        TextWatcher afterTextChangedListener = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // ignore
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // ignore
            }

            @Override
            public void afterTextChanged(Editable s) {
                checkIfHasEnoughBalance();
            }
        };

        binding.editTextAmount.addTextChangedListener(afterTextChangedListener);

        binding.payButton.setOnClickListener(v -> {
           if(isContactNumberValid()){
               showConfirmationDialog();
           }
           else{
               Toast.makeText(requireContext(), "Invalid contact number!", Toast.LENGTH_LONG).show();
           }
        });
    }

    private void checkIfHasEnoughBalance() {
        String editAmount = binding.editTextAmount.getText().toString().trim();
        if(editAmount.isEmpty()) {
            binding.payButton.setEnabled(false);
            return;
        }

        float amountToSend = Float.parseFloat(editAmount);
        if(amountToSend <= 0) {
            binding.payButton.setEnabled(false);
            return;
        }

        binding.payButton.setEnabled((savingsAccount.getBalance() - amountToSend) >= maintainingBalance);

        if(!binding.payButton.isEnabled()){
            Toast.makeText(requireContext(),String.format(Locale.getDefault(), "%s%s %s","Not enough balance! ", getFormattedAmount(maintainingBalance),  "maintaining balance is required."), Toast.LENGTH_LONG).show();
        }
    }

    private void showConfirmationDialog() {
        String receiver = binding.textViewRecipientName.getText().toString();
        String amount = binding.editTextAmount.getText().toString().trim();
        float value = 0;
        if(!amount.isEmpty())
            value = Float.parseFloat(amount);

        if(receiver.trim().isEmpty())
            receiver = binding.editTextPhoneRecipient.getText().toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Confirmation")
        .setMessage(String.format("Are you sure you want to send %s to %s?", getFormattedAmount(value), receiver))
        .setPositiveButton("Yes", (dialog, which) -> {
            // Action to perform when the user confirms (e.g., proceed with the action)
            // ...
            checkIfReceiverExists().addOnCompleteListener(task -> {
                if(!task.isSuccessful()) {
                    Toast.makeText(requireContext(), task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }

                if(task.getResult().isEmpty()){
                    Toast.makeText(requireContext(), "Recipient is not yet registered!", Toast.LENGTH_LONG).show();
                    return;
                }

                LoggedInUser receiverUser = task.getResult().getDocuments().get(0).toObject(LoggedInUser.class);

                assert receiverUser != null;
                if(receiverUser.getSavingsRef() == null)
                {
                    Toast.makeText(requireContext(), "Recipient doesn't have a savings account yet!", Toast.LENGTH_LONG).show();
                    return;
                }

                proceedPayment(loggedInUser, receiverUser, getAmount());
            });
        })
        .setNegativeButton("No", (dialog, which) -> {

            // Action to perform when the user cancels or dismisses the dialog (e.g., do nothing or handle cancellation)
            // ...
        })
        .setCancelable(false) // Prevent dismissing the dialog by tapping outside or pressing the back button
        .show();
    }

    private void proceedPayment(LoggedInUser sender, LoggedInUser receiver, float amount) {
        if(sender.getSavingsRef() == receiver.getSavingsRef())
            return;

        if(amount <= 0)
            return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        batch.update(receiver.getSavingsRef(), "balance", FieldValue.increment(amount));
        batch.update(sender.getSavingsRef(), "balance", FieldValue.increment(-amount));

        DocumentReference documentReference = db.collection(TRANSACTIONS_COLLECTION).document();
        Transaction transaction = new Transaction();
        transaction.setType("money");
        transaction.setAmount(amount);
        transaction.setDate(new Date());
        transaction.setSender(db.collection(USERS_COLLECTION).document(sender.getUserId()));
        transaction.setReceiver(db.collection(USERS_COLLECTION).document(receiver.getUserId()));
        transaction.setId(documentReference.getId());

        batch.set(documentReference, transaction);

        batch.commit()
        .addOnSuccessListener(aVoid -> {
            // Batch write succeeded
            // Handle success case
            // ...
            AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
            builder.setTitle("Success")
                    .setMessage("Transferred successfully")
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
                // Batch write failed
                // Handle failure case
                // ...
                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }


    @Override
    public void onResume() {
        super.onResume();
        if(getArguments() != null){
            String selectedContactNumber = formatMobileNumber(getArguments().getString("contactNumber").trim());
            String selectedContactName = getArguments().getString("contactName");

            binding.editTextPhoneRecipient.setText(selectedContactNumber);
            binding.textViewRecipientName.setText(selectedContactName);
        }
    }

    private boolean isContactNumberValid(){
        if(binding.editTextPhoneRecipient.getText().toString().isEmpty()) {
            return false;
        }
        return true;
    }

    @Override
    public void onFetchedUser() {
        binding.textViewRemainingBalance.setText(String.format("%s PHP %s", getString(R.string.available_balance), getFormattedAmount(savingsAccount.getBalance())));
    }

    private float getAmount(){
        float amount = 0;
        String editAmount = binding.editTextAmount.getText().toString().trim();

        try {
            amount = Float.parseFloat(editAmount);
        }
        catch (NumberFormatException ignored){

        }

        return amount;
    }
    private String getFormattedAmount(float amount){
        Locale locale = new Locale("en", "PH");
        Currency currency = Currency.getInstance(locale);
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        currencyFormatter.setCurrency(currency);

        return currencyFormatter.format(amount);
    }

    private Task<QuerySnapshot> checkIfReceiverExists(){
        String receiver = formatMobileNumber(binding.editTextPhoneRecipient.getText().toString().trim());
        return FirebaseFirestore.getInstance().collection(USERS_COLLECTION).whereEqualTo("phone", receiver).get();
    }
}