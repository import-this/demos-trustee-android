package gr.uoa.di.finer;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 *
 * @author Vasilis Poulimenos
 */
public final class ContextUtils {

    // Prevent instantiation.
    private ContextUtils() { throw new AssertionError("Non-instantiable class"); }

    private static final String TAG = ContextUtils.class.getName();

    public static final int SELECT_FILE_REQUEST = 0;

    /**
     *
     * @param context
     * @param toastMessageId
     */
    public static void showToast(Context context, @StringRes int toastMessageId) {
        Toast.makeText(context, toastMessageId, Toast.LENGTH_SHORT).show();
    }

    /**
     *
     * @param context
     * @param toastMessage
     */
    public static void showToast(Context context, String toastMessage) {
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show();
    }

    /**
     *
     * @param activity
     */
    public static void askUserForFile(Activity activity) {
        final Intent intent = new Intent(Intent.ACTION_GET_CONTENT)
                .setType("*/*")
                .addCategory(Intent.CATEGORY_OPENABLE);

        try {
            activity.startActivityForResult(
                    Intent.createChooser(intent, activity.getText(R.string.choose_file)),
                    SELECT_FILE_REQUEST);
        } catch (ActivityNotFoundException ignored) {
            // TODO: Potentially direct the user to the Market with a Dialog.
            showToast(activity, R.string.toast_no_file_manager);
        }
    }

    /**
     * a buffered input stream
     * As with any other stream, don't forget to close it when you are done.
     *
     * @param context
     * @param uri
     * @return
     * @throws FileNotFoundException
     */
    public static InputStream openInputFile(Context context, Uri uri) throws FileNotFoundException {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "File URI: " + uri.toString() + ", File Path: " + uri.getPath());
        }
        return new BufferedInputStream(context.getContentResolver().openInputStream(uri));
    }

}
