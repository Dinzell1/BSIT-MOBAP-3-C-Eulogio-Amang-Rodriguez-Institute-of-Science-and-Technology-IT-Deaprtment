package com.thesis.coinbox;

import static com.thesis.coinbox.utilities.PhoneUtilities.formatMobileNumber;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.thesis.coinbox.databinding.FragmentHomeBinding;
import com.thesis.coinbox.ui.main.MainActivity;

import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;

public class HomeFragment extends RequireLoginFragment{

    private FragmentHomeBinding binding;
    private NavController navController;

    public HomeFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {


        navController = Navigation.findNavController(requireActivity(), R.id.navHostFragment);

        binding.transferButton.setOnClickListener(v -> {
            navController.navigate(R.id.transferFragment);
        });

        binding.eLoadButton.setOnClickListener(v -> {
            navController.navigate(R.id.ELoadFragment);
        });

        binding.transactionsButton.setOnClickListener(v -> {
            navController.navigate(R.id.transactionsFragment);
        });

        binding.cashInButton.setOnClickListener(v -> {
            navController.navigate(R.id.cashInFragment);
        });
        ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        if ( actionBar != null ) {
            actionBar.setDisplayHomeAsUpEnabled(false);
        }
    }


    private void updateSavingsUI() {
        String welcomeText = String.format("%s %s", getString(R.string.welcome), loggedInUser.getDisplayName());
        binding.welcomeText.setText(welcomeText);

        Locale locale = new Locale("en", "PH");
        Currency currency = Currency.getInstance(locale);
        NumberFormat currencyFormatter = NumberFormat.getCurrencyInstance(locale);
        currencyFormatter.setCurrency(currency);
        String formattedBalance = currencyFormatter.format(savingsAccount.getBalance());
        binding.balance.setText(formattedBalance);
        binding.phoneNumber.setText(formatMobileNumber(loggedInUser.getPhone()));
    }

    @Override
    public void onFetchedUser() {
        updateSavingsUI();
    }


}