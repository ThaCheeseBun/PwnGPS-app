package nu.cheese.pwngps;

import android.annotation.SuppressLint;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.NonNull;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

public class LocationHelper {
    private static final long updateThreshold = 30L * 1000000000;
    private final Executor executor;
    private final LocationManager manager;

    private int usedSatellites = 0;
    public int getUsedSatellites() {
        return usedSatellites;
    }

    @SuppressLint("MissingPermission")
    public LocationHelper(Executor executor, LocationManager manager) {
        this.executor = executor;
        this.manager = manager;

        // Register handler to keep track of amount of satellites used
        manager.registerGnssStatusCallback(executor, new GnssStatus.Callback() {
            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                super.onSatelliteStatusChanged(status);
                int temp = 0;
                final int satelliteCount = status.getSatelliteCount();
                for (int i = 0; i < satelliteCount; i++) {
                    if (status.usedInFix(i)) {
                        temp++;
                    }
                }
                usedSatellites = temp;
            }
        });
    }

    @SuppressLint("MissingPermission")
    public Location getLocation() {
        // Get all enabled providers
        final List<String> providers = manager.getProviders(true);

        // Keep track of best one found
        Location bestLocation = null;
        float bestAccuracy = Float.MAX_VALUE;

        // Go through each provider
        for (String provider : providers) {
            // Get last cached using current provider
            Location location = manager.getLastKnownLocation(provider);
            // Check if the best accuracy so far
            if (location != null) {
                long time = SystemClock.elapsedRealtimeNanos() - location.getElapsedRealtimeNanos();
                if (time < updateThreshold && location.getAccuracy() < bestAccuracy) {
                    bestLocation = location;
                    bestAccuracy = location.getAccuracy();
                }
            }
        }

        // Return if we found something
        if (bestLocation != null) {
            return bestLocation;
        }

        // If we get here we ain't got shit, run manual update
        return updateLocation();
    }

    @SuppressLint("MissingPermission")
    private Location updateLocation() {
        List<String> providers = manager.getProviders(true);
        Location[] locations = new Location[providers.size()];
        CountDownLatch latch = new CountDownLatch(providers.size());
        for (int i = 0; i < providers.size(); i++) {
            int j = i;
            Log.d("wang", providers.get(j) + " starting");
            manager.getCurrentLocation(providers.get(j), null, executor, v -> {
                Log.d("wang", providers.get(j) + " done");
                locations[j] = v;
                latch.countDown();
            });
        }
        try {
            latch.await();
            Log.d("wang", "all done");

            Location bestLocation = null;
            float bestAccuracy = Float.MAX_VALUE;
            for (Location location : locations) {
                if (location != null && location.getAccuracy() < bestAccuracy) {
                    bestLocation = location;
                    bestAccuracy = location.getAccuracy();
                }
            }
            return bestLocation;

        } catch (InterruptedException e) {
            return null;
        }
    }
}
