package gr.uoa.di.finer.service;

import android.support.annotation.WorkerThread;

import gr.uoa.di.finer.ReadableDataStore;

/**
 * A read-write transactional data store, used to load and save data from and to storage.
 *
 * @author Vasilis Poulimenos
 */
@WorkerThread
public interface WritableDataStore extends ReadableDataStore {

    /**
     * Creates and stores a new election in this data store.
     *
     * @param electionId the ID of the election to create
     * @param question the question of the election
     * @param startTime the time that the election starts
     * @param endTime the time that the election ends
     * @param url the URL of the ABB board of the election
     * @param status the status of the election
     * @throws StoreException if the new election could not be saved to the data store
     */
    void createElection(
            String electionId, String question, long startTime, long endTime, String url, int status)
            throws StoreException;

    /**
     *
     *
     * @param electionId the ID of the election to modify
     * @param status the new status of the election
     * @throws StoreException
     */
    void setElectionStatus(String electionId, int status) throws StoreException;

    /**
     *
     *
     * @param electionId
     * @param decommitmentKey
     * @throws StoreException if the key could not be saved to the data store
     */
    void saveKey(String electionId, String decommitmentKey) throws StoreException;

    /**
     * Stores a new ballot for the election specified.
     *
     * @param electionId the ID of the election
     * @param serialNo the serial number of the ballot
     * @param partId the part ID of the ballot
     * @param voteCode the vote code
     * @param decommitment the decommitment value of the ballot
     * @throws StoreException if the ballot could not be saved to the data store
     */
    void saveBallot(
            String electionId, String serialNo, String partId, String voteCode, String decommitment)
            throws StoreException;

    /**
     *
     * @param electionId
     * @param decommitmentBundle
     * @throws StoreException
     */
    void saveDecommitmentBundle(String electionId, String decommitmentBundle) throws StoreException;

    /**
     * Erases the election with the specified ID.
     *
     * @param electionId the ID of the election to erase
     * @throws StoreException if the election could not be removed from the data store
     */
    void eraseElection(String electionId) throws StoreException;


    /**
     * Removes all ballots for the election with the specified ID.
     *
     * @param electionId the ID of the election
     * @throws StoreException if the ballots could not be removed from the data store
     */
    void eraseBallots(String electionId) throws StoreException;

    /**
     * Removes everything from the data store.
     *
     * @throws StoreException if the data store could not be cleared
     */
    void clear() throws StoreException;

}
