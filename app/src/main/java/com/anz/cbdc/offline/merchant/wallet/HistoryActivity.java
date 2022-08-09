package com.anz.cbdc.offline.merchant.wallet;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

public class HistoryActivity extends Activity implements View.OnClickListener {

    public static final String TAG = HistoryActivity.class.getSimpleName();
    private ImageView ivHomeicon;
    private Animation mAnimation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);
    }

    private void initViews() {
        // rlMainActivity = findViewById(R.id.rlMainActivity);
        ivHomeicon = findViewById(R.id.ivHomeicon);
        mAnimation = AnimationUtils.loadAnimation(HistoryActivity.this, R.anim.swinging);
        // sharedResource();
    }

    @Override
    public void onClick(View view) {
        Intent intent;

    }

    @Override
    public void onResume() {
        super.onResume();
        // sharedResource();
        // ivHomeicon.startAnimation(mAnimation);
    }

    @Override
    public void onPause() {
        // ivHomeicon.clearAnimation();
        super.onPause();
    }
}
