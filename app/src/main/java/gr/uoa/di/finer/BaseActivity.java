package gr.uoa.di.finer;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.UiThread;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import junit.framework.Assert;

import gr.uoa.di.finer.service.TrusteeService;
import gr.uoa.di.finer.service.TrusteeServiceHelper;

/**
 * A base activity that implements common functionality across app activities.
 *
 * @author Vasilis Poulimenos
 */
abstract class BaseActivity extends AppCompatActivity {

    private static final String TAG = BaseActivity.class.getName();

    private static final String KEY_ELECTION_ID = "gr.uoa.di.finer.key.ELECTION_ID";


    /**
     * The election ID of the current election/operation.
     * Note:
     * This is needed in onActivityResult to specify the election for which the file was chosen.
     * Note that it is necessary to save/restore this in on(Save/Restore)InstanceState, because in
     * order for the user to choose a file, a new activity is started and our own may be killed.
     */
    protected String currentElectionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentElectionId = null;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        currentElectionId = savedInstanceState.getString(KEY_ELECTION_ID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The user interacts with the activity. No notifications from the service necessary here.
        TrusteeService.hideNotifications();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The user is about to leave the activity. Turn notifications from the service back on.
        TrusteeService.showNotifications();
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        savedInstanceState.putString(KEY_ELECTION_ID, currentElectionId);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        currentElectionId = null;
    }


    /**
     * Method called when the user clicks an "Initialize Election" button.
     */
    public void initializeElection() {
        ContextUtils.askUserForFile(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case ContextUtils.SELECT_FILE_REQUEST:
                if (BuildConfig.DEBUG) Log.d(TAG, "Select file request result code: " + resultCode);
                if (resultCode == RESULT_OK) {
                    if (BuildConfig.DEBUG) Assert.assertNotNull(currentElectionId);
                    TrusteeServiceHelper.getInstance().initializeElection(
                            this, currentElectionId, data.getData());
                }
                break;
            default:
                Utils.wtf(TAG, "Unexpected request code");
                break;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /**
     * Attempts to verify the election with the specified ID, after notifying the user.
     */
    public void verifyElection() {
        if (RequestSender.isConnected(getApplicationContext())) {
            if (BuildConfig.DEBUG) Assert.assertNotNull(currentElectionId);
            if (!RequestSender.isConnectedViaWifi(getApplicationContext())) {
                openDataUsageDialog(currentElectionId);
            } else {
                TrusteeServiceHelper.getInstance().verifyElection(this, currentElectionId);
            }
        } else {
            ContextUtils.showToast(getApplicationContext(), R.string.toast_no_internet);
        }
    }


    @UiThread
    public static class DataUsageDialogFragment extends DialogFragment {
        private static final String ELECTION_ID = "electionId";

        public static DataUsageDialogFragment newInstance(String electionId) {
            final DataUsageDialogFragment fragment = new DataUsageDialogFragment();

            final Bundle args = new Bundle();
            args.putString(ELECTION_ID, electionId);
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.data_usage_title)
                .setMessage(R.string.data_usage_message)
                .setPositiveButton(R.string.data_usage_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        final String electionId = getArguments().getString(ELECTION_ID);
                        TrusteeServiceHelper.getInstance().verifyElection(getContext(), electionId);
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // The user cancelled the action. Nothing to do here.
                    }
                })
                .create();
        }
    }

    @UiThread
    public static class EraseElectionDialogFragment extends DialogFragment {
        private static final String ELECTION_ID = "electionId";

        public static EraseElectionDialogFragment newInstance(String electionId) {
            final EraseElectionDialogFragment fragment = new EraseElectionDialogFragment();

            final Bundle args = new Bundle();
            args.putString(ELECTION_ID, electionId);
            fragment.setArguments(args);

            return fragment;
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.erase_election_title)
                .setMessage(R.string.erase_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        TrusteeServiceHelper.getInstance().eraseElection(
                                getContext(), getArguments().getString(ELECTION_ID));
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // The user cancelled the action. Nothing to do here.
                    }
                })
                .create();
        }
    }

    private void openDataUsageDialog(String electionId) {
        DataUsageDialogFragment fragment = DataUsageDialogFragment.newInstance(electionId);
        fragment.show(getSupportFragmentManager(), DataUsageDialogFragment.class.getName());
    }

    public void openEraseElectionDialog(String electionId) {
        EraseElectionDialogFragment fragment = EraseElectionDialogFragment.newInstance(electionId);
        fragment.show(getSupportFragmentManager(), EraseElectionDialogFragment.class.getName());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflates the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_base, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handles action bar item clicks. The action bar will automatically handle clicks on
        // the Home/Up button, since we have specified a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            /*case R.id.action_settings:

                return true;*/
            case R.id.action_about:

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     *
     * @param electionId
     * @param status the status of the election
     * @param view
     */
    protected void setActionText(String electionId, ElectionStatus status, TextView view) {
        boolean isPending = TrusteeServiceHelper.getInstance().isPending(electionId);

        if (status == ElectionStatus.COMPLETED) {
            view.setText(R.string.activity_election_text_completed);
            return;
        }
        if (isPending) {
            switch (status) {
                case UNINITIALIZED:
                case INITIALIZED:       // A request is already sent.
                    view.setText(R.string.activity_election_text_pending);
                    break;
                case INITIALIZING:
                    view.setText(R.string.activity_election_text_initializing);
                    break;
                case VERIFYING:
                    view.setText(R.string.activity_election_text_verifying);
                    break;
                case ERASING:
                    view.setText(R.string.activity_election_text_erasing);
                    break;
                default:
                    break;
            }
        } else {
            switch (status) {
                case UNINITIALIZED:
                case INITIALIZING:      // An error occurred earlier.
                    view.setText(R.string.activity_election_text_initialize);
                    break;
                case INITIALIZED:
                case VERIFYING:         // An error occurred earlier.
                    view.setText(R.string.activity_election_text_verify);
                    break;
                case ERASING:
                    view.setText(R.string.activity_election_text_invalid);
                    break;
                default:
                    break;
            }
        }
    }

    private void getElectionId(View v) {
        Object tag = v.getTag();
        // Get the election ID associated with the view, if one exists.
        if (tag != null) {
            currentElectionId = (String) tag;
        }
    }

    /**
     *
     */
    protected class UninitializedElectionListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            getElectionId(v);
            initializeElection();
        }
    }

    /**
     *
     */
    protected class InitializedElectionListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            getElectionId(v);
            verifyElection();
        }
    }

    /**
     *
     */
    protected class PendingElectionListener implements View.OnClickListener {
        @Override
        public void onClick(View v) {
            getElectionId(v);
            cancelElection();
        }

        private void cancelElection() {
            if (BuildConfig.DEBUG) Assert.assertNotNull(currentElectionId);
            TrusteeServiceHelper.getInstance().cancelOperation(BaseActivity.this, currentElectionId);
        }
    }

    /**
     * Factory method for a new UninitializedElectionListener.
     * @return a new UninitializedElectionListener
     */
    protected View.OnClickListener newUninitializedElectionListener() {
        return new UninitializedElectionListener();
    }

    /**
     * Factory method for a new InitializedElectionListener.
     * @return a new InitializedElectionListener
     */
    protected View.OnClickListener newInitializedElectionListener() {
        return new InitializedElectionListener();
    }

    /**
     * Factory method for a new PendingElectionListener.
     * @return a new InitializedElectionListener
     */
    protected View.OnClickListener newPendingElectionListener() {
        return new PendingElectionListener();
    }

    /**
     * Keep a reference to each listener to reuse them for all corresponding onClick events.
     */
    private final View.OnClickListener uninitializedElectionListener =
            newUninitializedElectionListener();
    private final View.OnClickListener initializedElectionListener =
            newInitializedElectionListener();
    private final View.OnClickListener pendingElectionListener =
            newPendingElectionListener();

    /**
     *
     * @param electionId
     * @param status the status of the election
     * @param imageView the view upon which to set the action listener
     */
    protected void setAction(String electionId, ElectionStatus status, ImageView imageView) {
        boolean isPending = TrusteeServiceHelper.getInstance().isPending(electionId);

        switch (status) {
            case COMPLETED:
                imageView.setImageResource(R.drawable.ic_done_white_48dp);
                imageView.setOnClickListener(null);
                imageView.setClickable(false);
                break;
            case ERASING:
                // No action.
                imageView.setVisibility(View.INVISIBLE);
                break;
            default:
                // Handle the rest separately.
                if (isPending) {
                    imageView.setImageResource(R.drawable.ic_clear_white_48dp);
                    imageView.setOnClickListener(pendingElectionListener);
                } else {
                    switch (status) {
                        case UNINITIALIZED:
                        case INITIALIZING:          // An error occurred earlier.
                            imageView.setImageResource(R.drawable.ic_play_arrow_white_48dp);
                            imageView.setOnClickListener(uninitializedElectionListener);
                            break;
                        case INITIALIZED:
                        case VERIFYING:             // An error occurred earlier.
                            imageView.setImageResource(R.drawable.ic_cloud_download_white_48dp);
                            imageView.setOnClickListener(initializedElectionListener);
                            break;
                        default:
                            break;
                    }
                }
                break;
        }
    }

    /**
     *
     * @param status
     * @param progressBar
     */
    protected void setProgressBar(String electionId, ElectionStatus status, ProgressBar progressBar) {
        boolean isPending = TrusteeServiceHelper.getInstance().isPending(electionId);

        switch (status) {
            case UNINITIALIZED:
            case INITIALIZED:
            case COMPLETED:
                progressBar.setVisibility(View.INVISIBLE);
                break;
            default:
                // Handle the rest separately.
                if (!isPending) {
                    progressBar.setVisibility(View.INVISIBLE);
                    break;
                }
                progressBar.setVisibility(View.VISIBLE);
                switch (status) {
                    case INITIALIZING:
                    case ERASING:
                        progressBar.setIndeterminate(true);
                        break;
                    case VERIFYING:
                        progressBar.setIndeterminate(false);
                        break;
                    default:
                        // Nothing to do here.
                        break;
                }
                break;
        }
    }


    /**
     * A broadcast receiver that receives status updates from the main service.
     * <p>
     * This receiver is not registered in this activity. Subclasses should override this to
     * define their specific functionality and decide whether or not they will register it to
     * receive broadcasts.
     * <p>
     * This broadcast receiver only receives info that will be used to update the UI in some way,
     * so it should be registered in the onResume method (and unregistered in onPause respectively).
     * <p>
     * Note that a Dialog is not an Activity, so it will not replace the current
     * one at the top of the stack (and, thus, will not cause anything to pause).
     * http://stackoverflow.com/a/7384782/1751037
     */
    @UiThread
    protected class ServiceStatusReceiver extends BroadcastReceiver {
        protected void showToast(String electionId, @StringRes int toastMessageId) {
            ContextUtils.showToast(getApplicationContext(), getString(toastMessageId, electionId));
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // Note that the election ID may be null.
            final String electionId = intent.getStringExtra(TrusteeService.EXTRA_ELECTION_ID);

            // Only ACTION_BROADCAST_STATUS is handled here.
            if (BuildConfig.DEBUG) {
                Assert.assertEquals(intent.getAction(), TrusteeService.ACTION_BROADCAST_STATUS);
            }

            switch (intent.getIntExtra(TrusteeService.EXTRA_REQUEST_STATUS, -1)) {
                // Failures
                case TrusteeService.STATUS_FILE_NOT_FOUND:
                    showToast(electionId, R.string.notification_file_not_found);
                    break;
                case TrusteeService.STATUS_IO_ERROR:
                    showToast(electionId, R.string.notification_io_error);
                    break;
                case TrusteeService.STATUS_PARSE_ERROR:
                    showToast(electionId, R.string.notification_parse_error);
                    break;
                case TrusteeService.STATUS_STORAGE_ERROR:
                    showToast(electionId, R.string.notification_storage_error);
                    break;
                case TrusteeService.STATUS_STORAGE_FULL_ERROR:
                    showToast(electionId, R.string.notification_storage_full_error);
                    break;
                case TrusteeService.STATUS_INVALID_URL:
                    showToast(electionId, R.string.notification_invalid_url);
                    break;
                case TrusteeService.STATUS_SOCKET_TIMEOUT:
                    showToast(electionId, R.string.notification_timeout);
                    break;
                case -1:
                    Utils.wtf(TAG, "No status code was stored in the intent");
                    break;
                default:
                    // Some status codes are ignored.
                    break;
            }
        }
    }

}
