/*
 * Copyright 2014 Lothar Dierkes
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package is.gitana.openspeedometer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import android.app.AlertDialog;
import android.content.DialogInterface;


public class OpenSpeedometer extends Activity implements LocationListener,
        View.OnClickListener {
    private LocationManager locationManager;

    ///Application state
    private enum AppState {
        STOPPED, STARTED
    }

    private AppState state;
    private AppState pausedState; //state in onPause

    ///Bundle constants
    private final String KEY_STATE = "state";

    /// values...
    private float speed = 0;
    private long minDistance = 0;
    private long minTime = 0;

    /// Widgets
    private Button startStop;
    private TextView displayMs;
    private TextView displayKmh;


    /// Activity lifecycle
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_open_speedometer);

        if (savedInstanceState != null) {
            pausedState = AppState.valueOf(savedInstanceState.getString(KEY_STATE));
        } else {
            pausedState = AppState.STOPPED;
        }

        startStop = (Button) findViewById(R.id.startstop);
        startStop.setOnClickListener(this);

        displayMs = (TextView) findViewById(R.id.ms);
        displayKmh = (TextView) findViewById(R.id.kmh);

        locationManager =
                (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            setState(AppState.STOPPED);
        } else {
            setState(pausedState);
        }
    }

    @Override
    public void onPause() {
        pausedState = state;
        setState(AppState.STOPPED);
        super.onPause();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(KEY_STATE, pausedState.name());
        super.onSaveInstanceState(outState);
    }

    /// Utility
    private void setState(AppState newState) {
        if (newState == AppState.STOPPED) {
            locationManager.removeUpdates(this);
            startStop.setText(getString(R.string.button_start));
            displayKmh.setText(R.string.no_kmh);
            displayMs.setText(R.string.no_ms);
            state = newState;
        }
        else if (newState == AppState.STARTED) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime,
                    minDistance, this);
            startStop.setText(getString(R.string.button_stop));
            state = newState;
        }
    }

    private void requestEnableGps() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.enable_gps)
                .setCancelable(false)
                .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivity(intent);
                    }
                })
                .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        AlertDialog alert = builder.create();
        alert.show();
    }

    /// OnclickListener implementation
    public void onClick(View view) {
        if (state == AppState.STOPPED) {
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                setState(AppState.STARTED);
            } else {
                requestEnableGps();
            }
        }
        else if (state == AppState.STARTED) {
            setState(AppState.STOPPED);
        }
    }

    /// LocationListener implementation
    public void onLocationChanged(Location location) {
        Log.d("OpenSpeedometer", "Location changed.");
        if (location.hasSpeed()) {
            speed = location.getSpeed();
            displayMs.setText(String.format("%.2f m/s", speed));
            int kmh = (int) (speed * 3.6 + 0.5);
            displayKmh.setText(String.format("%d km/h", kmh));
        } else {
            Log.d("OpenSpeedometer", "Location does not have speed.");
        }
    }

    public void onProviderEnabled(String provider) {
    }

    public void onProviderDisabled(String provider) {
        if( provider.equals(LocationManager.GPS_PROVIDER)){
            Toast.makeText(this, "GPS disabled", Toast.LENGTH_LONG);
            setState(AppState.STOPPED);
        }
    }

    public void onStatusChanged(String provider, int status, Bundle extras) {
    }
}
