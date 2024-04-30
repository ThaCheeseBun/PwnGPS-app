package nu.cheese.pwngps;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.LocationManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import java.io.IOException;

import fi.iki.elonen.NanoHTTPD;

public class MainService extends Service {
    private final IBinder binder = new LocalBinder();
    private Server server;

    public class LocalBinder extends Binder {
        MainService getService() {
            return MainService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        final LocationHelper locationHelper = new LocationHelper(
                getMainExecutor(),
                (LocationManager) getSystemService(Context.LOCATION_SERVICE)
        );
        server = new Server(42069, locationHelper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final Notification notification = new NotificationCompat.Builder(this, MainActivity.NOTIFICATION_CHANNEL)
                .setOngoing(true)
                .build();

        ServiceCompat.startForeground(
                this,
                100,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        );

        if (!server.isAlive()) {
            try {
                server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            } catch (IOException e) {
                Log.e("Service", String.valueOf(e));
            }
        }

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public void onDestroy() {
        server.closeAllConnections();
        server.stop();
    }
}
