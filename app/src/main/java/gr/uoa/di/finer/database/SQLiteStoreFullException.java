package gr.uoa.di.finer.database;

import gr.uoa.di.finer.service.StoreFullException;

/**
 * Thrown to indicate that the SQLite database used is full.
 *
 * @author Vasilis Poulimenos
 */
class SQLiteStoreFullException extends StoreFullException {

    /**
     * Constructs a new {@code SQLiteStoreFullException} with a default detail message.
     */
    SQLiteStoreFullException() {
        super();
    }

    /**
     * Constructs a new {@code SQLiteStoreFullException} with a default detail message and
     * specified cause.
     *
     * @param cause the cause of this exception.
     */
    SQLiteStoreFullException(Throwable cause) {
        super(cause);
    }

}
