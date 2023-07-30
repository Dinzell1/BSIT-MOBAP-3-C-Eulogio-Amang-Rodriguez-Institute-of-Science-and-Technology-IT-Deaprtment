package com.thesis.coinbox;

import static com.thesis.coinbox.utilities.Constants.TRANSACTIONS_COLLECTION;
import static com.thesis.coinbox.utilities.Constants.USERS_COLLECTION;
import static com.thesis.coinbox.utilities.PhoneUtilities.formatMobileNumber;

import android.app.AlertDialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;
import com.thesis.coinbox.placeholder.data.model.LoggedInUser;
import com.thesis.coinbox.placeholder.data.model.Transaction;
import com.thesis.coinbox.databinding.FragmentELoadBinding;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

public class ELoadFragment extends RequireLoginFragment {

    private FragmentELoadBinding binding;
    private NavController navController;
    private LoggedInUser receiver;
    private String selectedSIM;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        binding = FragmentELoadBinding.inflate(inflater, container, false);
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

        TextWatcher afterPhoneTextChangedListener = new TextWatcher() {
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
                fetchReceiverUser();
            }
        };
        binding.editTextPhoneRecipient.addTextChangedListener(afterPhoneTextChangedListener);

        binding.editTextAmount.addTextChangedListener(afterTextChangedListener);

        binding.payButton.setOnClickListener(v -> {
            if(isContactNumberValid()){
                if(selectedSIM == null || selectedSIM.isEmpty()){
                    Toast.makeText(requireContext(), "Please select a valid SIM provider", Toast.LENGTH_LONG).show();
                    return;
                }
                showConfirmationDialog();
            }
            else{
                Toast.makeText(requireContext(), "Invalid contact number!", Toast.LENGTH_LONG).show();
            }
        });

        binding.globeButton.setOnClickListener(onRadioButtonClicked);
        binding.smartButton.setOnClickListener(onRadioButtonClicked);
        binding.tntButton.setOnClickListener(onRadioButtonClicked);
        binding.tmButton.setOnClickListener(onRadioButtonClicked);
        binding.ditoButton.setOnClickListener(onRadioButtonClicked);
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

        binding.payButton.setEnabled((savingsAccount.getBalance() - amountToSend) >= 0 && receiver != null);

        if(receiver == null){
            binding.editTextPhoneRecipient.setError("Receiver doesn't have an account");
            return;
        }

        if(!binding.payButton.isEnabled()){
            binding.editTextAmount.setError("Not enough balance!");
        }
    }

    private void showConfirmationDialog() {
        String receiver = binding.textViewRecipientName.getText().toString();
        String amount = binding.editTextAmount.getText().toString().trim();
        float value = 0;
        if(!amount.isEmpty())
            value = Float.parseFloat(amount);

        if(receiver.isEmpty())
            receiver = binding.editTextPhoneRecipient.getText().toString();

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Confirmation")
                .setMessage(String.format("Are you sure the receiving number is correct?\n Amount of %s will be loaded to %s?", getFormattedAmount(value), receiver))
                .setPositiveButton("Yes", (dialog, which) -> {
                    proceedPayment(loggedInUser, getAmount());
                })
                .setNegativeButton("No", (dialog, which) -> {

                    // Action to perform when the user cancels or dismisses the dialog (e.g., do nothing or handle cancellation)
                    // ...
                })
                .setCancelable(false) // Prevent dismissing the dialog by tapping outside or pressing the back button
                .show();
    }

    private void proceedPayment(LoggedInUser sender, float amount) {
        if(amount == 0)
            return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        WriteBatch batch = db.batch();

        batch.update(sender.getSavingsRef(), "balance", FieldValue.increment(-amount));

        DocumentReference documentReference = db.collection(TRANSACTIONS_COLLECTION).document();
        Transaction transaction = new Transaction();
        transaction.setType("e-load");
        transaction.setAmount(amount);
        transaction.setDate(new Date());
        transaction.setSender(FirebaseFirestore.getInstance().collection(USERS_COLLECTION).document(sender.getUserId()));
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
                        .setMessage("Loaded successfully")
                        .setPositiveButton("OK", (dialog, which) -> {
                            // Action to perform when the "OK" button is clicked
                            // ...
                            dialog.dismiss();
                            navController.navigate(R.id.homeFragment);
                        })
                        .setCancelable(false) // Prevent dismissing the dialog by tapping outside or pressing the back button
                        .show();
            })
            .addOnFailureListener(e -> {

                // Batch write failed
                // Handle failure case
                // ...
                Toast.makeText(requireContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            });
    }

    private View.OnClickListener onRadioButtonClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            selectedSIM = view.getTag().toString();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        if(getArguments() != null){
            String selectedContactNumber = formatMobileNumber(getArguments().getString("contactNumber").trim());
            String selectedContactName = getArguments().getString("contactName");

            binding.editTextPhoneRecipient.setText(selectedContactNumber);
            binding.textViewRecipientName.setText(selectedContactName);
            fetchReceiverUser();
        }
    }


    private void fetchReceiverUser(){
        receiver = null;
        binding.payButton.setEnabled(false);
        if(!isContactNumberValid())
            return;

        FirebaseFirestore.getInstance().collection(USERS_COLLECTION).whereEqualTo("phone", binding.editTextPhoneRecipient.getText().toString().trim())
                .get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            if(task.getResult().isEmpty()) {
                                binding.editTextPhoneRecipient.setError("Receiver doesn't have an account");
                                return;
                            }
                            if(getAmount() > 0) {
                                binding.payButton.setEnabled(true);
                            }

                            receiver = task.getResult().getDocuments().get(0).toObject(LoggedInUser.class);

                            binding.editTextPhoneRecipient.setError(null);
                        }
                        else{
                            binding.payButton.setEnabled(false);
                            Toast.makeText(requireContext(),task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private boolean isContactNumberValid(){
        return !binding.editTextPhoneRecipient.getText().toString().isEmpty();
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
}