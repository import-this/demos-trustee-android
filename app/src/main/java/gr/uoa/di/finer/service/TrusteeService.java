package gr.uoa.di.finer.service;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.ResultReceiver;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.WorkerThread;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import junit.framework.Assert;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import gr.uoa.di.finer.BuildConfig;
import gr.uoa.di.finer.ContextUtils;
import gr.uoa.di.finer.ElectionStatus;
import gr.uoa.di.finer.R;
import gr.uoa.di.finer.URLConnection;
import gr.uoa.di.finer.Utils;
import gr.uoa.di.finer.crypto.JNICryptosystem;
import gr.uoa.di.finer.database.TrusteeOpenHelper;
import gr.uoa.di.finer.database.WritableDatabaseHelper;
import gr.uoa.di.finer.net.HTTPRequestSender;
import gr.uoa.di.finer.parse.SimpleResponseParser;
import gr.uoa.di.finer.parse.protobuf.InitDataProtoParser;

import static gr.uoa.di.finer.service.TrusteeServiceHelper.EXTRA_RESULT_RECEIVER;

/**
 * A {@link Service} subclass for handling asynchronous task requests on separate worker threads.
 * <p>
 * The service is started as needed, handles each request in turn using worker threads, and stops
 * itself when it runs out of work.
 * <p>
 * All requests are handled on worker threads and, thus, will not block the application's main loop.
 * <p>
 *
 * Tasks are divided in two categories:
 * - Light tasks:
 *      These are tasks that finish relatively quickly, namely:
 *          - Creating an election.
 *          - Cancelling an operation.
 * - Heavy tasks:
 *      These are tasks that take a lot of time to complete, namely:
 *          - Initializing an election.
 *          - Verifying an election.
 *          - Deleting an election or all elections.
 *              (These can be slower than expected due to deletion of all ballots on disk.)
 *      Heavy tasks run on their own worker threads, so as not to prevent light tasks from running.
 *
 * <p>
 * Currently, light tasks run sequentially with other light tasks and heavy tasks run sequentially
 * with other heavy tasks (i.e. one worker thread for each category). This is preferred, since:
 *      1. Initialization and verification are CPU-intensive.
 *      2. Initialization and verification may consume a lot of battery power.
 *      3. Verification may consume a lot of bandwidth.
 *      4. The bigger the election, the worse.
 * The above restrictions may change in the future as devices become more capable.
 * <p>
 * This service also provides the ability to cancel intents (either the ones that have not yet
 * been processed or the ones that are currently being processed).
 * <p>
 * See <a href="http://developer.android.com/guide/components/services.html#ExtendingService">
 *          Extending the Service class
 *     </a>
 * <p>
 * As this service runs, it broadcasts its status using LocalBroadcastManager. Any component that
 * wants to see the status should implement a subclass of BroadcastReceiver and register to
 * receive broadcast Intents with action {@link TrusteeService#ACTION_BROADCAST_STATUS}.
 *
 * @author Vasilis Poulimenos
 */
public class TrusteeService extends Service {

    private static final String TAG = TrusteeService.class.getName();

    // The actions that this service can perform.
    static final String ACTION_CREATE_ELECTION = "gr.uoa.di.finer.action.CREATE_ELECTION";
    static final String ACTION_INITIALIZE_ELECTION = "gr.uoa.di.finer.action.INITIALIZE_ELECTION";
    static final String ACTION_VERIFY_ELECTION = "gr.uoa.di.finer.action.VERIFY_ELECTION";
    static final String ACTION_ERASE_ELECTION = "gr.uoa.di.finer.action.ERASE_ELECTION";
    static final String ACTION_ERASE_ALL_ELECTIONS = "gr.uoa.di.finer.action.ERASE_ALL_ELECTIONS";
    static final String ACTION_CANCEL_OPERATION = "gr.uoa.di.finer.action.CANCEL_OPERATION";

    // Extended data for the above actions.
    static final String EXTRA_QUESTION = "gr.uoa.di.finer.extra.QUESTION";
    static final String EXTRA_START_TIME = "gr.uoa.di.finer.extra.START_TIME";
    static final String EXTRA_END_TIME = "gr.uoa.di.finer.extra.END_TIME";
    static final String EXTRA_ABB_URL = "gr.uoa.di.finer.extra.ABB_URL";
    static final String EXTRA_CANCELLED_TASK = "gr.uoa.di.finer.extra.CANCELLED_TASK";


    // Broadcast actions.
    public static final String ACTION_BROADCAST_STATUS = "gr.uoa.di.finer.action.BROADCAST_STATUS";

    // Extended data for the above broadcast actions.
    public static final String EXTRA_REQUEST = "gr.uoa.di.finer.extra.REQUEST";
    public static final String EXTRA_ELECTION_ID = "gr.uoa.di.finer.extra.ELECTION_ID";
    public static final String EXTRA_REQUEST_STATUS = "gr.uoa.di.finer.extra.REQUEST_STATUS";
    public static final String EXTRA_BALLOT_PROGRESS = "gr.uoa.di.finer.extra.BALLOT_PROGRESS";

    // Broadcast status codes.
    public static final int STATUS_CREATED_ELECTION = 0;
    public static final int STATUS_INITIALIZING_ELECTION = 10;
    public static final int STATUS_INITIALIZING_ELECTION_PROGRESS = 20;
    public static final int STATUS_INITIALIZED_ELECTION = 30;
    public static final int STATUS_VERIFYING_ELECTION = 40;
    public static final int STATUS_VERIFYING_ELECTION_PROGRESS = 50;
    public static final int STATUS_VERIFIED_ELECTION = 60;
    public static final int STATUS_COMPLETED_ELECTION = 70;
    public static final int STATUS_ERASING_ELECTION = 80;
    public static final int STATUS_ERASED_ELECTION = 90;
    public static final int STATUS_ERASED_ALL_ELECTIONS = 100;
    public static final int STATUS_CANCELLED_OPERATION = 110;

    public static final int STATUS_ELECTION_EXISTS = 120;
    public static final int STATUS_FILE_NOT_FOUND = 130;
    public static final int STATUS_IO_ERROR = 140;
    public static final int STATUS_PARSE_ERROR = 150;
    public static final int STATUS_STORAGE_ERROR = 160;
    public static final int STATUS_STORAGE_FULL_ERROR = 170;
    public static final int STATUS_INVALID_URL = 180;
    public static final int STATUS_SOCKET_TIMEOUT = 190;


    /**
     * Boolean indicating whether the service should display notifications to the user.
     */
    private static volatile boolean showNotifications = true;

    /**
     * Tells the service to display notifications.
     */
    public static void showNotifications() {
        showNotifications = true;
    }

    /**
     * Tells the service not to display notifications.
     * An activity may choose to disable notifications so as to display its own message to the user.
     * Remember to re-enable notifications later (perhaps when the activity goes to the background).
     */
    public static void hideNotifications() {
        showNotifications = false;
    }


    /*
     * Implementation notes:
     *  - There are currently three worker threads active or waiting at any given point in time:
     *      - The main thread sends messages to a HandlerThread that is started in service creation.
     *      - The HandlerThread (there is only one) handles request dispatch to the thread pools.
     *      - There are two independent thread pools with only one thread each (currently).
     *  - After handling a request, a thread pool checks if there are any more requests awaiting
     *    processing and if not, it tries to stop the service. If the service will actually be
     *    stopped or not depends on whether a new request has arrived in the meantime.
     */

    private static final int INITIAL_MAP_CAPACITY = 4;

    /**
     * A map of all cancellable requests and their corresponding (pending) results (whether
     * waiting or currently being processed). These are kept in order to perform cancellation.
     */
    private final Map<String, Intent> requests =
            Collections.synchronizedMap(new HashMap<String, Intent>(INITIAL_MAP_CAPACITY));

    /**
     * A map of all the intents and their corresponding (pending) results (whether waiting
     * or currently being processed). These are kept in order to enable cancellable tasks.
     * Making this a WeakHashMap will automatically remove futures from processed intents.
     */
    private final Map<Intent, Future> requestResults =
            Collections.synchronizedMap(new WeakHashMap<Intent, Future>(INITIAL_MAP_CAPACITY));

    /**
     * A queue of all the service start IDs. Since we process IDs out of order,
     * this is used for stopping them in the same order we received them.
     */
    private final Queue<Integer> startIds = new LinkedBlockingQueue<>();

    /**
     * Tries to stop the service.
     * This may not actually stop it, since a new request may have arrived in the meantime.
     */
    private void tryStopSelf() {
        stopSelf(startIds.remove());
    }

    /*
     * *CRITICAL NOTE*
     * JNICryptosystem is currently implemented as a singleton (because the C++ object is also a
     * singleton), so the system works because there is only one thread in a pool (thus, one task).
     * DO NOT INCREASE the pool size before fixing JNICryptosystem.
     */
    private static final int CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = CORE_POOL_SIZE;
    static {
        // Make it foolproof.
        if (MAX_POOL_SIZE > 1) {
            throw new AssertionError("MAX_POOL_SIZE > 1 with singleton JNICryptosystem");
        }
    }

    /**
     * A factory that creates threads with background priority.
     * Based on Executors.DefaultThreadFactory.
     */
    private static final class BackgroundThreadFactory implements ThreadFactory {
        private static final AtomicInteger poolNumber = new AtomicInteger(1);
        private final ThreadGroup group;
        private final AtomicInteger threadNumber = new AtomicInteger(1);
        private final String namePrefix;

        BackgroundThreadFactory() {
            SecurityManager s = System.getSecurityManager();
            group = (s != null) ? s.getThreadGroup() : Thread.currentThread().getThreadGroup();
            namePrefix = "pool-" + poolNumber.getAndIncrement() + "-thread-";
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(group, r, namePrefix + threadNumber.getAndIncrement(), 0);
            if (t.isDaemon()) {
                t.setDaemon(false);
            }
            // There is not a clear correspondence between Process.THREAD_PRIORITY_BACKGROUND and
            // priority constants defined in Thread. So, just set it to half the normal priority.
            t.setPriority((int) Math.ceil((double) Thread.NORM_PRIORITY / 2));
            return t;
        }
    }

    /**
     * A thread pool executor specialized for the needs of this service.
     * This pool currently contains exactly one thread at all times.
     */
    @WorkerThread
    private final class ServiceThreadPoolExecutor extends ThreadPoolExecutor {
        ServiceThreadPoolExecutor() {
            super(CORE_POOL_SIZE, MAX_POOL_SIZE, 0L, TimeUnit.MILLISECONDS,
                  new LinkedBlockingQueue<Runnable>(), new BackgroundThreadFactory());
        }

        @Override
        protected void afterExecute(Runnable runnable, Throwable throwable) {
            super.afterExecute(runnable, throwable);
            tryStopSelf();
        }
    }


    /**
     * A handler for this service.
     * This is used to handle initial processing of incoming intents in a separate thread.
     */
    @SuppressLint("HandlerLeak")
    private final class ServiceHandler extends Handler {
        ServiceHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            handleIntent(msg.arg1, (Intent) msg.obj);
        }

        private void broadcastStatus(Intent request, int statusCode) {
            Intent localIntent = new Intent(ACTION_BROADCAST_STATUS)
                    // The election ID may be null.
                    .putExtra(EXTRA_ELECTION_ID, request.getStringExtra(EXTRA_ELECTION_ID))
                    .putExtra(EXTRA_REQUEST, request)
                    .putExtra(EXTRA_REQUEST_STATUS, statusCode);
            localBroadcastManager.sendBroadcast(localIntent);
        }

        private void displayNotification(Notification notification) {
            if (TrusteeService.showNotifications) {
                notificationManager.notify(NotificationFactory.getNotificationId(), notification);
            }
        }

        @WorkerThread
        private void handleIntent(int startId, Intent intent) {
            final SQLiteDatabase db;
            final Future<?> result;

            try {
                // Lazy-load the database. Loading it here allows for graceful exception handling.
                db = dbHelper.getWritableDatabase();
            } catch (SQLiteException e) {
                Log.e(TAG, "Failed to open writable database", e);
                broadcastStatus(intent, STATUS_STORAGE_ERROR);
                displayNotification(notificationFactory.newErrorNotification(
                        R.string.notification_generic_error, R.string.notification_storage_error));
                return;
            }

            if (BuildConfig.DEBUG) Log.d(TAG, "Handling intent...");
            startIds.add(startId);
            switch (intent.getAction()) {
                case ACTION_CREATE_ELECTION:
                    result = lightTaskThreadPool.submit(new CreateElectionTask(intent, db));
                    requestResults.put(intent, result);
                    break;
                case ACTION_INITIALIZE_ELECTION:
                    result = heavyTaskThreadPool.submit(new InitializeElectionTask(intent, db));
                    requests.put(intent.getStringExtra(EXTRA_ELECTION_ID), intent);
                    requestResults.put(intent, result);
                    break;
                case ACTION_VERIFY_ELECTION:
                    result = heavyTaskThreadPool.submit(new VerifyElectionTask(intent, db));
                    requests.put(intent.getStringExtra(EXTRA_ELECTION_ID), intent);
                    requestResults.put(intent, result);
                    break;
                case ACTION_ERASE_ELECTION:
                    result = heavyTaskThreadPool.submit(new EraseElectionTask(intent, db));
                    requestResults.put(intent, result);
                    break;
                case ACTION_ERASE_ALL_ELECTIONS:
                    result = heavyTaskThreadPool.submit(new EraseAllElectionsTask(intent, db));
                    requestResults.put(intent, result);
                    break;
                case ACTION_CANCEL_OPERATION:
                    Intent oldRequest = requests.remove(intent.getStringExtra(EXTRA_ELECTION_ID));
                    // The user may cancel the request many times in quick succession.
                    if (oldRequest != null) {
                        if (BuildConfig.DEBUG) Log.d(TAG, "Cancelling " + oldRequest);
                        requestResults.remove(oldRequest).cancel(true);
                        // Even though the actual cancellation may not have been performed yet if
                        // the task was currently running, tell the UI that it has. We will handle
                        // it in the background.
                        broadcastStatus(oldRequest, STATUS_CANCELLED_OPERATION);
                    }
                    tryStopSelf();
                    break;
                default:
                    Utils.wtf(TAG, "Unknown trustee service action");
                    break;
            }
            if (BuildConfig.DEBUG) Log.d(TAG, "Handled intent");
        }
    }


    private Looper serviceLooper;
    private Handler serviceHandler;

    private TrusteeOpenHelper dbHelper;

    private NotificationFactory notificationFactory;
    private NotificationManager notificationManager;
    private LocalBroadcastManager localBroadcastManager;

    private ServiceThreadPoolExecutor lightTaskThreadPool;
    private ServiceThreadPoolExecutor heavyTaskThreadPool;

    /**
     * Constructs a TrusteeService.
     */
    public TrusteeService() {
        super();
    }

    @Override
    @CallSuper
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread(
                TrusteeService.class.getName(),
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);

        dbHelper = new TrusteeOpenHelper(this);

        notificationFactory = new NotificationFactory(this);
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        lightTaskThreadPool = new ServiceThreadPoolExecutor();
        heavyTaskThreadPool = new ServiceThreadPoolExecutor();

        if (BuildConfig.DEBUG) {
            Log.d(TAG, TrusteeService.class.getSimpleName() + " created");
        }
    }

    @Override
    @CallSuper
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (BuildConfig.DEBUG) Assert.assertNotNull(intent);        // START_REDELIVER_INTENT

        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        serviceHandler.sendMessage(msg);

        // If this process dies before the service stops itself, restart it.
        return START_REDELIVER_INTENT;
    }

    @Override
    @CallSuper
    public void onDestroy() {
        serviceLooper.quit();
        lightTaskThreadPool.shutdownNow();
        heavyTaskThreadPool.shutdownNow();
        dbHelper.close();
        if (BuildConfig.DEBUG) {
            Assert.assertTrue("Thread pool is not empty", lightTaskThreadPool.getQueue().isEmpty());
            Assert.assertTrue("Thread pool is not empty", heavyTaskThreadPool.getQueue().isEmpty());
            Log.d(TAG, TrusteeService.class.getSimpleName() + " destroyed");
        }
    }

    /**
     * We don't provide binding, so this method simply returns null.
     * @see android.app.Service#onBind
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**********************************************************************************************/

    /*
     * If a service is stopped while it's still running in the foreground, then the ongoing
     * notification associated with it is also removed. However, since this service may not
     * stop after the current intent is processed, (because there may be additional intents
     * to process), make sure to remove the service from the foreground state.
     */

    @WorkerThread
    private abstract class Task implements Runnable {
        /**
         * The request that started this task.
         */
        protected final Intent request;
        /**
         * Subclasses should not close the data store, as this is handled by the superclass.
         */
        protected final WritableDataStore store;
        /**
         * Intent reused for local broadcasts.
         * Subclasses can customize this to meet their needs.
         */
        protected final Intent localIntent;
        /**
         * The ID used for displaying notifications.
         */
        protected final int notificationId;

        protected Task(Intent request, SQLiteDatabase db) {
            this.request = request;
            this.store = new WritableDatabaseHelper(db);
            this.notificationId = NotificationFactory.getNotificationId();
            this.localIntent = new Intent(ACTION_BROADCAST_STATUS)
                    .putExtra(EXTRA_REQUEST, request);
        }

        protected void broadcastStatus(int statusCode) {
            localIntent.putExtra(EXTRA_REQUEST_STATUS, statusCode);
            localBroadcastManager.sendBroadcast(localIntent);
        }

        protected void displayNotification(Notification notification) {
            if (TrusteeService.showNotifications) {
                notificationManager.notify(notificationId, notification);
            }
        }

        /**
         * Subclasses must override this method to provide the logic for their task.
         */
        protected abstract void performTask();

        /**
         * Executes the task that this class defines and performs cleanup.
         */
        @Override
        public final void run() {
            try {
                performTask();      // The actual work is done here.
            } finally {
                store.close();
            }
        }
    }

    @WorkerThread
    private abstract class ElectionTask extends Task {
        protected final String electionId;

        ElectionTask(Intent request, SQLiteDatabase db) {
            super(request, db);
            this.electionId = request.getStringExtra(EXTRA_ELECTION_ID);
            this.localIntent.putExtra(EXTRA_ELECTION_ID, this.electionId);
            if (BuildConfig.DEBUG) {
                Assert.assertNotNull(this.electionId);
            }
        }

        /**
         * Sends the result to the service helper.
         * Subclasses are responsible for deciding if and when to call this.
         */
        protected void notifyServiceHelper() {
            Bundle bundle = new Bundle();
            bundle.putString(EXTRA_ELECTION_ID, electionId);
            ((ResultReceiver) request.getParcelableExtra(EXTRA_RESULT_RECEIVER)).send(0, bundle);
        }
    }

    /**
     * An election task that sends progress reports and can be cancelled.
     */
    @WorkerThread
    private abstract class HeavyElectionTask extends ElectionTask {
        protected static final long COUNT_INTERVAL = 100;

        /**
         * The intent that will be used to broadcast progress.
         * Subclasses can customize this to meet their needs.
         */
        protected final Intent progressIntent;
        /**
         * The ID used for displaying ongoing notifications.
         * It is important that this is different from the ID for normal notifications.
         */
        protected final int ongoingNotificationId;

        protected HeavyElectionTask(Intent request, SQLiteDatabase db) {
            super(request, db);
            this.ongoingNotificationId = NotificationFactory.getNotificationId();
            this.progressIntent = new Intent(ACTION_BROADCAST_STATUS)
                    .putExtra(EXTRA_ELECTION_ID, this.electionId);
            if (BuildConfig.DEBUG) {
                Assert.assertNotSame("Same IDs", this.notificationId, this.ongoingNotificationId);
            }
        }

        protected void reportProgress(long progress, Notification notification) {
            progressIntent.putExtra(EXTRA_BALLOT_PROGRESS, progress);
            localBroadcastManager.sendBroadcast(progressIntent);
            notificationManager.notify(ongoingNotificationId, notification);
        }

        protected void reportProgress(int progress, Notification notification) {
            progressIntent.putExtra(EXTRA_BALLOT_PROGRESS, progress);
            localBroadcastManager.sendBroadcast(progressIntent);
            notificationManager.notify(ongoingNotificationId, notification);
        }

        protected abstract void performHeavyTask() throws InterruptedException;

        @Override
        protected final void performTask() {
            try {
                long t1 = System.currentTimeMillis();
                performHeavyTask();
                long t2 = System.currentTimeMillis();
                Log.i(TAG, "asdfasdfasdfasdfdsfadsfadsfasdfdf time: " + (t2 - t1));
            } catch (InterruptedException ignored) {
                // Nothing to report.
            } finally {
                // The task has finished, so cancellation does not make sense any more.
                requests.remove(electionId);
            }
        }
    }


    @WorkerThread
    private final class CreateElectionTask extends ElectionTask {
        CreateElectionTask(Intent request, SQLiteDatabase db) {
            super(request, db);
        }

        @Override
        protected void performTask() {
            final String question = request.getStringExtra(EXTRA_QUESTION);
            final long startTime = request.getLongExtra(EXTRA_START_TIME, -1);
            final long endTime = request.getLongExtra(EXTRA_END_TIME, -1);
            final String url = request.getStringExtra(EXTRA_ABB_URL);

            if (BuildConfig.DEBUG) {
                // Extended data are always set by the helper.
                Assert.assertNotSame(startTime, -1);
                Assert.assertNotSame(endTime, -1);
            }

            try {
                // If the election already exists, report it and stop.
                if (store.hasElection(electionId)) {
                    broadcastStatus(STATUS_ELECTION_EXISTS);
                    // TODO: Show notification?
                    return;
                }
                store.createElection(electionId, question, startTime, endTime, url,
                                     ElectionStatus.UNINITIALIZED.getValue());
                broadcastStatus(STATUS_CREATED_ELECTION);
            } catch (StoreException e) {
                Log.e(TAG, "Election creation failed", e);
                broadcastStatus(STATUS_STORAGE_ERROR);
                displayNotification(notificationFactory.newErrorNotification(
                        R.string.notification_create_error,
                        R.string.notification_storage_error, electionId));
            }
        }
    }

    @WorkerThread
    private final class InitializeElectionTask extends HeavyElectionTask {
        private static final String TAG = "InitializeElectionTask";
        private static final String LOG_ERROR_MSG = "Election initialization failed";

        InitializeElectionTask(Intent request, SQLiteDatabase db) {
            super(request, db);
            this.progressIntent.putExtra(EXTRA_REQUEST_STATUS, STATUS_INITIALIZING_ELECTION_PROGRESS);
        }

        private void reportCount(long count) {
            reportProgress(count, notificationFactory.newOngoingInitNotification(electionId, count));
        }

        /**
         *
         * The election should already be created.
         *
         * @param dataStream the input stream that will provide the initialization data
         * @throws IOException
         * @throws ParseException
         * @throws StoreException
         * @throws StoreFullException
         * @throws InterruptedException
         */
        private void parseData(InputStream dataStream)
                throws StoreException, ParseException, IOException, InterruptedException {
            final InitDataParser parser = new InitDataProtoParser(dataStream, store, electionId);
            final ElectionStatus status = ElectionStatus.valueOf(store.getElectionStatus(electionId));

            // In case of a previous failed attempt
            if (status == ElectionStatus.INITIALIZING) {
                Log.i(TAG, "Recovering from initialization error...");
                // Discard the ballots that were already inserted.
                store.eraseBallots(electionId);
                Log.i(TAG, "Restarting initialization...");
            } else if (status != ElectionStatus.UNINITIALIZED) {
                String msg = "Election is not in " + ElectionStatus.UNINITIALIZED + " state";
                Log.e(TAG, msg);
                throw new IllegalStateException(msg);
            }
            store.setElectionStatus(electionId, ElectionStatus.INITIALIZING.getValue());
            broadcastStatus(STATUS_INITIALIZING_ELECTION);

            parser.parseKey();

            // Break parsing into small database transactions to increase performance.
            // One large transaction is unlikely due to disk space needs and locking.
            for (;;) {
                int i;

                if (Thread.interrupted()) {
                    throw new InterruptedException("Election initialization interrupted");
                }
                store.beginTransaction();
                try {
                    for (i = 0; i < COUNT_INTERVAL && parser.parseBallot(); ++i) {}
                    store.setTransactionSuccessful();
                } finally {
                    store.endTransaction();
                }
                if (i < COUNT_INTERVAL) break;
                // Send an update for every COUNT_INTERVAL ballots parsed.
                reportCount(parser.getParsedBallotCount());
            }

            store.setElectionStatus(electionId, ElectionStatus.INITIALIZED.getValue());
            displayNotification(notificationFactory.newInitNotification(
                    electionId, parser.getParsedBallotCount()));
        }

        private void reportError(String electionId, int errorCode, @StringRes int errorMsg) {
            broadcastStatus(errorCode);
            displayNotification(notificationFactory.newErrorNotification(
                    R.string.notification_initialize_error, errorMsg, electionId));
        }

        @Override
        protected void performHeavyTask() throws InterruptedException {
            // The election initialization is a potentially time-consuming operation that the user
            // is aware of. So, make this a foreground service to prevent Android from killing it.
            startForeground(ongoingNotificationId, notificationFactory.newOngoingInitNotification(electionId));

            InputStream stream = null;
            boolean successful = false;
            try {
                stream = ContextUtils.openInputFile(TrusteeService.this, request.getData());
                parseData(stream);
                successful = true;
            } catch (StoreFullException e) {
                Log.e(TAG, LOG_ERROR_MSG, e);
                reportError(electionId, STATUS_STORAGE_FULL_ERROR, R.string.notification_storage_full_error);
            } catch (StoreException e) {
                Log.e(TAG, LOG_ERROR_MSG, e);
                reportError(electionId, STATUS_STORAGE_ERROR, R.string.notification_storage_error);
            } catch (ParseException e) {
                Log.e(TAG, LOG_ERROR_MSG, e);
                reportError(electionId, STATUS_PARSE_ERROR, R.string.notification_parse_error);
            } catch (FileNotFoundException e) {
                Log.e(TAG, LOG_ERROR_MSG, e);
                reportError(electionId, STATUS_FILE_NOT_FOUND, R.string.notification_file_not_found);
            } catch (IOException e) {
                Log.e(TAG, LOG_ERROR_MSG, e);
                reportError(electionId, STATUS_IO_ERROR, R.string.notification_io_error);
            } finally {
                notifyServiceHelper();
                // Notify observers last to prevent race conditions.
                if (successful) {
                    broadcastStatus(STATUS_INITIALIZED_ELECTION);
                }
                stopForeground(true);
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing initialization data file", e);
                    }
                }
            }
        }
    };

    @WorkerThread
    private final class VerifyElectionTask extends HeavyElectionTask {
        private static final String TAG = "VerifyElectionTask";
        private static final String LOG_ERROR_MSG = "Election verification failed";

        // TODO: Decide on server API.
        private static final String URL_ENCODING = "UTF-8";
        private static final String TOTAL_BALLOTS_URL_SUFFIX = "gettotal/?%s";
        private static final String RESPONSE_URL_SUFFIX = "?id=%s&start=%s&stop=%s";
        private static final String RESULT_URL_SUFFIX = "post/?%s";

        VerifyElectionTask(Intent request, SQLiteDatabase db) {
            super(request, db);
            this.progressIntent.putExtra(EXTRA_REQUEST_STATUS, STATUS_VERIFYING_ELECTION_PROGRESS);
        }


        private String makeTotalVotedBallotsUrl(String baseAbbUrl)
                throws UnsupportedEncodingException {
            return baseAbbUrl + String.format(TOTAL_BALLOTS_URL_SUFFIX,
                    URLEncoder.encode(electionId, URL_ENCODING));
        }

        private String makeAbbResponseUrl(String baseAbbUrl, long start, long stop)
                throws UnsupportedEncodingException {
            return baseAbbUrl + String.format(RESPONSE_URL_SUFFIX,
                    URLEncoder.encode(electionId, URL_ENCODING),
                    URLEncoder.encode(Long.toString(start), URL_ENCODING),
                    URLEncoder.encode(Long.toString(stop), URL_ENCODING));
        }

        private String makeTrusteeResultUrl(String baseAbbUrl)
                throws UnsupportedEncodingException {
            return baseAbbUrl + String.format(RESULT_URL_SUFFIX,
                    URLEncoder.encode(electionId, URL_ENCODING));
        }


        private void reportCount(long count, long total) {
            reportProgress(
                    Utils.toPercentage(count, total),
                    notificationFactory.newOngoingVerNotification(electionId, count, total));
        }

        /**
         *
         * @return
         * @throws MalformedURLException
         * @throws IOException
         */
        private long getTotalBallotCount(String baseAbbUrl) throws IOException {
            URLConnection connection = null;
            BufferedReader reader = null;
            try {
                connection = new HTTPRequestSender(makeTotalVotedBallotsUrl(baseAbbUrl)).sendGetRequest();
                reader = new BufferedReader(connection.getInputStreamReader());
                return Long.parseLong(reader.readLine());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing ballot count connection input stream reader", e);
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        /**
         *
         * @param decommitment the decommitment bundle
         * @throws MalformedURLException
         * @throws SocketTimeoutException
         * @throws IOException
         * @throws StoreException
         */
        private void postResult(String baseAbbUrl, String decommitment)
                throws IOException, StoreException {
            new HTTPRequestSender(makeTrusteeResultUrl(baseAbbUrl)).postResult(decommitment);
        }

        /**
         *
         * @throws MalformedURLException
         * @throws SocketTimeoutException
         * @throws IOException
         * @throws ParseException
         * @throws StoreException
         * @throws InterruptedException
         */
        @WorkerThread
        private void parseResponse()
                throws IOException, ParseException, StoreException, InterruptedException {
            URLConnection connection = null;
            BufferedReader reader = null;
            try {
                final ElectionStatus status = ElectionStatus.valueOf(store.getElectionStatus(electionId));

                // In case of a previous failed attempt
                if (status == ElectionStatus.VERIFYING) {
                    Log.i(TAG, "Recovering from verification error...");
                    // There is nothing to fix here.
                    Log.i(TAG, "Restarting verification...");
                } else if (status != ElectionStatus.INITIALIZED) {
                    String msg = "Election is not in " + ElectionStatus.INITIALIZED + " state";
                    Log.e(TAG, msg);
                    throw new IllegalStateException(msg);
                }
                store.setElectionStatus(electionId, ElectionStatus.VERIFYING.getValue());
                broadcastStatus(STATUS_VERIFYING_ELECTION);

                final String baseAbbUrl = store.getElectionAbb(electionId);
                final long totalCount = getTotalBallotCount(baseAbbUrl);
                final Cryptosystem cryptosystem = new JNICryptosystem(
                        store.getElectionDecommitmentKey(electionId));
                final String url = makeAbbResponseUrl(baseAbbUrl, 0, totalCount);
                final ResponseParser parser;

                connection = new HTTPRequestSender(url).sendGetRequest();
                reader = new BufferedReader(connection.getInputStreamReader());
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Reading response from " + url);
                }
                parser = new SimpleResponseParser(reader, cryptosystem, store, electionId);

                // Break parsing into small database transactions to increase performance.
                // One large transaction is unlikely due to disk space needs and locking.
                for (;;) {
                    int i;

                    if (Thread.interrupted()) {
                        throw new InterruptedException("Election verification interrupted");
                    }
                    store.beginTransaction();
                    try {
                        for (i = 0; i < COUNT_INTERVAL && parser.parse(); ++i) {}
                        store.setTransactionSuccessful();
                    } finally {
                        store.endTransaction();
                    }
                    if (i < COUNT_INTERVAL) break;
                    // Send an update for every COUNT_INTERVAL ballots parsed.
                    reportCount(parser.getParsedBallotCount(), totalCount);
                }

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Parsed ballots: " + Long.toString(parser.getParsedBallotCount()));
                }

                final String decommitmentBundle = cryptosystem.computeBundle();
                store.saveDecommitmentBundle(electionId, decommitmentBundle);
                //store.setElectionStatus(electionId, ElectionStatus.VERIFIED.getValue());

                broadcastStatus(STATUS_VERIFIED_ELECTION);
                displayNotification(notificationFactory.newVerNotification(
                        electionId, parser.getParsedBallotCount()));

                postResult(baseAbbUrl, decommitmentBundle);
                store.setElectionStatus(electionId, ElectionStatus.COMPLETED.getValue());
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException e) {
                        Log.e(TAG, "Error closing ballot connection input stream reader", e);
                    }
                }
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }

        private void reportError(String electionId, int errorCode, @StringRes int errorMsg) {
            broadcastStatus(errorCode);
            displayNotification(notificationFactory.newErrorNotification(
                    R.string.notification_verify_error, errorMsg, electionId));
        }

        @Override
        protected void performHeavyTask() throws InterruptedException {
            // Verification happens after the election is ended and is probably a time-sensitive
            // operation. So, make this a foreground service to prevent Android from killing it.
            startForeground(ongoingNotificationId, notificationFactory.newOngoingVerNotification(electionId));

            boolean successful = false;
            try {
                parseResponse();
                successful = true;
            } catch (StoreFullException e) {
                Log.e(TAG, LOG_ERROR_MSG, e);
                reportError(electionId, STATUS_STORAGE_FULL_ERROR, R.string.notification_storage_full_error);
            } catch (StoreException e) {
                Log.e(TAG, LOG_ERROR_MSG, e);
                reportError(electionId, STATUS_STORAGE_ERROR, R.string.notification_storage_error);
            } catch (ParseException e) {
                Log.e(TAG, LOG_ERROR_MSG, e);
                reportError(electionId, STATUS_PARSE_ERROR, R.string.notification_parse_error);
            } catch (MalformedURLException e) {
                Log.e(TAG, LOG_ERROR_MSG, e);
                reportError(electionId, STATUS_INVALID_URL, R.string.notification_invalid_url);
            } catch (SocketTimeoutException e) {
                Log.e(TAG, "Election verification - " + e.toString());
                reportError(electionId, STATUS_SOCKET_TIMEOUT, R.string.notification_timeout);
                // TODO: Restart with exponential backoff?
            } catch (IOException e) {
                Log.e(TAG, LOG_ERROR_MSG, e);
                reportError(electionId, STATUS_IO_ERROR, R.string.notification_io_error);
            } finally {
                notifyServiceHelper();
                // Notify observers last to prevent race conditions.
                if (successful) {
                    broadcastStatus(STATUS_COMPLETED_ELECTION);
                }
                stopForeground(true);
            }
        }
    }

    @WorkerThread
    private final class EraseElectionTask extends ElectionTask {
        EraseElectionTask(Intent request, SQLiteDatabase db) {
            super(request, db);
        }

        @Override
        protected void performTask() {
            boolean successful = false;
            try {
                store.setElectionStatus(electionId, ElectionStatus.ERASING.getValue());
                broadcastStatus(STATUS_ERASING_ELECTION);
                store.eraseElection(electionId);
                // Note that the election status will never change again.
                successful = true;
            } catch (StoreException e) {
                Log.e(TAG, "Election erasure failed", e);
                broadcastStatus(STATUS_STORAGE_ERROR);
                displayNotification(notificationFactory.newErrorNotification(
                        R.string.notification_erase_error,
                        R.string.notification_storage_error, electionId));
            } finally {
                notifyServiceHelper();
                // Notify observers last to prevent race conditions.
                if (successful) {
                    broadcastStatus(STATUS_ERASED_ELECTION);
                }
            }
        }
    }

    @WorkerThread
    private final class EraseAllElectionsTask extends Task {
        EraseAllElectionsTask(Intent request, SQLiteDatabase db) {
            super(request, db);
        }

        @Override
        protected void performTask() {
            try {
                store.clear();
                broadcastStatus(STATUS_ERASED_ALL_ELECTIONS);
            } catch (StoreException e) {
                Log.e(TAG, "All elections erasure failed", e);
                broadcastStatus(STATUS_STORAGE_ERROR);
                displayNotification(notificationFactory.newErrorNotification(
                        R.string.notification_erase_all_error,
                        R.string.notification_storage_error));
            }
        }
    }

}
