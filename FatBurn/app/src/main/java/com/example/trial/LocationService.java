package com.example.trial;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import com.example.trial.MainActivity;

import java.text.DecimalFormat;

public class LocationService extends Service {

    public static double prevlat = 0;
    public static double prevlong = 0;
    public static double Km = 0;
    public static double prevaltitude = 0;
    public static double total_kcal = 0;
    public static int counter = 0;
    int k = 1;



    private LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if (locationResult != null && locationResult.getLastLocation() != null) {
                double latitude = locationResult.getLastLocation().getLatitude();
                double longitude = locationResult.getLastLocation().getLongitude();
                double altitude = locationResult.getLastLocation().getAltitude();
                Log.d("LOCATION_UPDATE", latitude + ", " + longitude);

                if ((prevlat == 0 && prevlong == 0) || counter > 2){
                    prevlat = latitude;
                    prevlong = longitude;
                    prevaltitude = altitude;
                    counter = counter + 1;
                }else if (MainActivity.running == true){
                    Location locationprev = new Location("locationprev");
                    locationprev.setLongitude(prevlong);
                    locationprev.setLatitude(prevlat);

                    Location currentlocation = new Location("currentlocation");
                    currentlocation.setLatitude(latitude);
                    currentlocation.setLongitude(longitude);

                    double distance = locationprev.distanceTo(currentlocation) / 1000;
                    Km = Km + distance;

                    DecimalFormat df = new DecimalFormat("#.00");
                    String km_id = df.format(Km);
                    MainActivity.km_id.setText(km_id);

                    String speed_id = df.format(Km * 3600 / (3 * k));
                    if (distance * 1200 > 0.1){
                        MainActivity.speed_id.setText(speed_id);
                    }


                    double inclination = (altitude - prevaltitude) / (distance * 10);
                    String slope_id = df.format(inclination);
                    //if (inclination != 0)
                    MainActivity.slope_id.setText(slope_id);
                    //else
                    //    MainActivity.slope_id.setText("0.00");

                    double kcal_burned = distance * 1.036 * MainActivity.user_weight;
                    total_kcal = total_kcal + kcal_burned;

                    if (altitude < prevaltitude){
                        total_kcal = total_kcal - (0.066 * kcal_burned * inclination);
                    }else if (altitude > prevaltitude){
                        total_kcal = total_kcal + (0.12 * kcal_burned * inclination);
                    }

                    String kcal_id = df.format(total_kcal);
                    MainActivity.kcal_id.setText(kcal_id);

                    prevlat = latitude;
                    prevlong = longitude;
                    prevaltitude = altitude;
                    k = k + 1;
                }
            }
        }
    };

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private void startLocationService() {
        String channelId = "location_notification_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent resultIntent = new Intent();
        PendingIntent pendingIntent = PendingIntent.getActivity(getApplicationContext(), 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId);

        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setContentTitle("Location Service");
        builder.setDefaults(NotificationCompat.DEFAULT_ALL);
        builder.setContentText("Running");
        builder.setContentIntent(pendingIntent);
        builder.setAutoCancel(false);
        builder.setPriority(NotificationCompat.PRIORITY_MAX);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager != null && notificationManager.getNotificationChannel(channelId) == null) {
                NotificationChannel notificationChannel = new NotificationChannel(
                        channelId,
                        "Location Service",
                        NotificationManager.IMPORTANCE_HIGH
                );
                notificationChannel.setDescription("This channel is used by location service");
                notificationManager.createNotificationChannel(notificationChannel);
            }
        }

        LocationRequest locationRequest = new LocationRequest();
        locationRequest.setInterval(4000);
        locationRequest.setFastestInterval(2000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.getFusedLocationProviderClient(this)
                .requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        startForeground(Constants.LOCATION_SERVICE_ID, builder.build());

    }

    private void stopLocationService() {
        LocationServices.getFusedLocationProviderClient(this)
                .removeLocationUpdates(locationCallback);
        stopForeground(true);
        stopSelf();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null){
            String action = intent.getAction();
            if (action != null){
                if (action.equals(Constants.ACTION_START_LOCATION_SERVICE)){
                    startLocationService();
                }else if (action.equals(Constants.ACTION_STOP_LOCATION_SERVICE)){
                    stopLocationService();
                }
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }
}
