package gr.uoa.di.finer.database;

import android.content.ContentValues;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteFullException;
import android.database.sqlite.SQLiteStatement;
import android.support.annotation.WorkerThread;
import android.util.Log;

import gr.uoa.di.finer.BuildConfig;
import gr.uoa.di.finer.database.TrusteeContract.BallotPart;
import gr.uoa.di.finer.database.TrusteeContract.Election;
import gr.uoa.di.finer.database.TrusteeContract.ElectionDynamicData;
import gr.uoa.di.finer.service.StoreException;
import gr.uoa.di.finer.service.WritableDataStore;

/**
 * A class of helper methods for a writable (and, thus, also readable) SQLite database with
 * the {@link TrusteeContract} schema. This class makes it easy and efficient to perform the
 * more common queries in the application.
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
public class WritableDatabaseHelper extends ReadableDatabaseHelper implements WritableDataStore {

    private static final String TAG = WritableDatabaseHelper.class.getName();

    private static final String INSERT_ELECTION_ERROR_MSG = "Failed to insert election";
    private static final String INSERT_BALLOT_ERROR_MSG = "Failed to insert ballot";
    private static final String INSERT_DECOMMITMENT_BUNDLE_ERROR_MSG =
            "Failed to insert decommitment bundle";


    private final SQLiteStatement insertElectionStmt = db.compileStatement(
        String.format(
            "INSERT INTO %s (%s, %s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?, ?)",
            Election.TABLE_NAME,
            Election.COLUMN_NAME_ELECTION_ID,
            Election.COLUMN_NAME_QUESTION,
            Election.COLUMN_NAME_START_TIME,
            Election.COLUMN_NAME_END_TIME,
            Election.COLUMN_NAME_ABB_URL,
            Election.COLUMN_NAME_STATUS)
    );

    private final SQLiteStatement insertBallotStmt = db.compileStatement(
        String.format(
            "INSERT INTO %s (%s, %s, %s, %s, %s) VALUES (?, ?, ?, ?, ?)",
            BallotPart.TABLE_NAME,
            BallotPart.COLUMN_NAME_ELECTION_ID,
            BallotPart.COLUMN_NAME_SERIAL_NO,
            BallotPart.COLUMN_NAME_PART,
            BallotPart.COLUMN_NAME_VOTE_CODE,
            BallotPart.COLUMN_NAME_DECOMMITMENT)
    );

    private final SQLiteStatement insertDecommitmentBundleStmt = db.compileStatement(
        String.format(
            "INSERT INTO %s (%s, %s) VALUES (?, ?)",
            ElectionDynamicData.TABLE_NAME,
            ElectionDynamicData.COLUMN_NAME_ELECTION_ID,
            ElectionDynamicData.COLUMN_NAME_DECOMMITMENT_BUNDLE)
    );


    /**
     *
     * @param db a writable SQLite database
     * @throws IllegalArgumentException if the database provided is read-only
     */
    public WritableDatabaseHelper(SQLiteDatabase db) {
        super(db);
        if (db.isReadOnly()) {
            throw new IllegalArgumentException("Database is read-only");
        }
    }

    /**
     * Static utility method used to create a writable database helper.
     *
     * @param  dbHelper a helper used to open a writable database
     * @return a new writable database helper
     * @throws StoreException if the database cannot be opened
     */
    public static WritableDataStore newInstance(TrusteeOpenHelper dbHelper) throws StoreException {
        try {
            return new WritableDatabaseHelper(dbHelper.getWritableDatabase());
        } catch (SQLException e) {
            Log.e(TAG, "Writable database error", e);
            throw new StoreException("Could not open writable database", e);
        } catch (IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }


    @Override
    public void close() {
        if (!isClosed()) {
            insertElectionStmt.close();
            insertBallotStmt.close();
            insertDecommitmentBundleStmt.close();
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Writable database helper closed");
            }
            super.close();
        }
    }

    /**
     *
     * @param electionId
     * @param question
     * @param startTime
     * @param endTime
     * @param url
     * @param status
     * @throws SQLiteStoreException
     * @throws SQLiteStoreFullException
     */
    @Override
    public void createElection(
            String electionId, String question, long startTime, long endTime, String url, int status)
            throws StoreException {
        checkNotClosed();
        insertElectionStmt.clearBindings();
        insertElectionStmt.bindString(1, electionId);
        insertElectionStmt.bindString(2, question);
        insertElectionStmt.bindLong(3, startTime);
        insertElectionStmt.bindLong(4, endTime);
        insertElectionStmt.bindString(5, url);
        insertElectionStmt.bindLong(6, status);

        try {
            if (insertElectionStmt.executeInsert() == -1) {
                throw new SQLiteStoreException(INSERT_ELECTION_ERROR_MSG);
            }
        } catch (SQLiteFullException e) {
            throw new SQLiteStoreFullException(e);
        } catch (SQLException e) {
            throw new SQLiteStoreException(INSERT_ELECTION_ERROR_MSG, e);
        }
    }

    /**
     *
     * @param electionId
     * @param status
     * @throws SQLiteStoreException
     * @throws SQLiteStoreFullException
     */
    @Override
    public void setElectionStatus(String electionId, int status) throws StoreException {
        final ContentValues values;

        checkNotClosed();
        values = new ContentValues();
        values.put(Election.COLUMN_NAME_STATUS, status);

        try {
            db.update(
                    Election.TABLE_NAME,
                    values,
                    Election.COLUMN_NAME_ELECTION_ID + " = ?",
                    new String[] { electionId });
        } catch (SQLiteFullException e) {
            throw new SQLiteStoreFullException(e);
        } catch (SQLException e) {
            throw new SQLiteStoreException("Saving status failed", e);
        }
    }

    /**
     *
     * @param electionId
     * @param decommitmentKey
     * @throws SQLiteStoreException
     * @throws SQLiteStoreFullException
     */
    @Override
    public void saveKey(String electionId, String decommitmentKey) throws StoreException {
        final ContentValues values;

        checkNotClosed();
        values = new ContentValues();
        values.put(Election.COLUMN_NAME_DECOMMITMENT_KEY, decommitmentKey);

        try {
            db.update(
                    Election.TABLE_NAME,
                    values,
                    Election.COLUMN_NAME_ELECTION_ID + " = ?",
                    new String[] { electionId });
        } catch (SQLiteFullException e) {
            throw new SQLiteStoreFullException(e);
        } catch (SQLException e) {
            throw new SQLiteStoreException("Saving key failed", e);
        }
    }

    /**
     *
     * @param electionId
     * @param serialNo
     * @param partId
     * @param voteCode
     * @param decommitment
     * @throws SQLiteStoreException
     * @throws SQLiteStoreFullException
     */
    @Override
    public void saveBallot(
            String electionId, String serialNo, String partId, String voteCode, String decommitment)
            throws StoreException {
        checkNotClosed();
        insertBallotStmt.bindString(1, electionId);
        insertBallotStmt.bindString(2, serialNo);
        insertBallotStmt.bindString(3, partId);
        insertBallotStmt.bindString(4, voteCode);
        insertBallotStmt.bindString(5, decommitment);

        try {
            if (insertBallotStmt.executeInsert() == -1) {
                throw new SQLiteStoreException(INSERT_BALLOT_ERROR_MSG);
            }
        } catch (SQLiteFullException e) {
            throw new SQLiteStoreFullException(e);
        } catch (SQLException e) {
            throw new SQLiteStoreException(INSERT_BALLOT_ERROR_MSG, e);
        }
    }

    /**
     *
     * @param electionId
     * @param decommitmentBundle
     * @throws SQLiteStoreException
     * @throws SQLiteStoreFullException
     */
    @Override
    public void saveDecommitmentBundle(String electionId, String decommitmentBundle)
            throws StoreException {
        checkNotClosed();
        insertDecommitmentBundleStmt.bindString(1, electionId);
        insertDecommitmentBundleStmt.bindString(2, decommitmentBundle);

        try {
            if (insertDecommitmentBundleStmt.executeInsert() == -1) {
                throw new SQLiteStoreException(INSERT_DECOMMITMENT_BUNDLE_ERROR_MSG);
            }
        } catch (SQLiteFullException e) {
            throw new SQLiteStoreFullException(e);
        } catch (SQLException e) {
            throw new SQLiteStoreException(INSERT_DECOMMITMENT_BUNDLE_ERROR_MSG, e);
        }
    }

    /**
     *
     * @param electionId
     * @throws SQLiteStoreException
     * @throws SQLiteStoreFullException
     */
    @Override
    public void eraseElection(String electionId) throws StoreException {
        checkNotClosed();
        try {
            // First, delete the ballots in small parts to avoid SQLiteFullException exceptions.
            eraseBallots(electionId);
            // Now delete the entry from the election table.
            db.delete(
                    Election.TABLE_NAME,
                    Election.COLUMN_NAME_ELECTION_ID + " = ?",
                    new String[] { electionId });
        } catch (SQLiteFullException e) {
            throw new SQLiteStoreFullException(e);
        } catch (SQLException e) {
            throw new SQLiteStoreException("Failed to delete election", e);
        }
    }

    private static final int LIMIT = 10000;

    // SQLite in Android does not support the LIMIT clause in DELETE statements.
    // https://www.sqlite.org/lang_delete.html
    private static final String DELETE_N_BALLOTS_WHERE_CLAUSE = String.format(
        "%2$s IN (" +
            "SELECT %2$s " +
            "FROM %1$s " +
            "WHERE %3$s = ? " +
            "LIMIT %4$d)",
        BallotPart.TABLE_NAME,
        BallotPart.COLUMN_NAME_BALLOT_PART_ID,
        BallotPart.COLUMN_NAME_ELECTION_ID,
        LIMIT
    );

    /**
     *
     * @param electionId
     * @throws SQLiteStoreException
     * @throws SQLiteStoreFullException
     */
    @Override
    public void eraseBallots(String electionId) throws StoreException {
        checkNotClosed();
        try {
            final String [] whereArgs = new String[] { electionId };

            // Delete the ballots in small parts to avoid SQLiteFullException exceptions.
            while (db.delete(BallotPart.TABLE_NAME, DELETE_N_BALLOTS_WHERE_CLAUSE, whereArgs) == LIMIT) {}
        } catch (SQLiteFullException e) {
            throw new SQLiteStoreFullException(e);
        } catch (SQLException e) {
            throw new SQLiteStoreException("Failed to delete ballots", e);
        }
    }

    /**
     *
     * @throws SQLiteStoreException
     * @throws SQLiteStoreFullException
     */
    @Override
    public void clear() throws StoreException {
        checkNotClosed();
        beginTransaction();
        try {
            TrusteeContract.clear(db);
            setTransactionSuccessful();
        } catch (SQLiteFullException e) {
            throw new SQLiteStoreFullException(e);
        } catch (SQLException e) {
            throw new SQLiteStoreException("Emptying database failed", e);
        } finally {
            endTransaction();
        }
        // Emptying the database might leave a mess. Try to clean it up.
        try {
            Log.i(TAG, "Vacuuming...");
            vacuum();
            Log.i(TAG, "Vacuumed");
        } catch (StoreException e) {
            // Ok, the database is still totally usable.
            Log.w(TAG, e);
        }
    }

    /**
     * Tidies up the database.
     *
     * Defragments and shrinks the file, so as to increase performance and reclaim disk space.
     * When VACUUMing a database, as much as twice the size of the original database file is
     * required in free disk space.
     *
     * @throws SQLiteStoreException
     * @throws SQLiteStoreFullException
     */
    public void vacuum() throws StoreException {
        checkNotClosed();
        try {
            // http://sqlite.org/lang_vacuum.html
            db.execSQL("VACUUM");
        } catch (SQLiteFullException e) {
            throw new SQLiteStoreFullException(e);
        } catch (SQLException e) {
            throw new SQLiteStoreException("Vacuuming failed", e);
        }
    }

    /**
     * Tidies up the database and performs post-cleanup analysis.
     *
     * @throws SQLiteStoreException
     * @throws SQLiteStoreFullException
     */
    public void vacuumAndAnalyze() throws StoreException {
        vacuum();
        try {
            db.execSQL("ANALYZE");
        } catch (SQLException e) {
            throw new SQLiteStoreException("Analyzing failed", e);
        }
    }

}
