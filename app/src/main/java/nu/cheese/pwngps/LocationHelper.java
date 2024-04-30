package nu.cheese.pwngps;

import android.annotation.SuppressLint;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.SystemClock;

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

    private CountDownLatch updateRunning = new CountDownLatch(0);

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
    public Location getLocation() throws InterruptedException {
        // Get all enabled providers
        final List<String> providers = manager.getProviders(true);

        // Try each provider and find location with best accuracy
        Location bestLocation = null;
        float bestAccuracy = Float.MAX_VALUE;
        for (String provider : providers) {
            // Get last cached using current provider
            final Location location = manager.getLastKnownLocation(provider);
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
        // If update is already running, wait until done
        updateRunning.await();
        return updateLocation();
    }

    @SuppressLint("MissingPermission")
    private Location updateLocation() {
        // If already running return nothing
        if (updateRunning.getCount() > 0) {
            return null;
        }
        // Set running status
        updateRunning = new CountDownLatch(1);

        // Get all enabled providers
        final List<String> providers = manager.getProviders(true);
        // Try each provider
        final Location[] locations = new Location[providers.size()];
        final CountDownLatch latch = new CountDownLatch(providers.size());
        for (int i = 0; i < providers.size(); i++) {
            int j = i;
            manager.getCurrentLocation(providers.get(j), null, executor, v -> {
                locations[j] = v;
                latch.countDown();
            });
        }

        // Catch if interrupted since we are waiting
        try {
            // Wait until done
            latch.await();
        } catch (InterruptedException e) {
            // How rude, you get nothing
            updateRunning.countDown();
            return null;
        }

        // Find location with best accuracy
        Location bestLocation = null;
        float bestAccuracy = Float.MAX_VALUE;
        for (Location location : locations) {
            if (location != null && location.getAccuracy() < bestAccuracy) {
                bestLocation = location;
                bestAccuracy = location.getAccuracy();
            }
        }

        // Reset running status and return result
        updateRunning.countDown();
        return bestLocation;
    }
}
