package com.example.accidentpreventer;

import android.Manifest;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.lifecycle.ProcessLifecycleOwner;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.RequestBody;

public class MainActivity extends AppCompatActivity implements LocationListener, IBaseGpsListener, SensorEventListener, LifecycleObserver {
    protected LocationManager locationManager;
    protected LocationListener locationListener;
    protected MediaRecorder mRecorder;
    protected Context context;
    TextView txtLat;
    String lat;
    String provider;
    protected float latitude, longitude;
    protected boolean gps_enabled, network_enabled;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private TextView currentX, currentY, currentZ, accident;
    public Vibrator v;
    private float vibrateThreshold = 0;
    private long time;
    private Boolean isBackground;
    private double currentDecibel;
    private float a;

    public void initialize(){
        currentX = (TextView) findViewById(R.id.currentX);
        currentY = (TextView) findViewById(R.id.currentY);
        currentZ = (TextView) findViewById(R.id.currentZ);
        txtLat = (TextView) findViewById(R.id.textview1);
        accident = (TextView) findViewById(R.id.accident);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onSensorChanged(SensorEvent event) {

        // clean current values
        displayCleanValues();
        // display the current x,y,z accelerometer values
        displayCurrentValues(event);
        // display the max x,y,z accelerometer values
        getMicrophoneReading();
        if(detectAccident( event.values[0],event.values[1],event.values[2]) && getMicrophoneReading()){
            //v.vibrate(1050);
            //
//            buildNotifChannel();
//            try {
//                countDown();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
            accident.setText("ACCIDENT SUSPECTED! Your current soundLevels - "+currentDecibel+", current acceleration - "+a);
            createNotif();
            if(isBackground){
                Log.i("info", "onSensorChanged: HERE BACKGTOUND");
                ActivityManager activityManager = (ActivityManager) getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
                activityManager.moveTaskToFront(getTaskId(), ActivityManager.MOVE_TASK_NO_USER_ACTION);
            }
            createDialogBox();
            time=System.currentTimeMillis();
            Log.i("info", "onSensorChanged: LastTime - "+time);
            long currTime = System.currentTimeMillis();
            try {
                while (currTime - time <= 15000) {
                    currTime = System.currentTimeMillis();
                }
            }
            finally {
                Log.i("info", "onSensorChanged: in finally");
                try {
                    send_request();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void stop() {
        if (mRecorder != null) {
            mRecorder.stop();
            mRecorder.release();
            mRecorder = null;
        }
    }

    public double getAmplitude() {
        if (mRecorder != null) {
            Log.i("info", "getAmplitude: "+mRecorder.getMaxAmplitude());
            return (mRecorder.getMaxAmplitude() / 2700.0);
        }
        else
            return 0;

    }

    public Boolean getMicrophoneReading(){
        double SOUND_THRESHOLD = 140;
        double audio = getAmplitude();
         currentDecibel = 20 * Math.log10(audio );
        Log.i("info", "getMicrophoneReading: "+currentDecibel+" audio:"+audio);
        if(currentDecibel > SOUND_THRESHOLD)
            return true;
        else
            return false;

    }

    public void checkMicrophonePermission(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO},
                    1);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
    public void send_request() throws JSONException {
        MediaType mediaType;
        JSONObject postBodyString = new JSONObject();
        postBodyString.put("Latitude", latitude);
        postBodyString.put("Longitude", longitude);
        mediaType = MediaType.parse("application/json");
        RequestBody requestBody = RequestBody.create(String.valueOf(postBodyString), mediaType);
        HospitalService h= new HospitalService(this);
        h.postRequest("http:////192.168.1.120:5000/get_hospital", requestBody);
    }

    public boolean detectAccident(float x, float y, float z){
        a = (float) Math.sqrt(x*x+y*y+z*z);
        float g = 9.80665f;
        float threshold = 4*g;
        float G = a/g;
        Log.i("info", "detectAccident: Acceleration = "+a+", G = "+String.valueOf(G)+", Threshold = "+String.valueOf(threshold));
        if(G>threshold){
            return true;
        }
        return false;
    }


    public void displayCleanValues() {
        currentX.setText("0.0");
        currentY.setText("0.0");
        currentZ.setText("0.0");
    }

    // display the current x,y,z accelerometer values
    public void displayCurrentValues(SensorEvent event) {
        currentX.setText(Float.toString(event.values[0]));
        currentY.setText(Float.toString(event.values[1]));
        currentZ.setText(Float.toString(event.values[2]));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ProcessLifecycleOwner.get().getLifecycle().addObserver(this);
        initialize();
        //buildNotifChannel();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        checkMicrophonePermission();
        mRecorder = new MediaRecorder();
        mRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
        mRecorder.setOutputFile("/dev/null");
        try {
            mRecorder.prepare();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        mRecorder.start();

        if (sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            // success! we have an accelerometer

            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            vibrateThreshold = accelerometer.getMaximumRange() / 2;
        } else {
            // fai! we dont have an accelerometer!
        }

        //initialize vibration
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);

        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
        if(checkLocationPermission()){
            Log.i("info", "onCreate: Permissions granted");
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onAppBackgrounded() {
        isBackground=true;
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onAppForegrounded() {
        isBackground=false;
    }
    @Override
    public void onLocationChanged(@NonNull Location location) {
        txtLat = (TextView) findViewById(R.id.textview1);
        txtLat.setText("Latitude:" + location.getLatitude() + ", Longitude:" + location.getLongitude());
        latitude= (float) location.getLatitude();
        longitude= (float) location.getLongitude();
        Log.i("info", "onLocationChanged: Latitude: "+location.getLatitude()+", Longitude:"+location.getLongitude());
//        if(location!= null){
//            CLocation myLocation = new CLocation(location, this.useMetricUnits());
//            this.updateSpeed(myLocation);
//            CheckBox chkUseMetricUntis = (CheckBox) this.findViewById(R.id.chkMetricUnits);
//            chkUseMetricUntis.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//                @Override
//                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//                    // TODO Auto-generated method stub
//                    MainActivity.this.updateSpeed(null);
//                }
//            });
//        }
    }
    @Override
    public void onProviderDisabled(String provider) {
        Log.i("info","Latitude disable");
        buildAlertMessageNoGps();
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.i("info","Latitude enable");
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Log.i("info","Latitude status");
    }

    @Override
    public void onGpsStatusChanged(int event) {
        Log.i("info", "onGpsStatusChanged: ***************");
    }


    public void openPanicActivity(View view) {
        Intent intent = new Intent(this, PanicButtonPressed.class);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        startActivity(intent);
    }

    public void countDown() throws InterruptedException {
        for(int i=0;i<10;i++){
            v.vibrate(1000);
            TimeUnit.SECONDS.sleep(1);
        }
    }

    public boolean checkLocationPermission()
    {
        String permission = "android.permission.ACCESS_FINE_LOCATION";
        int res = this.checkCallingOrSelfPermission(permission);
        final LocationManager manager = (LocationManager) getSystemService( Context.LOCATION_SERVICE );

        if ( !manager.isProviderEnabled( LocationManager.GPS_PROVIDER ) ) {
            buildAlertMessageNoGps();
        }
        return (res == PackageManager.PERMISSION_GRANTED);
    }
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    public void createDialogBox() {
        Intent intent=new Intent(this,Alert.class);
//        intent.putExtra(Tags.MESSAGE,msg);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void updateSpeed(CLocation location) {
        // TODO Auto-generated method stub
        float nCurrentSpeed = 0;

        if(location != null)
        {
            location.setUseMetricunits(this.useMetricUnits());
            nCurrentSpeed = location.getSpeed();
        }

        Formatter fmt = new Formatter(new StringBuilder());
        fmt.format(Locale.US, "%5.1f", nCurrentSpeed);
        String strCurrentSpeed = fmt.toString();
        strCurrentSpeed = strCurrentSpeed.replace(' ', '0');

        String strUnits = "miles/hour";
        if(this.useMetricUnits())
        {
            strUnits = "meters/second";
        }

//        TextView txtCurrentSpeed = (TextView) this.findViewById(R.id.txtCurrentSpeed);
        Log.i("info", "updateSpeed: Speed - "+strCurrentSpeed+" "+strUnits);
//        txtCurrentSpeed.setText(strCurrentSpeed + " " + strUnits);
    }

    private boolean useMetricUnits() {
        // TODO Auto-generated method stub
//        CheckBox chkUseMetricUnits = (CheckBox) this.findViewById(R.id.chkMetricUnits);
//        return chkUseMetricUnits.isChecked();
        return true;
    }

    public void finish()
    {
        super.finish();
        System.exit(0);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    public void buildNotifChannel(){
        String NOTIFICATION_CHANNEL_ID = "com.example.preventaccident";
        String channelName = "My Background Service";
        NotificationChannel chan = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, NotificationManager.IMPORTANCE_NONE);
        chan.setLightColor(Color.BLUE);
        chan.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        assert manager != null;
        manager.createNotificationChannel(chan);
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    public void createNotif(){
        Log.i("info", "createNotif: here");
//        buildNotifChannel();
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, "com.example.simpleapp");
        Intent intent = new Intent(this, Alert.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("Latitude", latitude);
        intent.putExtra("Longitude", longitude);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Uri soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://"+ getApplicationContext().getPackageName() + "/" + R.raw.audio);
        Notification notification = notificationBuilder.setOngoing(true)
                .setSmallIcon(R.drawable.ic_launcher_background)
                .setContentTitle("Accident occured")
                .setPriority(NotificationManager.IMPORTANCE_HIGH)
                .setCategory(Notification.CATEGORY_CALL)
                .setDefaults(Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 1000})
                .setSound(soundUri)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(0, notification);
    }

}