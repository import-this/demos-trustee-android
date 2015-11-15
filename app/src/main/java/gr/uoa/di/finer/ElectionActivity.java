package gr.uoa.di.finer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.UiThread;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import junit.framework.Assert;

import gr.uoa.di.finer.database.TrusteeContract;
import gr.uoa.di.finer.database.TrusteeOpenHelper;
import gr.uoa.di.finer.service.StoreException;
import gr.uoa.di.finer.service.TrusteeService;


public class ElectionActivity extends BaseActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = ElectionActivity.class.getName();

    private static final int ELECTION_LOADER_ID = 1;

    public static final String EXTRA_ELECTION_ID = "gr.uoa.di.finer.extra.ELECTION_ID";


    private boolean firstOnResumeCall = true;

    private ProgressBar progressBar;

    private TrusteeOpenHelper dbHelper;

    private BroadcastReceiver statusReceiver;
    private IntentFilter statusIntentFilter;

    private void registerStatusReceiver() {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.registerReceiver(statusReceiver, statusIntentFilter);
    }

    private void unregisterStatusReceiver() {
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.unregisterReceiver(statusReceiver);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_election);
        setSupportActionBar((Toolbar) findViewById(R.id.action_toolbar));

        // Find which election the user chose.
        currentElectionId = getIntent().getStringExtra(EXTRA_ELECTION_ID);
        Assert.assertNotNull(currentElectionId);

        // The activity should always has an action bar or toolbar.
        assert getSupportActionBar() != null;
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(
                getString(R.string.activity_election_title) + " " + currentElectionId);

        progressBar = (ProgressBar) findViewById(R.id.progressbar);
        dbHelper = new TrusteeOpenHelper(this);

        // Load the data for the election asynchronously.
        getSupportLoaderManager().initLoader(ELECTION_LOADER_ID, null, this);

        statusReceiver = new ElectionStatusReceiver();
        statusIntentFilter = new IntentFilter(TrusteeService.ACTION_BROADCAST_STATUS);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Restart the loader to get any new data after onPause().
        if (firstOnResumeCall) {
            firstOnResumeCall = false;
        } else {
            getSupportLoaderManager().restartLoader(ELECTION_LOADER_ID, null, this);
        }
        registerStatusReceiver();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterStatusReceiver();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflates the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_election, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_erase:
                openEraseElectionDialog(currentElectionId);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /*
     * Note that we should not call close() on the Cursor from our AsyncTaskLoader ourselves.
     * In order to remove the reference of the Cursor in our CursorAdapter, we use the
     * CursorAdapter#swapCursor(android.database.Cursor) method so that the old Cursor
     * is not closed. (CursorAdapter#changeCursor(android.database.Cursor) closes it.)
     * http://developer.android.com/reference/android/support/v4/app/LoaderManager.LoaderCallbacks.html
     */

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle args) {
        switch (loaderID) {
            case ELECTION_LOADER_ID:
                // Return a cursor loader for the election with ID 'currentElectionId'.
                return new SimpleCursorLoader(this, dbHelper, new SimpleCursorLoader.Query() {
                    @Override
                    public Cursor make(ReadableDataStore store) throws StoreException {
                        return store.getElection(currentElectionId, null);
                    }
                });
            default:
                Utils.wtf(TAG, "Invalid loader id");
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // A SimpleCursorLoader will return null in case of error.
        if (cursor == null) {
            ContextUtils.showToast(this, R.string.toast_db_load_error);
            return;
        }

        // We don't keep any references to the cursors in this activity. So, just update the UI.
        final boolean succeeded = cursor.moveToFirst();
        int column;

        // The election ID should always be valid and the cursor should have exactly one row.
        Assert.assertTrue("Cursor is empty", succeeded);

        column = cursor.getColumnIndexOrThrow(TrusteeContract.Election.COLUMN_NAME_ELECTION_ID);
        ((TextView) findViewById(R.id.election_id)).setText(cursor.getString(column));
        column = cursor.getColumnIndexOrThrow(TrusteeContract.Election.COLUMN_NAME_QUESTION);
        ((TextView) findViewById(R.id.election_question)).setText(cursor.getString(column));

        column = cursor.getColumnIndexOrThrow(TrusteeContract.Election.COLUMN_NAME_START_TIME);
        ((TextView) findViewById(R.id.election_start_time)).setText(
                Utils.timestampToString(cursor.getLong(column)));
        column = cursor.getColumnIndexOrThrow(TrusteeContract.Election.COLUMN_NAME_END_TIME);
        ((TextView) findViewById(R.id.election_end_time)).setText(
                Utils.timestampToString(cursor.getLong(column)));

        column = cursor.getColumnIndexOrThrow(TrusteeContract.Election.COLUMN_NAME_ABB_URL);
        ((TextView) findViewById(R.id.election_abb_url)).setText(cursor.getString(column));

        TextView textView;
        textView = (TextView) findViewById(R.id.election_key);
        column = cursor.getColumnIndexOrThrow(
                TrusteeContract.Election.COLUMN_NAME_DECOMMITMENT_KEY);
        if (cursor.isNull(column)) {
            textView.setText(R.string.no_decommitment_key);
        } else {
            textView.setText(cursor.getString(column));
        }
        textView = (TextView) findViewById(R.id.election_bundle);
        column = cursor.getColumnIndexOrThrow(
                TrusteeContract.ElectionDynamicData.COLUMN_NAME_DECOMMITMENT_BUNDLE);
        if (cursor.isNull(column)) {
            textView.setText(R.string.no_decommitment_bundle);
        } else {
            textView.setText(cursor.getString(column));
        }

        column = cursor.getColumnIndexOrThrow(TrusteeContract.Election.COLUMN_NAME_STATUS);
        final ImageView button = (FloatingActionButton) findViewById(R.id.floating_action_button);
        final ElectionStatus status = ElectionStatus.valueOf(cursor.getInt(column));

        setAction(currentElectionId, status, button);
        setActionText(currentElectionId, status, (TextView) findViewById(R.id.election_button));
        setProgressBar(currentElectionId, status, progressBar);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // We don't keep any references to the cursors in this activity. Nothing to do here.
    }


    @UiThread
    private final class ElectionStatusReceiver extends ServiceStatusReceiver {
        private void restartLoader() {
            // Restart the loader to refresh the data in our activity.
            getSupportLoaderManager().restartLoader(ELECTION_LOADER_ID, null, ElectionActivity.this);
        }

        private void showProgress(int progress) {
            if (BuildConfig.DEBUG) Assert.assertNotSame(progress, -1);
            progressBar.setProgress(progress);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final String electionId = intent.getStringExtra(TrusteeService.EXTRA_ELECTION_ID);

            // The broadcast was intended for another election or action.
            if (!ElectionActivity.this.currentElectionId.equals(electionId)) {
                return;
            }
            // Only ACTION_BROADCAST_STATUS is handled here.
            if (BuildConfig.DEBUG) {
                Assert.assertEquals(intent.getAction(), TrusteeService.ACTION_BROADCAST_STATUS);
            }

            switch (intent.getIntExtra(TrusteeService.EXTRA_REQUEST_STATUS, -1)) {
                case TrusteeService.STATUS_INITIALIZING_ELECTION:
                case TrusteeService.STATUS_VERIFYING_ELECTION:
                case TrusteeService.STATUS_ERASING_ELECTION:
                    restartLoader();
                    break;
                case TrusteeService.STATUS_INITIALIZING_ELECTION_PROGRESS:
                    // Indeterminate progress mode. Nothing to update here.
                    break;
                case TrusteeService.STATUS_INITIALIZED_ELECTION:
                    progressBar.setVisibility(View.INVISIBLE);
                    showToast(electionId, R.string.toast_service_election_initialized);
                    restartLoader();
                    break;
                case TrusteeService.STATUS_VERIFYING_ELECTION_PROGRESS:
                    showProgress(intent.getIntExtra(TrusteeService.EXTRA_BALLOT_PROGRESS, -1));
                    break;
                case TrusteeService.STATUS_VERIFIED_ELECTION:
                case TrusteeService.STATUS_COMPLETED_ELECTION:
                    progressBar.setVisibility(View.INVISIBLE);
                    showToast(electionId, R.string.toast_service_election_completed);
                    restartLoader();
                    break;
                case TrusteeService.STATUS_ERASED_ELECTION:
                    progressBar.setVisibility(View.INVISIBLE);
                    // The election was erased, so this activity should stop.
                    finish();
                    break;
                case TrusteeService.STATUS_CANCELLED_OPERATION:
                    progressBar.setVisibility(View.INVISIBLE);
                    restartLoader();
                    break;
                default:
                    // Hide the progress bar if visible.
                    progressBar.setVisibility(View.INVISIBLE);
                    restartLoader();
                    // Delegate handling to superclass.
                    super.onReceive(context, intent);
                    break;
            }
        }
    }

}
