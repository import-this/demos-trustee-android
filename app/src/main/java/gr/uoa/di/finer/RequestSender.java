package gr.uoa.di.finer;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.io.IOException;
import java.io.Writer;

/**
 *
 * @author Vasilis Poulimenos
 */
public abstract class RequestSender {

    private static final String TAG = RequestSender.class.getName();

    /*
     * http://developer.android.com/training/monitoring-device-state/connectivity-monitoring.html
     * http://stackoverflow.com/a/4009133/1751037
     * http://stackoverflow.com/a/4239019/1751037
     */

    private static NetworkInfo getActiveNetworkInfo(Context context) {
        ConnectivityManager connectivityManager =
            (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        return connectivityManager.getActiveNetworkInfo();
    }

    private static boolean isConnected(NetworkInfo network) {
        return network != null && network.isConnected();
    }

    private static boolean isWifi(NetworkInfo network) {
        return network.getType() == ConnectivityManager.TYPE_WIFI;
    }


    /**
     *
     * @param context
     * @return {@code true} if the device is connected to the Internet
     */
    public static boolean isConnected(Context context) {
        NetworkInfo activeNetwork = getActiveNetworkInfo(context);
        return isConnected(activeNetwork);
    }

    /**
     *
     * @param context
     * @return {@code true} if the device is connected to the Internet via WiFi
     */
    public static boolean isConnectedViaWifi(Context context) {
        NetworkInfo activeNetwork = getActiveNetworkInfo(context);
        return isConnected(activeNetwork) && isWifi(activeNetwork);
    }


    protected final String urlString;

    protected RequestSender(String urlString) {
        this.urlString = urlString;
    }


    /**
     *
     * @return
     * @throws IOException
     */
    public abstract URLConnection sendGetRequest() throws IOException;

    /**
     *
     * @return
     * @throws IOException
     */
    public abstract URLConnection sendPostRequest() throws IOException;

    /**
     *
     * @param result
     * @throws IOException
     */
    public void postResult(String result) throws IOException {
        URLConnection connection = null;
        Writer writer = null;

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Posting result '" + result + "' to " + urlString);
        }
        try {
            connection = sendPostRequest();
            writer = connection.getOutputStreamWriter();
            // Buffering for a single bulk write is unnecessary.
            writer.write(result);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error closing connection output stream writer", e);
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

}
