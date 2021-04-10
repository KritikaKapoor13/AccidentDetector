package com.example.accidentpreventer;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class HospitalService extends AppCompatActivity {

    private float latitude, longitude;
    private String service;

    private String url = "http:////192.168.1.120:5000/get_services";
    private String postBodyString;
    private MediaType mediaType;
    private RequestBody requestBody;
    private String phone;
    private String message;
    String message_services = "Repair services needed at ";
    String message_hospital = "Emergency services needed at ";
    private static final int PERMISSION_SEND_SMS = 123;
    private Context context;

    public HospitalService(){
        this.context = this;
    }
    public HospitalService(Context context){
        this.context=context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hospital_service);
        latitude=getIntent().getFloatExtra("Latitude",0.0f);
        longitude=getIntent().getFloatExtra("Longitude",0.0f);
        service=getIntent().getStringExtra("Type");
        try {
            RequestBody requestBody = buildRequestBody();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(service.equals("com.example.accidentpreventer:id/services")){
            try {
                postRequest("http:////192.168.1.120:5000/get_services", requestBody);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        else{
            try {
                postRequest("http:////192.168.1.120:5000/get_hospital", requestBody);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        Log.i("info", "onCreate: in hospital service, location -  "+ String.valueOf(latitude)+","+String.valueOf(longitude)+", service requested - "+service);
    }
    private RequestBody buildRequestBody() throws JSONException {
        JSONObject postBodyString = new JSONObject();
        postBodyString.put("Latitude", latitude);
        postBodyString.put("Longitude", longitude);
        mediaType = MediaType.parse("application/json");
        requestBody = RequestBody.create(String.valueOf(postBodyString), mediaType);
        return requestBody;
    }


    public void postRequest(String URL, RequestBody requestBody) throws JSONException {
        //RequestBody requestBody = buildRequestBody();
        OkHttpClient okHttpClient = new OkHttpClient();
        Request request = new Request
                .Builder()
                .post(requestBody)
                .url(URL)
                .build();

        okHttpClient.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(final Call call, final IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.i("info", "run: FAILED "+ e.toString());
                        call.cancel();
                    }
                });

            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String result=response.body().string();
                            JSONObject Jobject = new JSONObject(result);
                            JSONObject service = Jobject.getJSONObject("Contact");
                            phone = service.getString("Phone");
                            String type = service.getString("Type");
                            if(type.equals("Service")) {
                                message = message_services + latitude + ", " + longitude;
                            } else {
                                message = message_hospital + latitude+", "+longitude;
                            }
                            Log.i("info", "run: message - "+phone+","+type+","+message);
                            requestSmsPermission(phone, message);
                        } catch (IOException | JSONException e) {
                            Log.i("info", "run: "+ e.toString());
                            e.printStackTrace();
                        }
                        Log.i("info", "run: SUCCESS - "+response.body());
                    }
                });


            }
        });
    }
    public void sendSMS(String phoneNo, String msg) {
        Log.i("info", "sendSMS: here with "+phoneNo+msg);
        try {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(phoneNo, null, msg, null, null);
            Toast.makeText(getApplicationContext(), "Message Sent",
                    Toast.LENGTH_LONG).show();
        } catch (Exception ex) {
            Log.i("info", "sendSMS: "+ex.getMessage());
//            Toast.makeText(getApplicationContext(),ex.getMessage().toString(),
//                    Toast.LENGTH_LONG).show();
            ex.printStackTrace();
        }
    }

    private void requestSmsPermission(String phone, String message) {
        Log.i("info", "requestSmsPermission: here with"+ phone+message);
        // check permission is given
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            // request permission (see result in onRequestPermissionsResult() method)
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{Manifest.permission.SEND_SMS},
                    PERMISSION_SEND_SMS);
        } else {
            // permission already granted run sms send
            sendSMS(phone, message);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_SEND_SMS: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    sendSMS(phone, message);
                } else {
                    // permission denied
                }
                return;
            }
        }
    }

}
