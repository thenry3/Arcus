package com.example.arcus;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class feed extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feed);

    }

    public void onClick(View v) {
        Intent intent = new Intent(this, ChoosePhoto.class);
        startActivity(intent);
    }
}
