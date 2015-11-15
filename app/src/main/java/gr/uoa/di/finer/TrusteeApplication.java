package gr.uoa.di.finer;

import android.app.Application;
import android.os.Build;
import android.os.Handler;
import android.os.StrictMode;
import android.util.Log;

/**
 * Application subclass used only to enable {@code StrictMode}. As stated in the docs, this is
 * normally not the best place for global application state, so it is not used for such purposes.
 *
 * @author Vasilis Poulimenos
 */
public final class TrusteeApplication extends Application {

    private static final String TAG = TrusteeApplication.class.getName();

    @Override
    public void onCreate() {
        super.onCreate();
        if (BuildConfig.DEBUG) {
            // Known Android bug and workaround:
            // Setting StrictMode here does not work in 4.1 and later.
            // https://code.google.com/p/android/issues/detail?id=35298
            enableStrictMode();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {  // 16
                // Restore strict mode after onCreate() returns.
                new Handler().postAtFrontOfQueue(new Runnable() {
                    @Override
                    public void run() {
                        enableStrictMode();
                    }
                });
            }
            Log.d(TAG, "Enabled StrictMode");
        }
    }

    private void enableStrictMode() {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build());
    }

}
