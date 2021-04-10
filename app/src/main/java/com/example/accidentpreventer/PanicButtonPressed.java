package com.example.accidentpreventer;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PanicButtonPressed extends AppCompatActivity {

    private TextView mTextView;
    private float latitude, longitude;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_panic_button_pressed2);
        latitude=getIntent().getFloatExtra("Latitude",0.0f);
        longitude=getIntent().getFloatExtra("Longitude",0.0f);
    }
    public void openActivity(View view) {
        Intent intent = new Intent(this, HospitalService.class);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        intent.putExtra("Type", this.getResources().getResourceName(view.getId()));
        startActivity(intent);
    }
}