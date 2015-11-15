package gr.uoa.di.finer;

import android.database.Cursor;
import android.support.annotation.WorkerThread;

import gr.uoa.di.finer.service.StoreException;

/**
 * A read-only transactional data store, used to retrieve data from storage.
 *
 * @author Vasilis Poulimenos
 */
@WorkerThread
public interface ReadableDataStore extends TransactionalDataStore {

    /**
     * Checks if the election with the ID specified exists.
     *
     * @param electionId the ID of the election to check
     * @return true if the election exists
     * @throws StoreException if there was a problem retrieving the info from the data store
     */
    boolean hasElection(String electionId) throws StoreException;

    /**
     * Retrieves the election with the specified ID.
     *
     * @param electionId the ID of the election to retrieve
     * @param columns A list of which columns to return. Passing null will return all columns,
     *      which is discouraged to prevent reading data from storage that isn't going to be used.
     * @return a cursor containing the election specified
     * @throws StoreException if there was a problem retrieving the election from the data store
     */
    Cursor getElection(String electionId, String[] columns) throws StoreException;

    /**
     * Retrieves all the elections in this data store.
     *
     * @param columns A list of which columns to return. Passing null will return all columns,
     *      which is discouraged to prevent reading data from storage that isn't going to be used.
     * @return a cursor containing all the elections stored
     * @throws StoreException if there was a problem retrieving the elections from the data store
     */
    Cursor getAllElections(String[] columns) throws StoreException;

    /**
     * Retrieves the status of the election specified.
     *
     * @param electionId the ID of the election to query
     * @return the status of the election
     * @throws StoreException if there was a problem retrieving the status from the data store
     */
    int getElectionStatus(String electionId) throws StoreException;

    /**
     * Retrieves the URL of the Audit Bulletin Board (ABB) of the election specified.
     *
     * @param electionId the ID of the election to query
     * @return the URL of the ABB of the election
     * @throws StoreException if there was a problem retrieving the ABB from the data store
     */
    String getElectionAbb(String electionId) throws StoreException;

    /**
     * Retrieves the decommitment key of the election specified.
     *
     * @param electionId the ID of the election to query
     * @return the decommitment key of the election of null if there is no key
     * @throws StoreException if there was a problem retrieving the key from the data store
     */
    String getElectionDecommitmentKey(String electionId) throws StoreException;

    /**
     * Retrieves the decommitment value of the ballot specified.
     *
     * @param electionId the ID of the election to query for a ballot
     * @param serialNumber the serial number of the ballot (for the election specified)
     * @param voteCode the vote code
     * @return the decommitment value of the ballot
     * @throws StoreException if there was a problem retrieving the value from the data store
     */
    String getBallotDecommitment(String electionId, String serialNumber, String voteCode)
            throws StoreException;

    /**
     * Closes this data store, releasing any system resources associated with it.
     */
    void close();

}
