package gr.uoa.di.finer;

import android.content.Context;
import android.database.Cursor;
import android.support.annotation.UiThread;
import android.support.annotation.WorkerThread;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.util.Log;

import gr.uoa.di.finer.database.ReadableDatabaseHelper;
import gr.uoa.di.finer.database.TrusteeOpenHelper;
import gr.uoa.di.finer.service.StoreException;

/**
 * A simple loader that performs a cursor query on a background thread.
 * Note that this loader will NOT close the database associated with {@link TrusteeOpenHelper}.
 * This is left to the clients.
 *
 * @see AsyncTaskLoader
 * @see CursorLoader
 * @author Vasilis Poulimenos
 */
class SimpleCursorLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = SimpleCursorLoader.class.getName();

    /**
     * Interface used by this class to get a query to perform against a read-only data store.
     * Clients must implement this interface in order to specify the query they want.
     */
    public interface Query {
        /**
         * Performs the query in a background thread.
         *
         * @param store the read-only data store against which the query will be made
         * @return a Cursor holding the whole result set
         * @throws StoreException if the query failed for some reason
         */
        Cursor make(ReadableDataStore store) throws StoreException;
    }

    protected final TrusteeOpenHelper dbHelper;
    protected final Query query;
    protected Cursor cursor;

    /**
     * Creates a new {@code SimpleCursorLoader}.
     *
     * @param context the context in which the loader is created
     * @param dbHelper the helper that will be used to get the database
     * @param query the query to perform
     */
    SimpleCursorLoader(Context context, TrusteeOpenHelper dbHelper, Query query) {
        super(context);
        this.dbHelper = dbHelper;
        this.query = query;
    }

    /*
     * Runs on a worker thread.
     */
    @WorkerThread
    @Override
    public Cursor loadInBackground() {
        ReadableDataStore store = null;
        try {
            final Cursor cursor;

            // Lazy-load the database in the worker thread.
            store = ReadableDatabaseHelper.newInstance(dbHelper);
            cursor = query.make(store);
            // Ensure the cursor window is filled.
            // Android loads the whole result set into memory,
            // preventing disk accesses later on in the UI thread.
            cursor.getCount();
            return cursor;
        } catch (StoreException e) {
            Log.e(TAG, "DataStore error in loader", e);
            return null;
        } finally {
            if (store != null) {
                store.close();
            }
        }
    }

    /*
     * Called when there is new data to deliver to the client.
     * The super class will take care of delivering it.
     * <p/>
     * Must be called from the UI thread.
     */
    @UiThread
    @Override
    public void deliverResult(Cursor cursor) {
        if (isReset()) {
            // An async query came in while the loader is stopped. We don't need the result.
            if (cursor != null) {
                cursor.close();
            }
            return;
        }
        final Cursor oldCursor = this.cursor;
        this.cursor = cursor;

        // If the Loader is currently started, we can immediately deliver its results.
        if (isStarted()) {
            super.deliverResult(cursor);
        }

        // Release the resources associated with oldCursor if needed.
        if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
            oldCursor.close();
        }
    }

    /**
     * Starts an asynchronous load of the data. When the result is ready the callbacks will
     * be called on the UI thread. If a previous load has been completed and is still valid
     * the result may be passed to the callbacks immediately.
     * <p/>
     * Must be called from the UI thread.
     */
    @UiThread
    @Override
    protected void onStartLoading() {
        // If we currently have a result available, deliver it immediately.
        if (cursor != null) {
            deliverResult(cursor);
        }
        // If the data has changed or is not currently available, start a load.
        if (takeContentChanged() || cursor == null) {
            forceLoad();
        }
    }

    /*
     * Must be called from the UI thread.
     */
    @UiThread
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped.
        onStopLoading();

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        cursor = null;
    }

}
