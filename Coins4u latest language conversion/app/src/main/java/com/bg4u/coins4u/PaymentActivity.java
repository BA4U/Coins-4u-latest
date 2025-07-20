package com.bg4u.coins4u;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.razorpay.Checkout;
import com.razorpay.ExternalWalletListener;
import com.razorpay.PaymentData;
import com.razorpay.PaymentResultWithDataListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class PaymentActivity extends Activity implements PaymentResultWithDataListener, ExternalWalletListener {

    private static final String RAZORPAY_KEY_ID = "rzp_live_uKNNoFARuXPBva";
    private static final String NAME = "Coins 4u";
    private static final double DEFAULT_AMOUNT = 9.00;
    private String transcId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Preload Razorpay checkout
        Checkout.preload(getApplicationContext());

        // Get the payment amount from the intent or use the default amount
        double amount = getIntent().getDoubleExtra("AMOUNT", DEFAULT_AMOUNT);
        String gameName = getIntent().getStringExtra("Game Name");
        String gameUID = getIntent().getStringExtra("Game UID");
        String tournamentId = getIntent().getStringExtra("TournamentId");

        // Generate a unique transaction ID
        transcId = generateTransactionId();

        // Initiate the Razorpay payment process with the provided amount
        initiateRazorpayPayment(amount, gameName, gameUID, tournamentId);
    }

    private void initiateRazorpayPayment(double amount, String gameName, String gameUID, String tournamentId) {
        try {
            JSONObject options = new JSONObject();
            options.put("name", NAME);
            options.put("description", "Payment for " + NAME);
            options.put("currency", "INR");
            options.put("amount", (int) (amount * 100)); // Convert amount to paise
            options.put("send_sms_hash", true);
            options.put("allow_rotation", true);
            options.put("prefill", generateDefaultPrefill());

            JSONObject notes = new JSONObject();
            notes.put("game_name", gameName);
            notes.put("game_uid", gameUID);
            notes.put("tournament_id", tournamentId);
            options.put("notes", notes);

            Checkout checkout = new Checkout();
            checkout.setKeyID(RAZORPAY_KEY_ID);
            checkout.setImage(R.drawable.coinlogowithtext_4u); // Replace with your app logo
            checkout.open(this, options);
        } catch (Exception e) {
            showToastMessage("Error initiating payment: " + e.getMessage());
            setResultAndFinish(false, null);
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID, PaymentData paymentData) {
        try {
            showToastMessage("Payment Successful");
            setResultAndFinish(true, paymentData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPaymentError(int code, String description, PaymentData paymentData) {
        try {
            showToastMessage("Payment Failed: " + description);
            setResultAndFinish(false, paymentData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onExternalWalletSelected(String walletName, PaymentData paymentData) {
        try {
            showToastMessage("External wallet selected: " + walletName);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String generateTransactionId() {
        Date c = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("ddMMyyyyHHmmss", Locale.getDefault());
        return df.format(c);
    }

    private JSONObject generateDefaultPrefill() {
        JSONObject preFill = new JSONObject();
        try {
            preFill.put("email", "bg4uanujofficial@gmail.com");
            preFill.put("contact", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return preFill;
    }

    private void setResultAndFinish(boolean paymentStatus, PaymentData paymentData) {
        Intent resultIntent = new Intent();
        resultIntent.putExtra("PAYMENT_STATUS", paymentStatus);
        if (paymentData != null) {
            resultIntent.putExtra("PAYMENT_ID", paymentData.getPaymentId());
            resultIntent.putExtra("PAYMENT_DATA", paymentData.getData().toString());
        }
        setResult(paymentStatus ? RESULT_OK : RESULT_CANCELED, resultIntent);
        finish();
    }

    private void showToastMessage(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
}