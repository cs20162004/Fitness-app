package com.example.trial;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.ActivityChooserView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;

import com.example.trial.LocationService;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_LOCATION_PERMISSION = 1;
    public static boolean running;
    private long pauseoffset;

    private Chronometer chronometer;

    public static TextView km_id;
    public static TextView slope_id;
    public static TextView kcal_id;
    public static TextView speed_id;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        km_id = findViewById(R.id.km_id);
        slope_id = findViewById(R.id.slope_id);
        kcal_id = findViewById(R.id.kcal_id);
        speed_id = findViewById(R.id.speed_id);

        chronometer = findViewById(R.id.timer_id);
        chronometer.setFormat("%s");
        chronometer.setBase(SystemClock.elapsedRealtime());

        findViewById(R.id.buttonstart).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_DENIED) {
                    ActivityCompat.requestPermissions(
                            MainActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            REQUEST_CODE_LOCATION_PERMISSION
                    );
                }else {
                    startLocationService();
                }

                if (!running) {
                    chronometer.setBase(SystemClock.elapsedRealtime() - pauseoffset);
                    chronometer.start();
                    running = true;
                }
            }
        });

        findViewById(R.id.buttonstop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopLocationService();

                if (running){
                    chronometer.stop();
                    pauseoffset = SystemClock.elapsedRealtime() - chronometer.getBase();
                    running = false;
                }
            }
        });

        findViewById(R.id.buttonreset).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chronometer.setBase(SystemClock.elapsedRealtime());
                pauseoffset = 0;

                kcal_id.setText("0.0");
                speed_id.setText("0.0");
                slope_id.setText("0.00");
                km_id.setText("0.00");

                LocationService.Km = 0;
                LocationService.total_kcal = 0;
                LocationService.prevlat = 0;
                LocationService.prevlong = 0;
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_LOCATION_PERMISSION && grantResults.length > 0){
            if (grantResults[0] != PackageManager.PERMISSION_DENIED){
                startLocationService();
            }else{
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isLocationServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager != null){
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)){
                if (LocationService.class.getName().equals(service.service.getClassName())) {
                    if (service.foreground){
                        return true;
                    }
                }
            }
            return false;
        }
        return false;
    }

    private void startLocationService() {
        if (!isLocationServiceRunning()){
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_START_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this, "Location service started", Toast.LENGTH_SHORT).show();
        }
    }

    private void stopLocationService() {
        if (isLocationServiceRunning()) {
            Intent intent = new Intent(getApplicationContext(), LocationService.class);
            intent.setAction(Constants.ACTION_STOP_LOCATION_SERVICE);
            startService(intent);
            Toast.makeText(this, "Location service stopped", Toast.LENGTH_SHORT).show();
        }
    }

}