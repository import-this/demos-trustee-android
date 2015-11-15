package gr.uoa.di.finer.net;

import android.support.annotation.WorkerThread;
import android.util.Log;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import gr.uoa.di.finer.BuildConfig;
import gr.uoa.di.finer.RequestSender;

/**
 *
 * @author Vasilis Poulimenos
 */
@WorkerThread
public class HTTPRequestSender extends RequestSender {

    // For Gingerbread and later (2.3+), HttpURLConnection is the best choice.
    // http://android-developers.blogspot.gr/2011/09/androids-http-clients.html
    // http://developer.android.com/reference/java/net/HttpURLConnection.html

    private static final String TAG = HTTPRequestSender.class.getName();

    private static final int CONNECT_TIMEOUT_MILLIS = 1_000 * 30;           // 30 seconds
    private static final int READ_TIMEOUT_MILLIS = 1_000 * 60;              // 1 minute

    private final URL url;

    public HTTPRequestSender(String urlString) throws MalformedURLException {
        super(urlString);
        this.url = new URL(urlString);
    }

    private HttpURLConnection openConnection() throws IOException {
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(CONNECT_TIMEOUT_MILLIS);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, String.format("Set connect timeout to %,d ms", CONNECT_TIMEOUT_MILLIS));
        }
        connection.setReadTimeout(READ_TIMEOUT_MILLIS);
        if (BuildConfig.DEBUG) {
            Log.d(TAG, String.format("Set read timeout to %,d ms", READ_TIMEOUT_MILLIS));
        }
        return connection;
    }

    /**
     *
     * @return
     * @throws IOException
     * @throws MalformedURLException
     */
    @Override
    public HTTPConnection sendGetRequest() throws IOException {
        HttpURLConnection connection = openConnection();
        if (BuildConfig.DEBUG) Log.d(TAG, "Sending GET request to " + urlString);
        return new HTTPConnection(connection);
    }

    /**
     *
     * @return
     * @throws IOException
     */
    @Override
    public HTTPConnection sendPostRequest() throws IOException {
        HttpURLConnection connection = openConnection();
        connection.setDoOutput(true);
        //connection.setChunkedStreamingMode(256);
        if (BuildConfig.DEBUG) Log.d(TAG, "Sending POST request to " + urlString);
        return new HTTPConnection(connection);
    }

}
