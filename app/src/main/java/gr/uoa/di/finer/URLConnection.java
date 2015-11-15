package gr.uoa.di.finer;

import android.support.annotation.WorkerThread;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

/**
 *
 * @author Vasilis Poulimenos
 */
@WorkerThread
public interface URLConnection {

    /**
     *
     * @return
     * @throws IOException
     */
    Reader getInputStreamReader() throws IOException;

    /**
     *
     * @return
     * @throws IOException
     */
    Writer getOutputStreamWriter() throws IOException;

    /**
     *
     */
    void disconnect();

}
