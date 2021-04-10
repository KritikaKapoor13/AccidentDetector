package com.example.accidentpreventer;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

public class Alert extends AppCompatActivity {
    private float latitude, longitude;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_alert);
        latitude=getIntent().getFloatExtra("Latitude",0.0f);
        longitude=getIntent().getFloatExtra("Longitude",0.0f);
        createDialogBox();
    }
    public void createDialogBox() {
        AlertDialog.Builder alertDialog2 = new AlertDialog.Builder(
                this);

// Setting Dialog Title
        alertDialog2.setTitle("Accident Predicted!! Waiting for confirmation from user");

// Setting Dialog Message
        alertDialog2.setMessage("Please respond within 15 seconds or the appropriate services will be automatically alerted.");
        Intent intent1 = new Intent(this, PanicButtonPressed.class);
        Intent intent2 = new Intent(this, MainActivity.class);
        alertDialog2.setPositiveButton("YES",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        intent1.putExtra("Latitude", latitude);
                        intent1.putExtra("Longitude", longitude);
                        startActivity(intent1);
                    }
                });
        alertDialog2.setNegativeButton("NO",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Write your code here to execute after dialog
                        dialog.cancel();
                        startActivity(intent2);
                    }
                });
        alertDialog2.show();
    }
}