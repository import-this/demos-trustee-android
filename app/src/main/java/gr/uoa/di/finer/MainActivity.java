package gr.uoa.di.finer;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TimePicker;

import junit.framework.Assert;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import gr.uoa.di.finer.database.TrusteeContract;
import gr.uoa.di.finer.database.TrusteeOpenHelper;
import gr.uoa.di.finer.service.StoreException;
import gr.uoa.di.finer.service.TrusteeService;
import gr.uoa.di.finer.service.TrusteeServiceHelper;


/**
 * The main activity and entry point for our app.
 *
 * @author Vasilis Poulimenos
 */
public class MainActivity extends BaseActivity
        implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final String TAG = MainActivity.class.getName();

    private static final int ELECTIONS_LOADER_ID = 0;


    private boolean firstOnResumeCall = true;

    private SimpleCursorAdapter listAdapter;
    private TrusteeOpenHelper dbHelper;

    private final String[] fromColumns = new String[] {
            TrusteeContract.Election.COLUMN_NAME_ELECTION_ID,
            TrusteeContract.Election.COLUMN_NAME_START_TIME,
            TrusteeContract.Election.COLUMN_NAME_STATUS
    };

    private final int[] toViews = new int[] {
            R.id.list_item_election_id,
            R.id.list_item_election_info,
            R.id.list_item_action
    };

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


    /**
     * A map that matches active elections with their progress bars.
     */
    private final Map<String, ProgressBar> electionProgressBars = new HashMap<>(4);


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.action_toolbar));
        //getSupportActionBar().setTitle(R.string.app_name);
        //getSupportActionBar().setSubtitle();

        dbHelper = new TrusteeOpenHelper(this);

        // Load the data for the elections asynchronously.
        getSupportLoaderManager().initLoader(ELECTIONS_LOADER_ID, null, this);

        listAdapter = new SimpleCursorAdapter(
                this,
                R.layout.list_item_election,
                null,                               // No cursor yet. We will get one in our loader.
                fromColumns,
                toViews,
                0                                   // No flags.
        );
        listAdapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {
            @Override
            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                switch (columnIndex) {
                    case 1:     // Start time - Election info
                        ((TextView) view).setText(Utils.timestampToString(cursor.getLong(columnIndex)));
                        return true;
                    case 2:     // Status - Action/ProgressBar
                        final String electionId = cursor.getString(cursor.getColumnIndexOrThrow(
                                TrusteeContract.Election.COLUMN_NAME_ELECTION_ID));
                        final ElectionStatus status = ElectionStatus.valueOf(
                                cursor.getInt(cursor.getColumnIndexOrThrow(
                                        TrusteeContract.Election.COLUMN_NAME_STATUS)));
                        final ProgressBar progressBar =
                                (ProgressBar) ((ViewGroup) view.getParent())
                                        .findViewById(R.id.list_item_progress);

                        electionProgressBars.put(electionId, progressBar);
                        setAction(electionId, status, (ImageButton) view);
                        setProgressBar(electionId, status, progressBar);
                        // The election ID tag will be used when the action listener is executed.
                        view.setTag(electionId);
                        return true;
                    default:
                        return false;
                }
            }
        });

        ListView listView = (ListView) findViewById(R.id.elections_list_view);
        // AdapterViews do not support adding views, so pass false to attachToRoot.
        View footer = getLayoutInflater().inflate(R.layout.footer, listView, false);
        // Note: The footer must be added before setting the adapter.
        listView.addFooterView(footer, null, false);

        listView.setAdapter(listAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final Cursor cursor = (Cursor) parent.getAdapter().getItem(position);
                viewElectionDetails(cursor.getString(cursor.getColumnIndex(
                        TrusteeContract.Election.COLUMN_NAME_ELECTION_ID)));
            }

            private void viewElectionDetails(String electionId) {
                Intent intent = new Intent(MainActivity.this, ElectionActivity.class)
                        .putExtra(ElectionActivity.EXTRA_ELECTION_ID, electionId);
                startActivity(intent);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                parent.getItemAtPosition(position);
                // TODO: Offer edit election here.
                return true;
            }
        });

        findViewById(R.id.floating_action_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNewElectionDialog();
            }
        });

        statusReceiver = new ElectionsStatusReceiver();
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
            getSupportLoaderManager().restartLoader(ELECTIONS_LOADER_ID, null, this);
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


    @UiThread
    public static class NewElectionDialogFragment extends DialogFragment {
        public static NewElectionDialogFragment newInstance() {
            return new NewElectionDialogFragment();
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final LayoutInflater inflater = getActivity().getLayoutInflater();
            final View view = inflater.inflate(R.layout.dialog_new_election, null);
            final EditText idEditText = ((EditText) view.findViewById(R.id.dialog_election_id));
            final EditText urlEditText = ((EditText) view.findViewById(R.id.dialog_abb_url));

            final Calendar currentDate = Calendar.getInstance();
            final int year = currentDate.get(Calendar.YEAR);
            final int month = currentDate.get(Calendar.MONTH);
            final int day = currentDate.get(Calendar.DAY_OF_MONTH);

            final DatePicker startDatePicker = (DatePicker) view.findViewById(R.id.dialog_start_date);
            final DatePicker endDatePicker = (DatePicker) view.findViewById(R.id.dialog_end_date);
            final TimePicker startTimePicker = (TimePicker) view.findViewById(R.id.dialog_start_time);
            final TimePicker endTimePicker = (TimePicker) view.findViewById(R.id.dialog_end_time);

            startDatePicker.init(year, month, day, new DatePicker.OnDateChangedListener() {
                @Override
                public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    // The end date will most likely be near the start date, so set it when changed.
                    // This will also make sure that the start date is not later than the end date.
                    endDatePicker.updateDate(year, monthOfYear, dayOfMonth);
                }
            });
            startTimePicker.setCurrentHour(0);
            startTimePicker.setCurrentMinute(0);
            startTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override
                public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                    final Calendar startDateTime = Calendar.getInstance();
                    final Calendar endDateTime = Calendar.getInstance();

                    startDateTime.set(
                            startDatePicker.getYear(), startDatePicker.getMonth(),
                            startDatePicker.getDayOfMonth(), hourOfDay, minute, 0);
                    startDateTime.set(Calendar.MILLISECOND, 0);
                    endDateTime.set(
                            endDatePicker.getYear(), endDatePicker.getMonth(),
                            endDatePicker.getDayOfMonth(), endTimePicker.getCurrentHour(),
                            endTimePicker.getCurrentMinute(), 0);
                    endDateTime.set(Calendar.MILLISECOND, 0);

                    // The start time must not be later than the end time if the date is the same.
                    if (startDateTime.after(endDateTime)) {
                        view.setCurrentHour(endTimePicker.getCurrentHour());
                        view.setCurrentMinute(endTimePicker.getCurrentMinute());
                    }
                }
            });

            // Advance the clock by 12 hours, in order to use as a useful default.
            currentDate.set(Calendar.HOUR_OF_DAY, 0);
            currentDate.add(Calendar.AM_PM, 1);

            endDatePicker.init(year, month, day, new DatePicker.OnDateChangedListener() {
                @Override
                public void onDateChanged(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                    final Calendar startDate = Calendar.getInstance();
                    final Calendar endDate = Calendar.getInstance();

                    endDate.set(year, monthOfYear, dayOfMonth, 0, 0, 0);
                    endDate.set(Calendar.MILLISECOND, 0);
                    startDate.set(startDatePicker.getYear(), startDatePicker.getMonth(),
                            startDatePicker.getDayOfMonth(), 0, 0, 0);
                    startDate.set(Calendar.MILLISECOND, 0);

                    // The end date must not be earlier than the start date, so forbid it.
                    if (endDate.before(startDate)) {
                        view.updateDate(
                                startDatePicker.getYear(),
                                startDatePicker.getMonth(),
                                startDatePicker.getDayOfMonth());
                    }
                }
            });
            endTimePicker.setCurrentHour(currentDate.get(Calendar.HOUR_OF_DAY));
            endTimePicker.setCurrentMinute(0);
            endTimePicker.setOnTimeChangedListener(new TimePicker.OnTimeChangedListener() {
                @Override
                public void onTimeChanged(TimePicker view, int hourOfDay, int minute) {
                    final Calendar startDateTime = Calendar.getInstance();
                    final Calendar endDateTime = Calendar.getInstance();

                    endDateTime.set(
                            endDatePicker.getYear(), endDatePicker.getMonth(),
                            endDatePicker.getDayOfMonth(), hourOfDay, minute, 0);
                    endDateTime.set(Calendar.MILLISECOND, 0);
                    startDateTime.set(
                            startDatePicker.getYear(), startDatePicker.getMonth(),
                            startDatePicker.getDayOfMonth(), startTimePicker.getCurrentHour(),
                            startTimePicker.getCurrentMinute(), 0);
                    startDateTime.set(Calendar.MILLISECOND, 0);

                    // The end time must not be earlier than the start time if the date is the same.
                    if (endDateTime.before(startDateTime)) {
                        // and it should not be the same time either, so advance it by 1 hour.
                        view.setCurrentHour(startTimePicker.getCurrentHour() + 1);
                        view.setCurrentMinute(startTimePicker.getCurrentMinute());
                    }
                }
            });

            idEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        final EditText editText = (EditText) v;

                        if (TextUtils.isEmpty(editText.getText())) {
                            editText.setError(getText(R.string.error_empty_election_id));
                        }
                    }
                }
            });
            idEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (TextUtils.isEmpty(s)) {
                        idEditText.setError(getText(R.string.error_empty_election_id));
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });
            urlEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (hasFocus) {
                        final EditText editText = (EditText) v;

                        if (TextUtils.isEmpty(editText.getText())) {
                            editText.setText(R.string.url_prefix);
                        }
                    }
                }
            });
            urlEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (TextUtils.isEmpty(s)) {
                        urlEditText.setError(getText(R.string.error_empty_url));
                    } else {
                        try {
                            new URL(s.toString()).toURI();
                        } catch (MalformedURLException | URISyntaxException ignored) {
                            urlEditText.setError(getText(R.string.error_invalid_url));
                        }
                    }
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.new_election)
                .setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        final EditText electionIdEditText =
                                (EditText) view.findViewById(R.id.dialog_election_id);
                        final EditText questionEditText =
                                (EditText) view.findViewById(R.id.dialog_question);
                        final EditText urlEditText =
                                (EditText) view.findViewById(R.id.dialog_abb_url);

                        final String electionId = electionIdEditText.getText().toString();
                        final String question = questionEditText.getText().toString();
                        final String url = urlEditText.getText().toString();

                        final Calendar cal = Calendar.getInstance();

                        cal.set(Calendar.MILLISECOND, 0);

                        cal.set(startDatePicker.getYear(), startDatePicker.getMonth(),
                                startDatePicker.getDayOfMonth(), startTimePicker.getCurrentHour(),
                                startTimePicker.getCurrentMinute(), 0);
                        final long startTime = cal.getTimeInMillis();

                        cal.set(endDatePicker.getYear(), endDatePicker.getMonth(),
                                endDatePicker.getDayOfMonth(), endTimePicker.getCurrentHour(),
                                endTimePicker.getCurrentMinute(), 0);
                        final long endTime = cal.getTimeInMillis();

                        TrusteeServiceHelper.getInstance().createElection(
                                getContext(), electionId, question, startTime, endTime,
                                (url.endsWith("/") ? url : url + "/"));
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
    public static class EraseAllDialogFragment extends DialogFragment {
        public static EraseAllDialogFragment newInstance() {
            return new EraseAllDialogFragment();
        }

        @Override
        @NonNull
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.erase_all_title)
                .setMessage(R.string.erase_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        TrusteeServiceHelper.getInstance().eraseAllElections(getContext());
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

    public void openNewElectionDialog() {
        NewElectionDialogFragment fragment = NewElectionDialogFragment.newInstance();
        fragment.show(getSupportFragmentManager(), NewElectionDialogFragment.class.getName());
    }

    public void openEraseAllDialog() {
        EraseAllDialogFragment fragment = EraseAllDialogFragment.newInstance();
        fragment.show(getSupportFragmentManager(), EraseAllDialogFragment.class.getName());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflates the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_erase_all:
                openEraseAllDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /*
     * Note that we should not call close() on the Cursor from our AsyncTaskLoader ourselves.
     * In order to remove the reference to the Cursor in our CursorAdapter, we use the
     * CursorAdapter#swapCursor(android.database.Cursor) method so that the old Cursor
     * is not closed. (CursorAdapter#changeCursor(android.database.Cursor) closes it.)
     * http://developer.android.com/reference/android/support/v4/app/LoaderManager.LoaderCallbacks.html
     */

    @Override
    public Loader<Cursor> onCreateLoader(int loaderID, Bundle args) {
        switch (loaderID) {
            case ELECTIONS_LOADER_ID:
                // Return a simple cursor loader that retrieves all the elections.
                return new SimpleCursorLoader(this, dbHelper, new SimpleCursorLoader.Query() {
                    @Override
                    public Cursor make(ReadableDataStore store) throws StoreException {
                        return store.getAllElections(fromColumns);
                    }
                });
            default:
                Utils.wtf(TAG, "Invalid loader id");
                return null;
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        // Database error.
        if (cursor == null) {
            ContextUtils.showToast(this, R.string.toast_db_load_error);
            return;
        }
        // The Loader will close the old cursor once it knows the application is no longer using it.
        listAdapter.swapCursor(cursor);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        // Clears out the adapter's reference to the Cursor. This prevents memory leaks.
        listAdapter.swapCursor(null);
    }


    @UiThread
    private final class ElectionsStatusReceiver extends ServiceStatusReceiver {
        private void restartLoader() {
            // Restart the loader to refresh the list view in our activity.
            getSupportLoaderManager().restartLoader(ELECTIONS_LOADER_ID, null, MainActivity.this);
        }

        private void showProgress(String electionId, int progress) {
            if (BuildConfig.DEBUG) Assert.assertNotSame(progress, -1);
            electionProgressBars.get(electionId).setProgress(progress);
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
                // Successes
                case TrusteeService.STATUS_CREATED_ELECTION:
                case TrusteeService.STATUS_ERASED_ALL_ELECTIONS:
                case TrusteeService.STATUS_CANCELLED_OPERATION:
                case TrusteeService.STATUS_INITIALIZING_ELECTION:
                case TrusteeService.STATUS_VERIFYING_ELECTION:
                case TrusteeService.STATUS_ERASING_ELECTION:
                    restartLoader();
                    break;
                case TrusteeService.STATUS_INITIALIZING_ELECTION_PROGRESS:
                    // Indeterminate progress mode. Nothing to update here.
                    break;
                case TrusteeService.STATUS_INITIALIZED_ELECTION:
                    showToast(electionId, R.string.toast_service_election_initialized);
                    restartLoader();
                    break;
                case TrusteeService.STATUS_VERIFYING_ELECTION_PROGRESS:
                    showProgress(electionId, intent.getIntExtra(TrusteeService.EXTRA_BALLOT_PROGRESS, -1));
                    break;
                case TrusteeService.STATUS_COMPLETED_ELECTION:
                    showToast(electionId, R.string.toast_service_election_completed);
                    restartLoader();
                    break;
                case TrusteeService.STATUS_ERASED_ELECTION:
                    showToast(electionId, R.string.toast_service_election_erased);
                    restartLoader();
                    break;
                // Failures
                case TrusteeService.STATUS_ELECTION_EXISTS:
                    showToast(electionId, R.string.toast_service_election_exists);
                    break;
                // Delegate handling to superclass.
                default:
                    restartLoader();
                    super.onReceive(context, intent);
                    break;
            }
        }
    }

}
