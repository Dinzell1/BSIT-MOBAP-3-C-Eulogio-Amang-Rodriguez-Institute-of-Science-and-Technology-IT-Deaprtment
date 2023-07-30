package com.thesis.coinbox;

import static com.thesis.coinbox.utilities.Constants.SAVINGS_COLLECTION;
import static com.thesis.coinbox.utilities.Constants.USERS_COLLECTION;

import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.Fragment;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.thesis.coinbox.placeholder.data.model.LoggedInUser;
import com.thesis.coinbox.placeholder.data.model.Savings;
import com.thesis.coinbox.ui.main.MainActivity;

import java.util.HashMap;
import java.util.Map;

public abstract class RequireLoginFragment extends Fragment {
    protected LoggedInUser loggedInUser;
    protected Savings savingsAccount;
    private FirebaseAuth firebaseAuth;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();

        if(firebaseAuth.getCurrentUser() == null){
            return;
        }

        fetchUserData();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ActionBar actionBar = ((MainActivity) getActivity()).getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchUserData();
    }

    private void fetchUserData(){
        FirebaseFirestore.getInstance().collection(USERS_COLLECTION).document(firebaseAuth.getCurrentUser().getUid()).get()
            .addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    loggedInUser = task.getResult().toObject(LoggedInUser.class);

                    assert loggedInUser != null;
                    if(loggedInUser.getSavingsRef() == null){
                        createSavingsAccount(loggedInUser);
                    }
                    else{
                        FirebaseFirestore.getInstance().collection(SAVINGS_COLLECTION).document(loggedInUser.getSavingsRef().getId()).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                            @Override
                            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                if(task.isSuccessful()){
                                    savingsAccount = task.getResult().toObject(Savings.class);
                                    onFetchedUser();
                                }
                            }
                        });
                    }
                }
            });
    }

    private void createSavingsAccount(LoggedInUser loggedInUser) {
        DocumentReference newSavingsAccountRef = FirebaseFirestore.getInstance().collection(SAVINGS_COLLECTION).document(loggedInUser.getUserId());

        Savings newSavingsAccount = new Savings();
        newSavingsAccountRef.set(newSavingsAccount).addOnCompleteListener(task -> {
            Map<String, Object> userData = new HashMap<>();
            userData.put("savingsRef", newSavingsAccountRef);

            FirebaseFirestore.getInstance().collection(USERS_COLLECTION).document(loggedInUser.getUserId())
                    .set(userData, SetOptions.merge())
                    .addOnCompleteListener(task2 -> {
                        savingsAccount = newSavingsAccount;
                        onFetchedUser();
                    });
        });
    }

    public void onFetchedUser(){
    }
}
