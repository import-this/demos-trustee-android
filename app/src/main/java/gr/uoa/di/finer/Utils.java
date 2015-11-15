package gr.uoa.di.finer;

import android.util.Log;

import java.text.DateFormat;
import java.util.Date;

/**
 *
 * @author Vasilis Poulimenos
 */
public final class Utils {

    // Prevent instantiation.
    private Utils() { throw new AssertionError("Non-instantiable class"); }

    /**
     * What a Terrible Failure. Log it and throw an AssertionError in debug builds.
     *
     * @param tag the tag for the log message
     * @param msg the log message
     */
    public static void wtf(String tag, String msg) {
        Log.wtf(tag, msg);
        if (BuildConfig.DEBUG) {
            throw new AssertionError(msg);
        }
    }

    /**
     * Converts a timestamp to a formatted date string with default formatting.
     *
     * @param millis the timestamp in milliseconds
     * @return the formatted date string
     */
    public static String timestampToString(long millis) {
        final DateFormat dateFormat = DateFormat.getDateTimeInstance();
        final Date date = new Date(millis);
        return dateFormat.format(date);
    }

    /**
     * Converts a value in the range 0..max to its corresponding percentage (i.e. 0..100).
     *
     * @param value the current value
     * @param max the maximum value (the upper limit of the range)
     * @return the corresponding percentage
     */
    public static int toPercentage(long value, long max) {
        return (int)((double)value / max * 100);
    }

}
