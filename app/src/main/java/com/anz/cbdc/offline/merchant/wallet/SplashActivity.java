package com.anz.cbdc.offline.merchant.wallet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    public static final String TAG = SplashActivity.class.getSimpleName();

    Context context = SplashActivity.this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash);

        new Handler().postDelayed(() -> {
            /* Create an Intent that will start the Menu-Activity. */
            Intent i = new Intent(context, NfcHome.class);
            startActivity(i);
            finish();
        }, 2000);
    }


}
