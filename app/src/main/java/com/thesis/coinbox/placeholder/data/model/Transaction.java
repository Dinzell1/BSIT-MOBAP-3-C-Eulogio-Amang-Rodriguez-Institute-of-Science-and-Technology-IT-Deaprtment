package com.thesis.coinbox.placeholder.data.model;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentReference;

import java.util.Date;

public class Transaction {
    private String id;
    private Date date;
    private DocumentReference sender;
    private DocumentReference receiver;
    private float amount;
    private String type;

    public Transaction() {
        // Default constructor required for Firebase Realtime Database
    }

    public Transaction(String id, Date date, DocumentReference sender, DocumentReference receiver, float amount, String type) {
        this.id = id;
        this.date = date;
        this.sender = sender;
        this.receiver = receiver;
        this.amount = amount;
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public DocumentReference getSender() {
        return sender;
    }

    public void setSender(DocumentReference sender) {
        this.sender = sender;
    }

    public DocumentReference getReceiver() {
        return receiver;
    }

    public void setReceiver(DocumentReference receiver) {
        this.receiver = receiver;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        Transaction other = (Transaction)obj;
        return other.getId().equals(getId());
    }
}
