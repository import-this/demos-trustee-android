package gr.uoa.di.finer.service;

import android.support.annotation.WorkerThread;

import java.io.IOException;

/**
 *
 * @author Vasilis Poulimenos
 */
@WorkerThread
public interface InitDataParser {

    /**
     *
     * @throws IOException
     * @throws ParseException
     * @throws StoreException
     */
    void parseKey() throws IOException, ParseException, StoreException;

    /**
     *
     * @return
     * @throws IOException
     * @throws ParseException
     * @throws StoreException
     */
    boolean parseBallot() throws IOException, ParseException, StoreException;

    /**
     * Returns the number of ballots that have been parsed so far.
     * @return the number of parsed ballots
     */
    long getParsedBallotCount();

}
