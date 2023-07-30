package com.thesis.coinbox.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.thesis.coinbox.R;
import com.thesis.coinbox.placeholder.data.model.LoggedInUser;
import com.thesis.coinbox.placeholder.data.model.Transaction;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionsAdapter extends RecyclerView.Adapter<TransactionsAdapter.TransactionViewHolder> {
    private List<Transaction> transactions;
    private Context context;
    private LoggedInUser user;
    private TransactionsAdapter adapter;

    public void setTransactions(List<Transaction> transactions, LoggedInUser user, Context context) {
        this.transactions = transactions;
        this.context = context;
        this.user = user;
    }

    @NonNull
    @Override
    public TransactionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_transaction, parent, false);
        return new TransactionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TransactionViewHolder holder, int position) {
        Transaction transaction = transactions.get(position);
        holder.bind(transaction, user, context);
    }

    @Override
    public int getItemCount() {
        return transactions != null ? transactions.size() : 0;
    }

    static class TransactionViewHolder extends RecyclerView.ViewHolder {
        private final TextView dateTextView;
        private final TextView senderTextView;
        private final TextView receiverTextView;
        private final TextView amountTextView;

        private final SimpleDateFormat dateTimeFormat = new SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale.getDefault());
        private FirebaseFirestore db;

        TransactionViewHolder(@NonNull View itemView) {
            super(itemView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            senderTextView = itemView.findViewById(R.id.senderTextView);
            receiverTextView = itemView.findViewById(R.id.receiverTextView);
            amountTextView = itemView.findViewById(R.id.amountTextView);
            db = FirebaseFirestore.getInstance();
        }

        void bind(Transaction transaction, LoggedInUser user, Context context) {
            if(user == null)
                return;
            dateTextView.setText(dateTimeFormat.format(transaction.getDate()));
            transaction.getSender().get().addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    LoggedInUser sender = task.getResult().toObject(LoggedInUser.class);
                    senderTextView.setText(String.format(Locale.getDefault(),"%s: %s", context.getString(R.string.sender_label), sender.getDisplayName()));
                }
                else{
                    senderTextView.setText("Unknown");
                }
            });

            transaction.getReceiver().get().addOnCompleteListener(task -> {
                if(task.isSuccessful()){
                    LoggedInUser receiver = task.getResult().toObject(LoggedInUser.class);
                    receiverTextView.setText(String.format(Locale.getDefault(),"%s: %s", context.getString(R.string.receiver_label), receiver.getDisplayName()));
                }
                else{
                    senderTextView.setText("Unknown");
                }
            });

            String amount = String.format(Locale.getDefault(), "%s%s", "+", transaction.getAmount());
            if(transaction.getSender().getId().equals(user.getUserId()))
                amount = String.format(Locale.getDefault(), "%s%s", "-", transaction.getAmount());
            amountTextView.setText(amount);
        }
    }
}

