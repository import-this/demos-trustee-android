package gr.uoa.di.finer.database;

import gr.uoa.di.finer.service.StoreException;

/**
 * Thrown to indicate that an error occurred while attempting to use a SQLite database.
 *
 * @author Vasilis Poulimenos
 */
class SQLiteStoreException extends StoreException {

    /**
     * Constructs a new {@code SQLiteStoreException} with the specified detail message.
     *
     * @param message the detail message for this exception.
     */
    SQLiteStoreException(String message) {
        super(message);
    }

    /**
     * Constructs a new {@code SQLiteStoreException} with the specified detail message and cause.
     *
     * @param message the detail message for this exception.
     * @param cause the cause of this exception.
     */
    SQLiteStoreException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Constructs a new {@code SQLiteStoreException} with the specified cause.
     *
     * @param cause the cause of this exception.
     */
    SQLiteStoreException(Throwable cause) {
        super(cause);
    }

}
