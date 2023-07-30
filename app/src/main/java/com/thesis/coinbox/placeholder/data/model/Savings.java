package com.thesis.coinbox.placeholder.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.NonNull;

public class Savings implements Parcelable {
    float balance = 1000f;

    public Savings() {
    }

    protected Savings(Parcel in) {
        balance = in.readFloat();
    }

    public static final Creator<Savings> CREATOR = new Creator<Savings>() {
        @Override
        public Savings createFromParcel(Parcel in) {
            return new Savings(in);
        }

        @Override
        public Savings[] newArray(int size) {
            return new Savings[size];
        }
    };

    public float getBalance() {
        return balance;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeFloat(balance);
    }
}
