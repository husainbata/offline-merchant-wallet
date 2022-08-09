package com.anz.cbdc.offline.merchant.wallet;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.google.gson.Gson;

import java.math.BigDecimal;

public class NfcHome extends Activity implements View.OnClickListener {

    public static final String TAG = NfcHome.class.getSimpleName();
    private RelativeLayout rlMainActivity;
    private RelativeLayout rlHistoryActivity;
    private ImageView ivHomeicon;
    private Animation mAnimation;
    private static final String SGD = "SGD ";
    private static final String SHARED_PREFERENCES_FILE_NAME = "merchantWalletSP";
    private static final String MERCHANT_WALLET_BALANCE_SP = "merchantWalletBalance";
    private static final String PAYMENT_CODE_SP = "paymentCode";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfc_home);
        initViews();
        rlMainActivity.setOnClickListener(this);
        rlHistoryActivity.setOnClickListener(this);
    }

    private void initViews() {
        rlMainActivity = findViewById(R.id.rlMainActivity);
        rlHistoryActivity = findViewById(R.id.rlHistoryActivity);
        ivHomeicon = findViewById(R.id.ivHomeicon);
        mAnimation = AnimationUtils.loadAnimation(NfcHome.this, R.anim.swinging);
        // hardReset();
        // clearPaymentCode();
        getPaymentCode();
        //sharedPreferences();
    }

    private void sharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, MODE_PRIVATE);
        String offlineWalletBalance = sharedPreferences.getString(MERCHANT_WALLET_BALANCE_SP, "0.00");
        BigDecimal bd = new BigDecimal(offlineWalletBalance).setScale(2, BigDecimal.ROUND_HALF_UP);
        //((TextView)NfcHome.this.findViewById(R.id.tvMobileWalletBalance)).setText(SGD + String.format("%,.2f", bd));
    }

    private void hardReset() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(MERCHANT_WALLET_BALANCE_SP, "1285.55");
        editor.commit();
    }

    private void getPaymentCode() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, MODE_PRIVATE);
        Gson gson = new Gson();
        Data allPayments = gson.fromJson(sharedPreferences.getString(PAYMENT_CODE_SP, ""), Data.class);
        Log.i("availablePaymentCodes", "PAYMENT_CODE_SP>> " + (allPayments != null ? allPayments.getMap().values() : ""));
    }

    private void clearPaymentCode() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREFERENCES_FILE_NAME, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(PAYMENT_CODE_SP);
        editor.commit();
    }

    @Override
    public void onClick(View view) {
        Intent intent;

        switch (view.getId()) {
            case R.id.rlMainActivity:
                intent = new Intent(this, OfflineCardTransfersActivity.class);
                this.startActivity(intent);
                break;
            case R.id.rlHistoryActivity:
                intent = new Intent(this, HistoryActivity.class);
                this.startActivity(intent);
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        sharedPreferences();
        ivHomeicon.startAnimation(mAnimation);
    }

    @Override
    public void onPause() {
        ivHomeicon.clearAnimation();
        super.onPause();
    }
}
