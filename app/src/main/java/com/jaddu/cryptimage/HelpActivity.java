package com.jaddu.cryptimage;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class HelpActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);
    }

    //Back key to enter MainActivity
    @Override
    public void onBackPressed() {
        Intent intent=new Intent(getBaseContext(), MainActivity.class);
        startActivity(intent);
    }
}
