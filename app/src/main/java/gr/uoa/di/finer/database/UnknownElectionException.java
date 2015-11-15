package gr.uoa.di.finer.database;

/**
 * Thrown to indicate that a non-existent election was specified.
 *
 * @author Vasilis Poulimenos
 */
public class UnknownElectionException extends IllegalArgumentException {

    /**
     * Constructs a new {@code UnknownElectionException} with the specified election ID.
     *
     * @param electionId the election ID that was specified for this exception.
     */
    public UnknownElectionException(String electionId) {
        super("Election ID: " + electionId);
    }

}
