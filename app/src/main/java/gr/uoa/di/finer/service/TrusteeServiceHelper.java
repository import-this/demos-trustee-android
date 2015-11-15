package gr.uoa.di.finer.service;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.ResultReceiver;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static gr.uoa.di.finer.service.TrusteeService.ACTION_CANCEL_OPERATION;
import static gr.uoa.di.finer.service.TrusteeService.ACTION_CREATE_ELECTION;
import static gr.uoa.di.finer.service.TrusteeService.ACTION_ERASE_ALL_ELECTIONS;
import static gr.uoa.di.finer.service.TrusteeService.ACTION_ERASE_ELECTION;
import static gr.uoa.di.finer.service.TrusteeService.ACTION_INITIALIZE_ELECTION;
import static gr.uoa.di.finer.service.TrusteeService.ACTION_VERIFY_ELECTION;
import static gr.uoa.di.finer.service.TrusteeService.EXTRA_ABB_URL;
import static gr.uoa.di.finer.service.TrusteeService.EXTRA_CANCELLED_TASK;
import static gr.uoa.di.finer.service.TrusteeService.EXTRA_ELECTION_ID;
import static gr.uoa.di.finer.service.TrusteeService.EXTRA_END_TIME;
import static gr.uoa.di.finer.service.TrusteeService.EXTRA_QUESTION;
import static gr.uoa.di.finer.service.TrusteeService.EXTRA_START_TIME;

/**
 * A singleton which exposes a simple asynchronous API to clients
 * with the actions that the {@link TrusteeService} can perform.
 *
 * @author Vasilis Poulimenos
 */
public final class TrusteeServiceHelper {

    static final String EXTRA_RESULT_RECEIVER = "gr.uoa.di.finer.extra.RESULT_RECEIVER";


    // Private constructor to prevent instantiation from other classes.
    private TrusteeServiceHelper() {}

    // https://en.wikipedia.org/wiki/Singleton_pattern#Initialization-on-demand_holder_idiom
    private static final class SingletonHolder {
        private static final TrusteeServiceHelper INSTANCE = new TrusteeServiceHelper();
    }

    public static TrusteeServiceHelper getInstance() {
        return SingletonHolder.INSTANCE;
    }


    private static final int INITIAL_CAPACITY = 4;

    /**
     * A map of all cancellable requests and their corresponding intents.
     * These are kept in order to prevent duplicate requests and perform cancellation.
     */
    private final Map<String, Intent> requests =
            Collections.synchronizedMap(new HashMap<String, Intent>(INITIAL_CAPACITY));


    /**
     * A result receiver for notifying the service helper of when an operation has finished.
     */
    class ServiceResultReceiver extends ResultReceiver {
        ServiceResultReceiver() {
            super(null);
        }

        /**
         * Removes the specified election ID from the current requests.
         *
         * @param resultCode Arbitrary result code delivered by the sender. This is ignored.
         * @param resultData A bundle containing the ID of the election that was processed.
         */
        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            requests.remove(resultData.getString(EXTRA_ELECTION_ID));
        }
    }

    private final ServiceResultReceiver resultReceiver = new ServiceResultReceiver();


    /**
     * Tells if the election specified has an associated pending request.
     *
     * @param electionId the ID of the election to check
     * @return true if the specified election has a pending operation
     */
    public boolean isPending(String electionId) {
        return requests.containsKey(electionId);
    }

    /**
     * Starts the Trustee service to create a new election.
     * The election will not be initialized. To initialize it, use {@link #initializeElection}.
     *
     * If the service is already performing a similar task, this action will be queued.
     *
     * @param context the context associated with this request
     * @param electionId the ID of the election to create
     * @param question the question posed for this election
     * @param startTime the time when the election starts as a timestamp
     * @param endTime the time when the elections ends as a timestamp
     * @param abbUrl the URL of the ABB server
     */
    public void createElection(Context context, String electionId, String question,
                               long startTime, long endTime, String abbUrl) {
        Intent intent = new Intent(context, TrusteeService.class)
                .setAction(ACTION_CREATE_ELECTION)
                .putExtra(EXTRA_ELECTION_ID, electionId)
                .putExtra(EXTRA_QUESTION, question)
                .putExtra(EXTRA_START_TIME, startTime)
                .putExtra(EXTRA_END_TIME, endTime)
                .putExtra(EXTRA_ABB_URL, abbUrl);
        context.startService(intent);
    }

    /**
     * Starts the Trustee service to perform the data initialization of the election specified.
     *
     * If the service is already performing a similar task, this action will be queued.
     * This task can be cancelled by calling {@link #cancelOperation} with the election ID.
     *
     * @param context the context associated with this request
     * @param electionId the ID of the election to initialize
     * @param uri the uri location of the file containing the initialization data
     */
    public void initializeElection(Context context, String electionId, Uri uri) {
        if (!requests.containsKey(electionId)) {
            Intent intent = new Intent(context, TrusteeService.class)
                    .setAction(ACTION_INITIALIZE_ELECTION)
                    .putExtra(EXTRA_ELECTION_ID, electionId)
                    .putExtra(EXTRA_RESULT_RECEIVER, resultReceiver)
                    .setData(uri);
            requests.put(electionId, intent);
            context.startService(intent);
        }
    }

    /**
     * Starts the Trustee service to perform the data verification the election specified.
     *
     * If the service is already performing a similar task, this action will be queued.
     * This task can be cancelled by calling {@link #cancelOperation} with the election ID.
     *
     * @param context the context associated with this request
     * @param electionId the ID of the election to verify
     */
    public void verifyElection(Context context, String electionId) {
        if (!requests.containsKey(electionId)) {
            Intent intent = new Intent(context, TrusteeService.class)
                    .setAction(ACTION_VERIFY_ELECTION)
                    .putExtra(EXTRA_ELECTION_ID, electionId)
                    .putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);
            requests.put(electionId, intent);
            context.startService(intent);
        }
    }

    /**
     * Starts the Trustee service to erase the election specified.
     *
     * If the service is already performing a similar task, this action will be queued.
     *
     * @param context the context associated with this request
     * @param electionId the ID of the election to erase
     */
    public void eraseElection(Context context, String electionId) {
        if (!requests.containsKey(electionId)) {
            Intent intent = new Intent(context, TrusteeService.class)
                    .setAction(ACTION_ERASE_ELECTION)
                    .putExtra(EXTRA_ELECTION_ID, electionId)
                    .putExtra(EXTRA_RESULT_RECEIVER, resultReceiver);
            requests.put(electionId, intent);
            context.startService(intent);
        }
    }

    /**
     * Starts the Trustee service to erase all elections.
     *
     * If the service is already performing a similar task, this action will be queued.
     *
     * @param context the context associated with this request
     */
    public void eraseAllElections(Context context) {
        Intent intent = new Intent(context, TrusteeService.class)
                .setAction(ACTION_ERASE_ALL_ELECTIONS);
        context.startService(intent);
    }

    /**
     * Cancels a previously requested operation, as specified by the other methods.
     *
     * Not all operations are cancellable (those that tend to be long-running are).
     * A cancellable operation can be cancelled whether it is currently being processed or will be
     * processed in the future. If the requested operation cannot be cancelled, nothing will happen.
     *
     * If the service is already performing a similar task, this action will be queued.
     *
     * @param context the context associated with this request
     * @param electionId the election ID of the old request that is going to be cancelled
     */
    public void cancelOperation(Context context, String electionId) {
        if (requests.containsKey(electionId)) {
            Intent intent = new Intent(context, TrusteeService.class)
                    .setAction(ACTION_CANCEL_OPERATION)
                    .putExtra(EXTRA_ELECTION_ID, electionId)
                    .putExtra(EXTRA_CANCELLED_TASK, requests.remove(electionId));
            context.startService(intent);
        }
    }

}
