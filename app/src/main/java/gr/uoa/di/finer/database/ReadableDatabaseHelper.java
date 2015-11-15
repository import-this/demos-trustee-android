package gr.uoa.di.finer.database;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.support.annotation.WorkerThread;
import android.util.Log;

import gr.uoa.di.finer.BuildConfig;
import gr.uoa.di.finer.ReadableDataStore;
import gr.uoa.di.finer.database.TrusteeContract.BallotPart;
import gr.uoa.di.finer.database.TrusteeContract.Election;
import gr.uoa.di.finer.database.TrusteeContract.ElectionDynamicData;
import gr.uoa.di.finer.service.StoreException;

/**
 * A class of helper methods for a readable SQLite database with the {@link TrusteeContract} schema.
 * This class makes it easy and efficient to perform the more common queries in the application.
 * <p>
 * This class does not own the database provided upon construction and, thus, will not close it when
 * the method {@link #close()} is called. Clients must take care to call {@link #close()} when they
 * no longer need this object, in order to release resources used internally to query the database
 * (see, for instance, {@link SQLiteStatement}s), and then close the SQLiteDatabase - immediately
 * or at some point in the future, depending on usage by other objects.
 * <p>
 * Instances of this class are *NOT* thread-safe.
 * However, the {@link SQLiteDatabase} provided upon construction is. As a result, one could use
 * multiple instances of this class in separate threads to access the same {@link SQLiteDatabase}
 * concurrently. Check the {@link TrusteeOpenHelper} documentation for an analysis of SQLiteDatabase
 * connections in Android.
 * <p>
 * All exceptions thrown by SQLite operations (i.e. {@link android.database.SQLException},
 * {@link SQLiteException} and subclasses thereof) are unchecked. However, this class wraps the
 * above exceptions in the checked {@link SQLiteStoreException}, leaving some of the burden of
 * exception handling to the compiler.
 * <p>
 * Note:
 * Since most methods of this class perform queries on an {@link SQLiteDatabase} (which can be slow,
 * especially if the database is accessed - or even worse modified - by multiple threads/connections
 * outside of this class, causing blocking), they should not be called from the UI thread. Check the
 * {@link TrusteeOpenHelper} documentation for more info on this.
 *
 * @author Vasilis Poulimenos
 */
@WorkerThread
public class ReadableDatabaseHelper implements ReadableDataStore {

    private static final String TAG = WritableDatabaseHelper.class.getName();

    private static final String BALLOT_DECOMMITMENT_QUERY_STRING = String.format(
        "SELECT %s " +
        "FROM %s " +
        "WHERE %s = ? AND %s = ? AND %s = ?",
        BallotPart.COLUMN_NAME_DECOMMITMENT,
        BallotPart.TABLE_NAME,
        BallotPart.COLUMN_NAME_ELECTION_ID,
        BallotPart.COLUMN_NAME_SERIAL_NO,
        BallotPart.COLUMN_NAME_VOTE_CODE
    );


    protected SQLiteDatabase db;

    /*
     * The query for retrieval of a decommitment value associated with a ballot.
     */
    private final SQLiteStatement ballotDecommitmentQuery;

    /**
     * Creates a new ReadableDatabaseHelper.
     *
     * The constructed object does NOT own the {@link SQLiteDatabase} it uses. Thus, when the object
     * is closed, the database provided upon construction is NOT closed. Clients should close the
     * {@link SQLiteDatabase} object themselves after closing the ReadableDatabaseHelper instance.
     *
     * @param db a readable SQLite database
     */
    public ReadableDatabaseHelper(SQLiteDatabase db) {
        this.db = db;
        this.ballotDecommitmentQuery = db.compileStatement(BALLOT_DECOMMITMENT_QUERY_STRING);
    }

    /**
     * Static utility method used to create a readable database helper.
     *
     * @param dbHelper a helper used to open a readable database
     * @return a new readable database helper
     * @throws StoreException if the database cannot be opened
     */
    public static ReadableDataStore newInstance(TrusteeOpenHelper dbHelper) throws StoreException {
        try {
            return new ReadableDatabaseHelper(dbHelper.getReadableDatabase());
        } catch (SQLiteException e) {
            Log.e(TAG, "Readable database error", e);
            throw new StoreException("Could not open readable database", e);
        }
    }


    /**
     * Indicates whether or not this helper is closed.
     *
     * @return {@code true} if this helper is closed.
     */
    protected boolean isClosed() {
        return db == null;
    }

    /**
     * Checks if this helper is closed.
     *
     * @throws IllegalStateException if this helper is closed.
     */
    protected void checkNotClosed() {
        if (isClosed()) {
            throw new IllegalStateException("Database helper is closed");
        }
    }


    /**
     * Closes this helper.
     * Nothing is done if this helper has already been closed.
     */
    @Override
    public void close() {
        if (!isClosed()) {
            ballotDecommitmentQuery.close();
            db = null;
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Readable database helper closed");
            }
        }
    }

    /**
     * Begins a transaction.
     * <p>
     * Here is the standard idiom for transactions:
     *
     * <pre>
     *   dataSource.beginTransaction();
     *   try {
     *     ...
     *     dataSource.setTransactionSuccessful();
     *   } finally {
     *     dataSource.endTransaction();
     *   }
     * </pre>
     *
     * @throws StoreException if there was a problem starting the transaction
     */
    @Override
    public void beginTransaction() throws StoreException {
        checkNotClosed();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
                db.beginTransactionNonExclusive();
            } else {
                db.beginTransaction();
            }
        } catch (SQLiteException e) {
            throw new SQLiteStoreException(e);
        }
    }

    /**
     * Marks the current transaction as successful.
     *
     * @throws StoreException if there was a problem marking the transaction
     */
    @Override
    public void setTransactionSuccessful() throws StoreException {
        checkNotClosed();
        try {
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            throw new SQLiteStoreException(e);
        }
    }

    /**
     * Ends a transaction.
     *
     * @throws StoreException if there was a problem ending the transaction
     */
    @Override
    public void endTransaction() throws StoreException {
        checkNotClosed();
        try {
            db.endTransaction();
        } catch (SQLiteException e) {
            throw new SQLiteStoreException(e);
        }
    }


    /**
     * Checks if the election with the ID specified exists.
     *
     * @param electionId the ID of the election to check
     * @return true if the election exists
     * @throws StoreException if there was a problem retrieving the info from the database
     */
    @Override
    public boolean hasElection(String electionId) throws StoreException {
        Cursor cursor = getElection(electionId, null);
        try {
            return cursor.moveToFirst();
        } finally {
            cursor.close();
        }
    }

    // Joins: https://www.sqlite.org/lang_select.html
    private static final String ELECTION_QUERY_STRING = String.format(
            "SELECT %%s " +
            "FROM %1$s LEFT JOIN %2$s " +
                "ON %1$s.%3$s = %2$s.%4$s " +
            "WHERE %5$s = ?",
            Election.TABLE_NAME, ElectionDynamicData.TABLE_NAME,
            Election.COLUMN_NAME_ELECTION_ID, ElectionDynamicData.COLUMN_NAME_ELECTION_ID,
            Election.COLUMN_NAME_ELECTION_ID
    );

    /**
     * Retrieves the election with the specified ID.
     *
     * @param electionId the ID of the election to retrieve
     * @param columns A list of which columns to return. Passing null will return all columns,
     *      which is discouraged to prevent reading data from storage that isn't going to be used.
     * @return a cursor containing the election specified
     * @throws StoreException if there was a problem retrieving the election from the database
     */
    @Override
    public Cursor getElection(String electionId, String[] columns) throws StoreException {
        checkNotClosed();
        try {
            final StringBuilder columnsBuilder = new StringBuilder();

            if (columns != null && columns.length != 0) {
                SQLiteQueryBuilder.appendColumns(columnsBuilder, columns);
            } else {
                columnsBuilder.append("*");
            }
            // Note for future reference: rawQuery binds values as Strings.
            return db.rawQuery(
                    String.format(ELECTION_QUERY_STRING, columnsBuilder.toString()),
                    new String[] { electionId });
        } catch (SQLiteException e) {
            throw new SQLiteStoreException("Failed to retrieve election", e);
        }
    }

    private static final String SORT_ORDER = String.format(
            "%s ASC, %s ASC, %s ASC",
            Election.COLUMN_NAME_STATUS,        // New elections first, completed elections last.
            Election.COLUMN_NAME_START_TIME,    // New elections that start earlier first.
            Election.COLUMN_NAME_ELECTION_ID    // Election ID is not meaningful, so leave it third.
    );

    /**
     * Retrieves all the elections in this database.
     *
     * @param columns A list of which columns to return. Passing null will return all columns,
     *      which is discouraged to prevent reading data from storage that isn't going to be used.
     * @return a cursor containing all the elections stored
     * @throws StoreException if there was a problem retrieving the elections from the database
     */
    @Override
    public Cursor getAllElections(String[] columns) throws StoreException {
        checkNotClosed();
        try {
            return db.query(
                    Election.TABLE_NAME,
                    columns,
                    null, null, null, null,         // No WHERE, GROUP BY or HAVING clauses.
                    SORT_ORDER);
        } catch (SQLiteException e) {
            throw new SQLiteStoreException("Failed to retrieve elections", e);
        }
    }

    /**
     * Retrieves the status of the election specified.
     *
     * @param electionId the ID of the election to query
     * @return the status of the election
     * @throws StoreException if there was a problem retrieving the status from the database
     * @throws UnknownElectionException if the election ID does not exist
     */
    @Override
    public int getElectionStatus(String electionId) throws StoreException {
        Cursor cursor = null;

        checkNotClosed();
        try {
            cursor = db.query(
                    Election.TABLE_NAME,
                    new String[] { Election.COLUMN_NAME_STATUS },
                    Election.COLUMN_NAME_ELECTION_ID + " = ?",
                    new String[] { electionId },
                    null, null, null);              // No GROUP BY, HAVING or ORDER BY clauses.
            if (!cursor.moveToFirst()) {
                throw new UnknownElectionException(electionId);
            }
            return cursor.getInt(0);
        } catch (SQLiteException e) {
            throw new SQLiteStoreException("Failed to retrieve election status", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Retrieves the URL of the Audit Bulletin Board (ABB) of the election specified.
     *
     * @param electionId the ID of the election to query
     * @return the URL of the ABB of the election
     * @throws StoreException if there was a problem retrieving the ABB from the database
     * @throws UnknownElectionException if the election ID does not exist
     */
    @Override
    public String getElectionAbb(String electionId) throws StoreException {
        Cursor cursor = null;

        checkNotClosed();
        try {
            cursor = db.query(
                    Election.TABLE_NAME,
                    new String[] { Election.COLUMN_NAME_ABB_URL},
                    Election.COLUMN_NAME_ELECTION_ID + " = ?",
                    new String[] { electionId },
                    null, null, null);              // No GROUP BY, HAVING or ORDER BY clauses.
            if (!cursor.moveToFirst()) {
                throw new UnknownElectionException(electionId);
            }
            return cursor.getString(0);
        } catch (SQLiteException e) {
            throw new SQLiteStoreException("Failed to retrieve election ABB URL", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    private static final String ELECTION_DECOMMITMENT_QUERY = String.format(
        "SELECT %s " +
        "FROM %s " +
        "WHERE %s = ?",
        Election.COLUMN_NAME_DECOMMITMENT_KEY,
        Election.TABLE_NAME,
        Election.COLUMN_NAME_ELECTION_ID
    );

    /**
     * Retrieves the decommitment key of the election specified.
     *
     * @param electionId the ID of the election to query
     * @return the decommitment key for the specified election or null if there is no key.
     * @throws StoreException if there was a problem retrieving the key from the database
     * @throws UnknownElectionException if the election ID does not exist
     */
    @Override
    public String getElectionDecommitmentKey(String electionId) throws StoreException {
        Cursor cursor = null;

        checkNotClosed();
        try {
            // Note for future reference: rawQuery binds values as Strings.
            cursor = db.rawQuery(ELECTION_DECOMMITMENT_QUERY, new String[] { electionId });
            if (!cursor.moveToFirst()) {
                throw new UnknownElectionException(electionId);
            }
            return (cursor.isNull(0)) ? null : cursor.getString(0);
        } catch (SQLiteException e) {
            throw new SQLiteStoreException("Failed to retrieve decommitment key", e);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     *
     * It is assumed that the election ID is valid.
     *
     * @param electionId the ID of the election to query for a ballot
     * @param serialNumber the serial number of the ballot (for the election specified)
     * @param voteCode the vote code
     * @return the decommitment associated with the ballot or null if the ballot is invalid
     * @throws StoreException if there was a problem retrieving the value from the database
     */
    @Override
    public String getBallotDecommitment(String electionId, String serialNumber, String voteCode)
            throws StoreException {
        checkNotClosed();
        ballotDecommitmentQuery.bindString(1, electionId);
        ballotDecommitmentQuery.bindString(2, serialNumber);
        ballotDecommitmentQuery.bindString(3, voteCode);

        try {
            return ballotDecommitmentQuery.simpleQueryForString();
        } catch (SQLiteDoneException ignored) {
            // Invalid ballot
            return null;
        } catch (SQLiteException e) {
            throw new SQLiteStoreException("Failed to query ballot decommitment", e);
        }
    }

}
