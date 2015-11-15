package gr.uoa.di.finer.net;

import android.support.annotation.WorkerThread;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.HttpURLConnection;

import gr.uoa.di.finer.URLConnection;

/**
 * A thin wrapper around HttpURLConnection.
 * You cannot create instances of this class directly. Use {@code HTTPRequestSender} instead.
 *
 * @author Vasilis Poulimenos
 */
@WorkerThread
public class HTTPConnection implements URLConnection {

    private final HttpURLConnection con;

    HTTPConnection(HttpURLConnection con) {
        this.con = con;
    }

    /*
     * On Android (2.3+), the default charset is UTF-8.
     * http://developer.android.com/reference/java/nio/charset/Charset.html
     */

    /**
     *
     * @return
     * @throws IOException
     */
    public InputStream getInputStream() throws IOException {
        return con.getInputStream();
    }

    /**
     *
     * @return
     * @throws IOException
     */
    public OutputStream getOutputStream() throws IOException {
        return con.getOutputStream();
    }

    /**
     * The input stream returned is NOT buffered.
     *
     * @return
     * @throws IOException
     */
    @Override
    public Reader getInputStreamReader() throws IOException {
        return new InputStreamReader(getInputStream());
    }

    /**
     * The output stream returned is NOT buffered.
     *
     * @return
     * @throws IOException
     */
    @Override
    public Writer getOutputStreamWriter() throws IOException {
        return new OutputStreamWriter(getOutputStream());
    }

    @Override
    public void disconnect() {
        con.disconnect();
    }

}
