package nu.cheese.pwngps;

import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;

import fi.iki.elonen.NanoHTTPD;

public class Server extends NanoHTTPD {
    private final LocationHelper locationHelper;
    private final SimpleDateFormat dateFormat;

    public Server(int port, LocationHelper locationHelper) {
        super(port);
        this.locationHelper = locationHelper;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSSSZ", Locale.US);
        this.dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    @Override
    // Disable gzip compression
    protected boolean useGzipWhenAccepted(Response r) {
        return false;
    }

    @Override
    public Response serve(IHTTPSession session) {
        final String uri = session.getUri();
        if (uri != null && uri.equals("/gps.xhtml")) {
            try {
                final Location location = locationHelper.getLocation();
                if (location != null) {
                    final JSONObject obj = new JSONObject();
                    obj.put("Latitude", location.getLatitude());
                    obj.put("Longitude", location.getLongitude());
                    obj.put("Altitude", location.hasAltitude() ? location.getAltitude() : 0);
                    obj.put("Updated", dateFormat.format(location.getTime()));

                    // Only put amount of satellites used if provided by GPS
                    if (Objects.equals(location.getProvider(), LocationManager.GPS_PROVIDER)) {
                        obj.put("NumSatellites", locationHelper.getUsedSatellites());
                    } else {
                        obj.put("NumSatellites", 0);
                    }

                    return newFixedLengthResponse(Response.Status.OK, "application/json", obj.toString());
                }
            } catch (Exception e) {
                Log.e("serve", e.toString());
            }
            // If we end up here assume something went wrong
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "application/json", "{}");
        } else {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "Not Found");
        }
    }
}
