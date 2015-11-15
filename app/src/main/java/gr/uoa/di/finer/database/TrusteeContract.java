package gr.uoa.di.finer.database;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;

/**
 * Contract class per the Android Database documentation:
 * http://developer.android.com/training/basics/data-storage/databases.html
 *
 * No error handling or exception chaining is performed here. Everything is left to the clients.
 *
 * @author Vasilis Poulimenos
 */
public final class TrusteeContract {

    /*
     * SQLite notes:
     * 0. Datatypes in SQLite Version 3:
     *    http://www.sqlite.org/datatype3.html
     *    Even though SQLite does not specify conventional data types, they are
     *    preferred here for completeness and consistency with other databases.
     * 1. According to the SQL standard, PRIMARY KEY should always imply NOT NULL.
     *    Unfortunately, due to a bug in some early versions, this is not the case in SQLite.
     *    Unless the column is an INTEGER PRIMARY KEY or the table is a WITHOUT ROWID table or
     *    the column is declared NOT NULL, SQLite allows NULL values in a PRIMARY KEY column.
     *    http://www.sqlite.org/lang_createtable.html
     * 2. A column declared INTEGER PRIMARY KEY will autoincrement.
     *    (It will be filled automatically with an unused integer, usually one more than
     *    the largest ROWID currently in use. This is true regardless of whether or not
     *    the AUTOINCREMENT keyword is used.)
     *    https://www.sqlite.org/faq.html#q1
     *    https://www.sqlite.org/autoinc.html
     * 3. No support for the MATCH clause in foreign key constraints:
     *    https://www.sqlite.org/foreignkeys.html#fk_unsupported
     *    All foreign key constraints in SQLite are handled as if MATCH SIMPLE were specified.
     * 4. No TRUNCATE TABLE statement.
     *    However, SQLite employs the so called "truncate optimization":
     *    When the WHERE is omitted from a DELETE statement and the table being deleted has
     *    *no triggers*, an optimization occurs that causes the DELETE to occur by dropping and
     *    recreating the table.
     *    So, in order to perform truncation simply execute:
     *    DELETE FROM <table>
     *    https://www.sqlite.org/lang_delete.html
     *
     * SQLite in Android notes:
     * 0. Due to the database lifecycle in an Android app, "IF NOT EXISTS" or "IF EXISTS" are
     *    unnecessary, but are left for completeness and consistency with other databases.
     */

    // Prevent instantiation.
    private TrusteeContract() { throw new AssertionError("Non-instantiable class"); }


    /**
     *
     * @param db
     */
    static void create(SQLiteDatabase db) {
        Election.create(db);
        Option.create(db);
        BallotPart.create(db);
        ElectionDynamicData.create(db);
    }

    /**
     *
     * @param db
     */
    static void upgrade(SQLiteDatabase db) {
        // The order is important to avoid foreign key constraints violations.
        ElectionDynamicData.upgrade(db);
        BallotPart.upgrade(db);
        Option.upgrade(db);
        Election.upgrade(db);
        create(db);
    }

    /**
     * Erases the entire content of all tables in the database.
     *
     * @param db
     */
    static void clear(SQLiteDatabase db) {
        // Since there is no TRUNCATE TABLE statement, employ the truncate optimization.
        // The order is important to avoid foreign key constraints violations.
        ElectionDynamicData.truncate(db);
        BallotPart.truncate(db);
        Option.truncate(db);
        Election.truncate(db);
    }


    public static final class Election {
        public static final String TABLE_NAME = "Election";
        // Cursor adaptors expect a column with name '_id'.
        public static final String COLUMN_NAME_ELECTION_ID = BaseColumns._ID;
        public static final String COLUMN_NAME_QUESTION = "question";
        public static final String COLUMN_NAME_START_TIME = "startTime";
        public static final String COLUMN_NAME_END_TIME = "endTime";
        public static final String COLUMN_NAME_ABB_URL = "abbUrl";
        public static final String COLUMN_NAME_DECOMMITMENT_KEY = "decommitmentKey";
        public static final String COLUMN_NAME_STATUS = "status";

        private static final String ELECTION_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_NAME_ELECTION_ID + " VARCHAR(36) PRIMARY KEY NOT NULL," +
                COLUMN_NAME_QUESTION + " VARCHAR(200) NOT NULL," +
                COLUMN_NAME_START_TIME + " TIMESTAMP WITHOUT TIME ZONE NOT NULL," +
                COLUMN_NAME_END_TIME + " TIMESTAMP WITHOUT TIME ZONE NOT NULL," +
                COLUMN_NAME_ABB_URL + " VARCHAR NOT NULL," +
                COLUMN_NAME_DECOMMITMENT_KEY + " VARCHAR," +
                COLUMN_NAME_STATUS + " INTEGER NOT NULL" +
            ")";

        private static void drop(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        }

        private static void truncate(SQLiteDatabase db) {
            db.delete(TABLE_NAME, null, null);
        }

        private static void create(SQLiteDatabase db) {
            db.execSQL(ELECTION_TABLE_CREATE);
        }

        private static void upgrade(SQLiteDatabase db) {
            drop(db);
        }

        // Prevent instantiation.
        private Election() { throw new AssertionError("Non-instantiable class"); }
    }

    // TODO: This is not used at all for now. Leave it or not?
    static final class Option {
        public static final String TABLE_NAME = "Option";
        public static final String COLUMN_NAME_ELECTION_ID = "electionId";
        public static final String COLUMN_NAME_OPTION_INDEX = "optionIndex";
        public static final String COLUMN_NAME_OPTION = "option";

        private static final String OPTION_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_NAME_ELECTION_ID + " VARCHAR(36) NOT NULL " +
                    "CONSTRAINT optionElectionID_fk " +
                    "REFERENCES " + Election.TABLE_NAME+"("+Election.COLUMN_NAME_ELECTION_ID+") " +
                    "ON DELETE CASCADE," +
                COLUMN_NAME_OPTION_INDEX + " INTEGER NOT NULL," +
                COLUMN_NAME_OPTION + " VARCHAR NOT NULL," +
                "CONSTRAINT optionPK PRIMARY KEY (electionID, optionIndex)" +
            ")";

        private static void drop(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        }

        private static void truncate(SQLiteDatabase db) {
            db.delete(TABLE_NAME, null, null);
        }

        private static void create(SQLiteDatabase db) {
            db.execSQL(OPTION_TABLE_CREATE);
        }

        private static void upgrade(SQLiteDatabase db) {
            drop(db);
        }

        // Prevent instantiation.
        private Option() { throw new AssertionError("Non-instantiable class"); }
    }

    static final class BallotPart {
        public static final String TABLE_NAME = "BallotPart";
        public static final String COLUMN_NAME_BALLOT_PART_ID = "ballotPartId";
        public static final String COLUMN_NAME_ELECTION_ID = "electionId";
        public static final String COLUMN_NAME_SERIAL_NO = "serialNo";
        public static final String COLUMN_NAME_PART = "part";
        public static final String COLUMN_NAME_VOTE_CODE = "voteCode";
        public static final String COLUMN_NAME_DECOMMITMENT = "decommitment";

        private static final String BALLOT_PART_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_NAME_BALLOT_PART_ID + " INTEGER PRIMARY KEY," +
                COLUMN_NAME_ELECTION_ID + " VARCHAR(36) NOT NULL " +
                    "CONSTRAINT ballot_part_election_id_fkey " +
                    "REFERENCES " + Election.TABLE_NAME+"("+ Election.COLUMN_NAME_ELECTION_ID+") " +
                    "ON DELETE CASCADE," +
                COLUMN_NAME_SERIAL_NO + " VARCHAR NOT NULL," +
                COLUMN_NAME_PART + " TEXT CHECK(part IN ('A', 'B')) NOT NULL," +
                COLUMN_NAME_VOTE_CODE + " VARCHAR NOT NULL," +
                COLUMN_NAME_DECOMMITMENT + " VARCHAR NOT NULL," +
                "CONSTRAINT ballot_part_ukey UNIQUE " + String.format(
                    "(%s,%s,%s,%s)",
                    COLUMN_NAME_ELECTION_ID, COLUMN_NAME_SERIAL_NO, COLUMN_NAME_PART,
                    COLUMN_NAME_VOTE_CODE) +
            ")";

        private static void drop(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        }

        private static void truncate(SQLiteDatabase db) {
            db.delete(TABLE_NAME, null, null);
        }

        private static void create(SQLiteDatabase db) {
            db.execSQL(BALLOT_PART_TABLE_CREATE);
        }

        private static void upgrade(SQLiteDatabase db) {
            drop(db);
        }

        // Prevent instantiation.
        private BallotPart() { throw new AssertionError("Non-instantiable class"); }
    }

    public static final class ElectionDynamicData {
        public static final String TABLE_NAME = "ElectionDynamicData";
        public static final String COLUMN_NAME_ELECTION_ID = "electionId";
        public static final String COLUMN_NAME_BALLOT_COUNT = "ballotCount";
        public static final String COLUMN_NAME_DECOMMITMENT_BUNDLE = "decommitmentBundle";

        private static final String ELECTION_DYNAMIC_DATA_TABLE_CREATE =
            "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                COLUMN_NAME_ELECTION_ID + " VARCHAR(36) PRIMARY KEY NOT NULL " +
                    "CONSTRAINT election_dynamic_data_election_id_fk " +
                    "REFERENCES " + Election.TABLE_NAME+"("+ Election.COLUMN_NAME_ELECTION_ID+") " +
                    "ON DELETE CASCADE," +
                COLUMN_NAME_DECOMMITMENT_BUNDLE + " VARCHAR NOT NULL" +
            ")";

        private static void drop(SQLiteDatabase db) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        }

        private static void truncate(SQLiteDatabase db) {
            db.delete(TABLE_NAME, null, null);
        }

        private static void create(SQLiteDatabase db) {
            db.execSQL(ELECTION_DYNAMIC_DATA_TABLE_CREATE);
        }

        private static void upgrade(SQLiteDatabase db) {
            drop(db);
        }

        // Prevent instantiation.
        private ElectionDynamicData() { throw new AssertionError("Non-instantiable class"); }
    }

}
