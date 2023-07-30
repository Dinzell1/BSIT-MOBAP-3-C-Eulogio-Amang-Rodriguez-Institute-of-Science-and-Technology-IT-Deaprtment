package com.thesis.coinbox.ui.transactions;

import static com.thesis.coinbox.utilities.Constants.TRANSACTIONS_COLLECTION;
import static com.thesis.coinbox.utilities.Constants.USERS_COLLECTION;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.tabs.TabLayout;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.thesis.coinbox.R;
import com.thesis.coinbox.RequireLoginFragment;
import com.thesis.coinbox.adapters.TransactionsAdapter;
import com.thesis.coinbox.placeholder.data.model.Transaction;
import com.thesis.coinbox.databinding.FragmentTransactionsBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFRun;


public class TransactionsFragment extends RequireLoginFragment {
    private TransactionsAdapter adapter;

    private final List<Transaction> transactions = new ArrayList<>();
    private String type = "money";
    private FragmentTransactionsBinding binding;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentTransactionsBinding.inflate(inflater, container, false);
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));
        adapter = new TransactionsAdapter();

        binding.recyclerView.setAdapter(adapter);

        return binding.getRoot();
    }

    private void refreshList() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        CollectionReference transactionsRef = db.collection(TRANSACTIONS_COLLECTION);

        Query query = transactionsRef.where(Filter.or(
                        Filter.equalTo("sender", db.collection(USERS_COLLECTION).document(loggedInUser.getUserId())),
                        Filter.equalTo("receiver", db.collection(USERS_COLLECTION).document(loggedInUser.getUserId()))
                ))
                .whereEqualTo("type", type)
                .orderBy("date", Query.Direction.DESCENDING);

        adapter.notifyItemRangeRemoved(0, transactions.size());
        transactions.clear();
        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    for (DocumentSnapshot document : task.getResult()) {
                        Transaction transaction = document.toObject(Transaction.class);
                        transaction.setId(document.getId());
                        if (transactions.contains(transaction))
                            continue;
                        transactions.add(transaction);
                        adapter.notifyItemInserted(transactions.size() - 1);
                    }
                } else {
                    String message = task.getException().getMessage();
                    Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public void onFetchedUser() {
        adapter.setTransactions(transactions, loggedInUser, requireContext());
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {

                type = tab.getText().toString().toLowerCase(Locale.getDefault());
                refreshList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // Called when a tab is unselected
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // Called when a tab is reselected (tab was already selected)
            }
        });
        refreshList();
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                String[] months = getResources().getStringArray(R.array.spinnerItems);
                String filter = months[i];
                if (filter.equals("All")) {
                    refreshList();
                    return;
                }
                int targetYear = Calendar.getInstance().get(Calendar.YEAR); // Replace with the desired year

                // Calculate the first and last day of the target month
                int targetMonth = i;
                LocalDate startDate = LocalDate.of(targetYear, targetMonth, 1);
                Date startDateTime = Date.from(startDate.atStartOfDay(ZoneId.systemDefault()).toInstant());

                int lastDayOfMonth = startDate.getMonth().length(targetYear % 4 == 0);
                LocalDate endDate = LocalDate.of(targetYear, targetMonth, lastDayOfMonth);
                Date endDateTime = Date.from(endDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
                FirebaseFirestore db = FirebaseFirestore.getInstance();
                Query collection = db.collection(TRANSACTIONS_COLLECTION);
                Query query = collection.whereGreaterThanOrEqualTo("date", startDateTime)
                        .whereLessThanOrEqualTo("date", endDateTime)
                        .where(Filter.or(
                                Filter.equalTo("sender", db.collection(USERS_COLLECTION).document(loggedInUser.getUserId())),
                                Filter.equalTo("receiver", db.collection(USERS_COLLECTION).document(loggedInUser.getUserId()))
                        ))
                        .whereEqualTo("type", type)
                        .orderBy("date", Query.Direction.DESCENDING);

                adapter.notifyItemRangeRemoved(0, transactions.size());
                transactions.clear();
                query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {


                            for (DocumentSnapshot document : task.getResult()) {
                                Transaction transaction = document.toObject(Transaction.class);
                                transaction.setId(document.getId());
                                if (transactions.contains(transaction))
                                    continue;
                                transactions.add(transaction);
                                adapter.notifyItemInserted(transactions.size() - 1);
                            }
                        } else {
                            String message = task.getException().getMessage();
                            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });



        binding.saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Call the method to create the DOCX file
                createDocxFile(transactions);
            }
        });

    }


    // iterate through the list of transactions and create a DOCX file
    private void createDocxFile(List<Transaction> transactions) {
        try {
            XWPFDocument doc = new XWPFDocument();
            XWPFParagraph paragraph = doc.createParagraph();

            XWPFRun run = paragraph.createRun();
            run.setText("Transaction List");
            run.setBold(true);
            run.setFontSize(14);

            for (Transaction transaction : transactions) {
                String date = formatDate(transaction.getDate());
                String sender = transaction.getSender().getId();
                String receiver = transaction.getReceiver().getId();
                String money = String.valueOf(transaction.getAmount());

                // Add transaction details to the document
                XWPFParagraph transactionParagraph = doc.createParagraph();
                XWPFRun transactionRun = transactionParagraph.createRun();
                transactionRun.setText("Date: " + date);
                transactionRun.addBreak();
                transactionRun.setText("Sender: " + sender);
                transactionRun.addBreak();
                transactionRun.setText("Receiver: " + receiver);
                transactionRun.addBreak();
                transactionRun.setText("Money: " + money);
                transactionRun.addBreak();
            }

            // Save the document to a file
            File appFolder = getApplicationContext().getFilesDir();
            File outputFile = new File(appFolder, "TransactionList.docx");
            FileOutputStream out = new FileOutputStream(outputFile);
            doc.write(out);
            out.close();

            // Show a toast message to indicate successful file creation
            Toast.makeText(requireContext(), "Transaction list downloaded as DOCX", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), "Error creating DOCX file", Toast.LENGTH_SHORT).show();
        }
    }

    private Context getApplicationContext() {
        return requireContext();
    }


//    private void createDocxFile(List<Transaction> transactions) {
//        try {
//            XWPFDocument doc = new XWPFDocument();
//            XWPFParagraph paragraph = doc.createParagraph();
//
//            XWPFRun run = paragraph.createRun();
//            run.setText("Transaction List");
//            run.setBold(true);
//            run.setFontSize(14);
//
//            for (Transaction transaction : transactions) {
//                String date = formatDate(transaction.getDate());
//                String sender = transaction.getSender().getId();
//                String receiver = transaction.getReceiver().getId();
//                String money = String.valueOf(transaction.getAmount());
//
//                // Add transaction details to the document
//                XWPFParagraph transactionParagraph = doc.createParagraph();
//                XWPFRun transactionRun = transactionParagraph.createRun();
//                transactionRun.setText("Date: " + date);
//                transactionRun.addBreak();
//                transactionRun.setText("Sender: " + sender);
//                transactionRun.addBreak();
//                transactionRun.setText("Receiver: " + receiver);
//                transactionRun.addBreak();
//                transactionRun.setText("Money: " + money);
//                transactionRun.addBreak();
//            }
//
//            // Save the document to a file
//            File outputFile = new File(Environment.getExternalStorageDirectory(), "TransactionList.docx");
//            FileOutputStream out = new FileOutputStream(outputFile);
//            doc.write(out);
//            out.close();
//
//            // Show a toast message to indicate successful file creation
//            Toast.makeText(requireContext(), "Transaction list downloaded as DOCX", Toast.LENGTH_SHORT).show();
//        } catch (Exception e) {
//            e.printStackTrace();
//            Toast.makeText(requireContext(), "Error creating DOCX file", Toast.LENGTH_SHORT).show();
//        }
//    }

    private String formatDate(Date date) {
        return date.toString();
    }
}
