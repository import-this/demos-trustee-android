package gr.uoa.di.finer.service;

import android.support.annotation.WorkerThread;

import java.io.IOException;

/**
 *
 * @author Vasilis Poulimenos
 */
@WorkerThread
public interface ResponseParser {

    /**
     * Parses the next valid ballot.
     *
     * @return
     * @throws IOException
     * @throws ParseException
     * @throws StoreException
     */
    boolean parse() throws IOException, ParseException, StoreException;

    /**
     * Returns the number of valid ballots that have been parsed so far.
     * @return the number of valid parsed ballots
     */
    long getParsedBallotCount();

}
