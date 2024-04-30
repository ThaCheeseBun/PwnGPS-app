package nu.cheese.pwngps;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.Manifest;
import android.os.IBinder;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {
    public final static String NOTIFICATION_CHANNEL = "SERVICE";

    private MainService service;
    private boolean serviceBound = false;
    private NotificationManager notificationManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            final Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        final NotificationChannel serviceChannel = new NotificationChannel(NOTIFICATION_CHANNEL, "Service Status", NotificationManager.IMPORTANCE_LOW);
        notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(serviceChannel);

        final Button permissionButton = findViewById(R.id.permissionButton);
        permissionButton.setOnClickListener(v -> {
            requestPermissions();
        });

        if (hasLocation() && hasNotifications()) {
            // If we have required permissions, immediately start service
            updatePermissionView(false);
            startService();
        } else {
            // Otherwise show permission layout
            updatePermissionView(true);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        getApplicationContext().unbindService(serviceConnection);
        serviceBound = false;
    }

    private boolean hasLocation() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private boolean hasNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        } else {
            return notificationManager.areNotificationsEnabled();
        }
    }
    private final ActivityResultLauncher<String[]> permissionRequest =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (hasLocation() && hasNotifications()) {
                    updatePermissionView(false);
                    startService();
                } else {
                    updatePermissionView(true);
                }
            });
    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.POST_NOTIFICATIONS
            });
        } else {
            permissionRequest.launch(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            });
        }
    }

    private void updatePermissionView(boolean toggle) {
        final ConstraintLayout permissionLayout = findViewById(R.id.permissionLayout);
        final ConstraintLayout serviceLayout = findViewById(R.id.serviceLayout);
        final TextView permissionStatus = findViewById(R.id.permissionStatus);

        if (toggle) {
            permissionLayout.setVisibility(View.VISIBLE);
            serviceLayout.setVisibility(View.GONE);
            permissionStatus.setText(String.format("Location: %s\nNotifications: %s", hasLocation(), hasNotifications()));
        } else {
            permissionLayout.setVisibility(View.GONE);
            serviceLayout.setVisibility(View.VISIBLE);
        }
    }

    private void startService() {
        final Intent intent = new Intent(this, MainService.class);
        final Context ctx = getApplicationContext();
        ctx.startForegroundService(intent);
        ctx.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
    }

    @SuppressLint("SetTextI18n")
    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder binder) {
            MainService.LocalBinder localBinder = (MainService.LocalBinder) binder;
            service = localBinder.getService();
            serviceBound = true;

            final TextView serviceStatus = findViewById(R.id.serviceStatus);
            serviceStatus.setText("Service is running :)");
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            serviceBound = false;

            final TextView serviceStatus = findViewById(R.id.serviceStatus);
            serviceStatus.setText("Service is not running, restart app");
        }
    };
}