package gr.uoa.di.finer.database;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.support.annotation.WorkerThread;
import android.util.Log;

/**
 * Class for the creation and version management of the main database of the application.
 * <p>
 * Apart from potentially invalid SQL strings exceptions, no error handling is performed here.
 * <p>
 * Instances of this class are thread-safe.
 *
 * <h2>About {@link SQLiteDatabase}s</h2>
 *
 * {@link SQLiteDatabase} is thread-safe. One could use the same instance to access it concurrently.
 *
 *
 * <h2>About {@link SQLiteOpenHelper}s</h2>
 *
 * This class makes it easy to create/open/upgrade the database lazily, meaning that instantiating
 * this class does not open the database. This is deferred until {@link #getReadableDatabase()} or
 * {@link #getWritableDatabase()} is called.
 * <p>
 * A reference to the database opened upon first retrieval is kept internally.
 * Repeated calls to {@link #getReadableDatabase()} or {@link #getWritableDatabase()} will return
 * the exact same database. The two methods do not return separate databases, nor they create a
 * new one upon each execution. If you want different {@link SQLiteDatabase} objects, you have to
 * create more that one instance of this helper class.
 * @see SQLiteOpenHelper#getWritableDatabase()
 *
 * <h2>About database connections in Android</h2>
 *
 * Note:
 * Some of the things stated below are not publicly documented (but most of them are documented
 * in the Android SDK), so they may be subject to change in a future version of Android.
 * <p>
 * Behind the scenes Android uses {@code SQLiteSession} objects. Session objects are thread-bound.
 * The {@link SQLiteDatabase} created by an instance of {@link SQLiteOpenHelper} uses a thread-local
 * variable to associate a session with each thread for the use of that thread alone. Consequently,
 * each thread has its own session object and therefore its own transaction state independent of
 * other threads.
 * <p>
 * A thread has at most one session per database. This constraint ensures that a thread can never
 * use more than one database connection at a time for a given database. This is necessary as the
 * number of available database connections is limited.
 * <p>
 * When <em>Write-Ahead Logging (WAL)</em> is enabled, the database can execute simultaneous
 * read-only transactions with one read-write (but only one). When WAL is not enabled, read-only
 * transactions can execute in parallel but only one read-write transaction.
 * <p>
 * A {@link SQLiteDatabase} can have multiple active sessions at the same time. Each session
 * acquires and releases connections to the database (from a {@code SQLiteConnectionPool} of
 * fixed size as needed to perform each requested database transaction. If all connections are
 * in use, then transactions on some sessions will block until a connection becomes available.
 * <p>
 * Currently, Android creates a connection pool with only one active connection at a time when in
 * non-WAL mode. On the other hand, when in WAL mode the connection pool has many connections (the
 * exact size is system defined - dependent upon the device memory and possibly other properties -,
 * but they are at least two).
 * <p>
 * A session acquires a single database connection only for the duration of a single (implicit or
 * explicit) transaction, then releases it. This allows a small pool of database connections to be
 * shared efficiently by multiple sessions as long as they are not all trying to perform database
 * transactions at the same time. Because there are a limited number of database connections and
 * the session holds a database connection for the entire duration of a transaction, it is important
 * to keep transactions short, especially for read-write transactions, which may block others
 * from executing.
 *
 * Conclusions:
 * <ul>
 * <li>Use separate {@link TrusteeOpenHelper}s to get separate {@link SQLiteDatabase}s.</li>
 * <li>There is only one session (so, at most one connection) per thread per database.</li>
 * <li>There can be only one writer at any given moment, regardless of journal mode.</li>
 * <li>There can be as many concurrent readers as needed, regardless of journal mode.</li>
 * <li>When Write-Ahead Logging (WAL) is enabled, one writer does not block readers.</li>
 * <li>Do not perform database transactions on the UI thread.</li>
 * <li>Keep database transactions as short as possible.</li>
 * </ul>
 *
 * @author Vasilis Poulimenos
 */
public class TrusteeOpenHelper extends SQLiteOpenHelper {

    /*
     * SQLite in Android notes:
     * 0. Android Gingerbread (2.3.3/SDK10) supports SQLite 3.6.22, so this defines our syntax.
     *    http://stackoverflow.com/a/4377116/1751037
     *
     *    The supported syntax for SQLite 3.6.22 can be inferred from the following resource:
     *    http://www.sqlite.org/changes.html#version_3_6_22
     *
     *    Unsupported features:
     *      - WITHOUT ROWID optimization (support was added to SQLite version 3.8.2).
     *        https://www.sqlite.org/withoutrowid.html
     *
     * SQLite notes:
     * 0. Tuning:
     *    0. Write-Ahead Logging (WAL):
     *       WAL has significant performance advantages compared to a rollback journal, while
     *       its disadvantages do not apply to our use case. The most important benefits are:
     *       1. WAL is significantly faster in most scenarios.
     *       2. WAL provides more concurrency as readers do not block writers and a writer does not
     *          block readers. Reading and writing can proceed concurrently.
     *       However, WAL was introduced in SQLite version 3.7.0 (Android 3.0/API 11). In order to
     *       take advantage of it when available, we are going to conditionally enable it.
     *       http://www.sqlite.org/wal.html
     *    1. Page size:
     *       The default (usually 4096 bytes) seems optimal.
     *       https://www.sqlite.org/pragma.html#pragma_page_size
     *       SQLiteDatabase.html#setPageSize(long)
     *    2. Cache size:
     *       1. Page cache size:
     *          The default seems optimal.
     *          https://www.sqlite.org/pragma.html#pragma_cache_size
     *       2. SQL cache size:
     *          The default is enough.
     *          SQLiteDatabase.html#setMaxSqlCacheSize(int)
     * 1. Enabling Foreign Key Support:
     *    SQL foreign key constraints were introduced in SQLite version 3.6.19 and are disabled
     *    by default (for backwards compatibility). To enable them check the following resource:
     *    https://www.sqlite.org/foreignkeys.html#fk_enable
     * 2. The VACUUM command:
     *    https://www.sqlite.org/lang_vacuum.html
     * 3. The ANALYZE command:
     *    https://www.sqlite.org/lang_analyze.html
     * 4. The SQLITE_BUSY result code.
     *    https://www.sqlite.org/rescode.html#busy
     *    https://www.sqlite.org/pragma.html#pragma_busy_timeout
     */

    private static final String TAG = TrusteeOpenHelper.class.getName();

    private static final String DATABASE_NAME = "trustee.db";
    /*
     * Remember to increment the database version number if the database schema is changed.
     */
    private static final int DATABASE_VERSION = 1;

    private static final int BUSY_TIMEOUT_MILLIS = 1_000 * 60 * 2;              // 2 minutes

    public TrusteeOpenHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }


    private void enableWriteAheadLogging() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            setWriteAheadLoggingEnabled(true);
            Log.i(TAG, "Set Write-Ahead logging enabled");
        } else {
            // Since the {@link SQLiteOpenHelper#setWriteAheadLoggingEnabled(boolean)} method is
            // available since API 16, systems with API 10-15 will get some journal mode SQLite
            // errors/warnings when opening a database, but everything will still work just fine.
            Log.w(TAG, "Database will be opened in non-WAL mode first. Harmless error will occur.");
        }
    }

    /**
     * Returns the same {@link SQLiteDatabase} that {@link SQLiteOpenHelper#getWritableDatabase()}
     * returns, but enables Write-Ahead logging when available (if not already enabled).
     *
     * @see SQLiteOpenHelper#getWritableDatabase()
     */
    @Override
    @WorkerThread
    public SQLiteDatabase getWritableDatabase() {
        enableWriteAheadLogging();
        return super.getWritableDatabase();
    }

    @Override
    @WorkerThread
    public SQLiteDatabase getReadableDatabase() {
        // WAL is not supported for read-only databases, but the system does not actually return
        // a read-only database, because - as stated in SQLiteOpenHelper - "it is inefficient and
        // breaks many applications". Since we get a writable database most of the time, enable WAL.
        enableWriteAheadLogging();
        return super.getReadableDatabase();
    }


    /**
     * Called when the database is created for the first time. This is where the
     * creation of tables and the initial population of the tables should happen.
     *
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            // Tell SQLite to shrink the database file after deletions, so as to reclaim disk space.
            // Note that auto-vacuuming must be turned on before any tables are created.
            // http://sqlite.org/pragma.html#pragma_auto_vacuum
            db.execSQL("PRAGMA auto_vacuum = FULL");
        } catch (SQLException impossible) {
            Log.wtf(TAG, "Invalid SQL string", impossible);
        }
        TrusteeContract.create(db);
        Log.i(TAG, "Created database");
    }

    /**
     * Called when the database needs to be upgraded. The implementation
     * should use this method to drop tables, add tables, or do anything else it
     * needs to upgrade to the new schema version.
     * <p/>
     * <p>
     * The SQLite ALTER TABLE documentation can be found
     * <a href="http://sqlite.org/lang_altertable.html">here</a>. If you add new columns
     * you can use ALTER TABLE to insert them into a live table. If you rename or remove columns
     * you can use ALTER TABLE to rename the old table, then create the new table and then
     * populate the new table with the contents of the old table.
     * </p><p>
     * This method executes within a transaction.  If an exception is thrown, all changes
     * will automatically be rolled back.
     * </p>
     *
     * @param db         The database.
     * @param oldVersion The old database version.
     * @param newVersion The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Upgrading database " + DATABASE_NAME + " from version " + oldVersion + " to " +
                newVersion + ", which will destroy all old data");
        TrusteeContract.upgrade(db);
        Log.i(TAG, "Upgraded database");
        db.execSQL("VACUUM");
        Log.d(TAG, "Vacuumed database");
        db.execSQL("ANALYZE");
        Log.d(TAG, "Analyzed database");
    }

    /**
     * Opens the database, with foreign key support enabled.
     * <p>
     * Note:
     * According to the Android docs, {@link #onConfigure} is the appropriate place for
     * enabling foreign key constraints (with the
     * {@link SQLiteDatabase#setForeignKeyConstraintsEnabled} method), but both methods
     * were added in API level 16, whereas our application supports API level 10 and up.
     * </p>
     *
     * @param db The database.
     */
    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        // Note that if the database is read-only (no changes), foreign key support is not needed.
        if (!db.isReadOnly()) {
            // Enable foreign key constraints for this session.
            try {
                db.execSQL("PRAGMA foreign_keys = ON");
                Log.i(TAG, "Enabled foreign key constraints");
            } catch (SQLException impossible) {
                Log.wtf(TAG, "Invalid SQL string", impossible);
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            if (db.enableWriteAheadLogging()) {
                Log.i(TAG, "Enabled Write-Ahead logging");
            }
        }

        Cursor cursor = null;
        try {
            // Set a busy timeout so as to try to avoid SQLITE_BUSY errors in the case of multiple
            // connections. Notice that if an operation in a concurrent connection is long-running
            // (lasts more than 'busy_timeout'), then the error will not be avoided.
            // Note: This pragma returns data, so rawQuery is used instead of execSQL.
            cursor = db.rawQuery("PRAGMA busy_timeout = " + BUSY_TIMEOUT_MILLIS, null);
            Log.i(TAG, String.format("Set busy timeout to %,d ms", BUSY_TIMEOUT_MILLIS));

            db.execSQL("PRAGMA synchronous = NORMAL");
            Log.i(TAG, "Set synchronous flag to NORMAL");

            // https://www.sqlite.org/pragma.html#pragma_journal_mode
            // Will this actually improve performance?
            //db.execSQL("PRAGMA journal_mode = TRUNCATE");
        } catch (SQLException impossible) {
            Log.wtf(TAG, "Invalid SQL string", impossible);
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        Log.i(TAG, "Opened database as " + ((db.isReadOnly()) ? "read-only" : "read-write"));
    }

}
