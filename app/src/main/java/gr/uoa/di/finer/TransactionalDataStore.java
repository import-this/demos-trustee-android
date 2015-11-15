package gr.uoa.di.finer;

import android.support.annotation.WorkerThread;

import gr.uoa.di.finer.service.StoreException;

/**
 * A data store that supports transactions.
 *
 * @author Vasilis Poulimenos
 */
@WorkerThread
interface TransactionalDataStore {

    /**
     * Begins a transaction.
     *
     * @throws StoreException if there was a problem starting the transaction
     */
    void beginTransaction() throws StoreException;

    /**
     * Marks the current transaction as successful.
     *
     * @throws StoreException if there was a problem marking the transaction
     */
    void setTransactionSuccessful() throws StoreException;

    /**
     * Ends a transaction.
     *
     * @throws StoreException if there was a problem ending the transaction
     */
    void endTransaction() throws StoreException;

}
