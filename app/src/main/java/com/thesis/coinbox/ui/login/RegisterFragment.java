package com.thesis.coinbox.ui.login;

import static com.thesis.coinbox.utilities.Constants.SAVINGS_COLLECTION;
import static com.thesis.coinbox.utilities.Constants.USERS_COLLECTION;
import static com.thesis.coinbox.utilities.PhoneUtilities.formatMobileNumber;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.thesis.coinbox.R;
import com.thesis.coinbox.placeholder.data.model.LoggedInUser;
import com.thesis.coinbox.placeholder.data.model.Savings;
import com.thesis.coinbox.databinding.FragmentRegisterBinding;
import com.thesis.coinbox.ui.main.MainActivity;

public class RegisterFragment extends Fragment {

    private FragmentRegisterBinding binding;
    private FirebaseAuth firebaseAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        binding = FragmentRegisterBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseAuth = FirebaseAuth.getInstance();

        final EditText usernameEditText = binding.emailField;
        final EditText emailEditText = binding.email;
        final EditText passwordEditText = binding.password;
        final EditText phone = binding.editTextPhone;
        final Button registerButton = binding.register;
        final ProgressBar loadingProgressBar = binding.loading;

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
                boolean isUserNameValid = LoginViewModel.isEmailValid(usernameEditText.getText().toString().trim());
                boolean isPasswordValid = LoginViewModel.isPasswordValid(passwordEditText.getText().toString().trim());

                registerButton.setEnabled(isUserNameValid && isPasswordValid);

                if(!isUserNameValid)
                    usernameEditText.setError("Invalid");

                if(!isPasswordValid)
                    passwordEditText.setError("Invalid");
            }
        };
        usernameEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.addTextChangedListener(afterTextChangedListener);
        passwordEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if(isFormValid()) {
                    loadingProgressBar.setVisibility(View.VISIBLE);

                    checkPhoneAvailability().addOnCompleteListener(task -> {
                        if(!task.isSuccessful())
                        {
                            showLoginFailed(task.getException().getMessage());
                            loadingProgressBar.setVisibility(View.GONE);
                            return;
                        }

                        if(!task.getResult().isEmpty()){
                            showLoginFailed("Phone number was already in use!");
                            loadingProgressBar.setVisibility(View.GONE);
                            return;
                        }

                        String phoneNumber = phone.getText().toString().trim();

                        register(emailEditText.getText().toString(),
                                passwordEditText.getText().toString(), usernameEditText.getText().toString().trim(), phoneNumber);
                    });
                }
            }
            return false;
        });

        registerButton.setOnClickListener(v -> {
            if(isFormValid()) {
                loadingProgressBar.setVisibility(View.VISIBLE);
                checkPhoneAvailability().addOnCompleteListener(task -> {
                    if(!task.isSuccessful())
                    {
                        showLoginFailed(task.getException().getMessage());
                        loadingProgressBar.setVisibility(View.GONE);
                        return;
                    }

                    if(!task.getResult().isEmpty()){
                        showLoginFailed("Phone number was already in use!");
                        loadingProgressBar.setVisibility(View.GONE);
                        return;
                    }

                    String phoneNumber = phone.getText().toString().trim();

                    register(emailEditText.getText().toString(),
                            passwordEditText.getText().toString(), usernameEditText.getText().toString().trim(), phoneNumber);
                });
            }
        });
    }

    private Task<QuerySnapshot> checkPhoneAvailability(){
        String phoneNumber = binding.editTextPhone.getText().toString().trim();
        return FirebaseFirestore.getInstance().collection(USERS_COLLECTION).whereEqualTo("phone", phoneNumber).get();
    }

    public boolean isFormValid(){
        if(binding.emailField.getText().toString().trim().isEmpty()){
            binding.emailField.setError(getString(R.string.email_hint));
            return false;
        }

        if(binding.email.getText().toString().trim().isEmpty()){
            binding.email.setError(getString(R.string.email_hint));
            return false;
        }

        if(binding.password.getText().toString().trim().isEmpty()){
            binding.password.setError(getString(R.string.password_hint));
            return false;
        }

        if(binding.confirmPassword.getText().toString().trim().isEmpty()){
            binding.confirmPassword.setError(getString(R.string.password_hint));
            return false;
        }

        if(!binding.confirmPassword.getText().toString().trim().equals(binding.password.getText().toString().trim())){
            binding.confirmPassword.setError("Password doesn't match");
            return false;
        }

        if(binding.editTextPhone.getText().toString().trim().isEmpty()){
            binding.editTextPhone.setError("This field is required");
            return false;
        }

        return true;
    }

    public void register(String username, String password, String displayName, String phone) {
        String formattedPhone = formatMobileNumber(phone);

        firebaseAuth.createUserWithEmailAndPassword(username, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        String uid = task.getResult().getUser().getUid();
                        LoggedInUser data = new LoggedInUser(uid, displayName, formattedPhone);

                        DocumentReference newSavingsAccountRef = FirebaseFirestore.getInstance().collection(SAVINGS_COLLECTION).document(uid);

                        Savings newSavingsAccount = new Savings();
                        newSavingsAccountRef.set(newSavingsAccount).addOnCompleteListener(task1 -> {
                            data.setSavingsRef(newSavingsAccountRef);

                            FirebaseFirestore.getInstance().collection(USERS_COLLECTION).document(uid).set(data)
                            .addOnCompleteListener(task2 -> {
                                if(task2.isSuccessful()){
                                    startActivity(new Intent(requireContext(), MainActivity.class));
                                    requireActivity().finish();
                                }
                                else{
                                    showLoginFailed(task2.getException().getMessage());
                                }
                            });
                        });

                    } else {
                        showLoginFailed(task.getException().getMessage());
                    }
                });
    }


    private void showLoginFailed(String errorString) {
        if (getContext() != null && getContext().getApplicationContext() != null) {
            Toast.makeText(
                    getContext().getApplicationContext(),
                    errorString,
                    Toast.LENGTH_LONG).show();
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}