package com.thesis.coinbox.placeholder.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.DocumentReference;

import java.util.List;

/**
 * Data class that captures user information for logged in users retrieved from LoginRepository
 */
public class LoggedInUser implements Parcelable {
    public LoggedInUser() {
    }

    private String userId;
    private String displayName;
    private String phone;
    private DocumentReference savingsRef;

    public LoggedInUser(String userId, String displayName, String phone) {
        this.userId = userId;
        this.displayName = displayName;
        this.phone = phone;
    }

    protected LoggedInUser(Parcel in) {
        userId = in.readString();
        displayName = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(displayName);
        dest.writeString(phone);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LoggedInUser> CREATOR = new Creator<LoggedInUser>() {
        @Override
        public LoggedInUser createFromParcel(Parcel in) {
            return new LoggedInUser(in);
        }

        @Override
        public LoggedInUser[] newArray(int size) {
            return new LoggedInUser[size];
        }
    };

    public void setSavingsRef(DocumentReference savingsRef) {
        this.savingsRef = savingsRef;
    }


    public String getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public DocumentReference getSavingsRef() {
        return savingsRef;
    }

    public String getPhone() {
        return phone;
    }

    public List<Transaction> getTransactions() {
        return null;
    }
}