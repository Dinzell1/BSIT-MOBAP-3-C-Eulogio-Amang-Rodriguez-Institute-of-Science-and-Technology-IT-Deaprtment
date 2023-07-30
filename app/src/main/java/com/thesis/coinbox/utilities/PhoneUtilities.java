package com.thesis.coinbox.utilities;

public class PhoneUtilities {
    public static String formatMobileNumber(String phoneNumber) {
        // Remove leading zero if present
        if (phoneNumber.startsWith("0")) {
            phoneNumber = phoneNumber.substring(1);
        }

        // Remove non-digit characters
        phoneNumber = phoneNumber.replaceAll("\\D+", "");

        // Remove country code if present
        String countryCode = "63"; // Replace with your country code
        if (phoneNumber.startsWith(countryCode)) {
            phoneNumber = phoneNumber.substring(countryCode.length());
        }

        // Limit to 10 digits
        if (phoneNumber.length() > 10) {
            phoneNumber = phoneNumber.substring(0, 10);
        }

        return phoneNumber;
    }

}
