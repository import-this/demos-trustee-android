package gr.uoa.di.finer.parse;

import android.support.annotation.WorkerThread;

import java.io.IOException;
import java.io.Reader;

import gr.uoa.di.finer.service.Cryptosystem;
import gr.uoa.di.finer.service.ParseException;
import gr.uoa.di.finer.ReadableDataStore;
import gr.uoa.di.finer.service.ResponseParser;
import gr.uoa.di.finer.service.StoreException;

/**
 * A simple efficient stream parser for the server response.
 * Each SimpleResponseParser may be used to read a single stream.
 * Instances of this class are NOT thread-safe.
 *
 * TODO: Come up with a good scheme that deals with faulty/malicious servers.
 *
 * @author Vasilis Poulimenos
 */
@WorkerThread
public class SimpleResponseParser implements ResponseParser {

    private static final String TAG = SimpleResponseParser.class.getName();

    private static final int CAPACITY_ESTIMATE = 64;
    private static final int MAX_STRING_SIZE = 2048;

    private final Reader reader;
    private final Cryptosystem cryptosystem;
    private final ReadableDataStore store;
    private final String electionId;
    private final StringBuilder builder;
    private long ballotCount;
    private int lookahead;

    /**
     *
     * @param reader
     * @param cryptosystem
     * @param store
     * @param electionId
     */
    public SimpleResponseParser(Reader reader, Cryptosystem cryptosystem,
                                ReadableDataStore store, String electionId) {
        this.reader = reader;
        this.cryptosystem = cryptosystem;
        this.store = store;
        this.electionId = electionId;
        this.builder = new StringBuilder(CAPACITY_ESTIMATE);
        this.ballotCount = 0;
    }

    private boolean isAsciiDigit(int ch) {
        return ch >= '0' && ch <= '9';
    }

    private boolean isAsciiUppercaseLetterOrDigit(int ch) {
        return (ch >= 'A' && ch <= 'Z') || isAsciiDigit(ch);
    }

    private void checkSerialNoCh(int ch) throws InvalidTokenException {
        if (!isAsciiDigit(ch)) {
            throw new InvalidTokenException(builder.toString());
        }
    }

    private void checkVoteCodeCh(int ch) throws InvalidTokenException {
        if (!isAsciiUppercaseLetterOrDigit(ch)) {
            throw new InvalidTokenException(builder.toString());
        }
    }

    private String parseSerialNo() throws IOException, ParseException {
        builder.setLength(0);
        while (lookahead != ' ') {
            if (lookahead == -1) {
                throw new EOFException();
            }
            checkSerialNoCh(lookahead);
            builder.append((char) lookahead);
            if (builder.length() > MAX_STRING_SIZE) {
                throw new TooLongTokenException(builder.toString());
            }
            lookahead = reader.read();
        }
        if (builder.length() == 0) {
            throw new EmptyTokenException("serialNo");
        }
        return builder.toString();
    }

    private String parseVoteCode() throws IOException, ParseException {
        builder.setLength(0);
        while (lookahead != '\n') {
            if (lookahead == -1) {
                throw new EOFException();
            }
            checkVoteCodeCh(lookahead);
            builder.append((char) lookahead);
            if (builder.length() > MAX_STRING_SIZE) {
                throw new TooLongTokenException(builder.toString());
            }
            lookahead = reader.read();
        }
        if (builder.length() == 0) {
            throw new EmptyTokenException("voteCode");
        }
        return builder.toString();
    }

    /**
     *
     * @return
     * @throws IOException
     * @throws EOFException
     * @throws TooLongTokenException
     * @throws StoreException
     */
    private String parseLine() throws IOException, ParseException, StoreException {
        final String serialNo = parseSerialNo();
        lookahead = reader.read();                  // Skip <space>.
        final String voteCode = parseVoteCode();
        return store.getBallotDecommitment(electionId, serialNo, voteCode);
    }

    /**
     *
     * Expected format: <serial no><space><vote code>\n
     * where:
     *      <serial no> is a series of numeric characters.
     *      <vote code> is a series of ASCII alphanumerical (uppercase) characters.
     *
     * @throws IOException
     * @throws ParseException
     * @throws StoreException
     * @return
     */
    @Override
    public boolean parse() throws IOException, ParseException, StoreException {
        String decommitment;

        do {    // until a valid ballot is found.
            if ((lookahead = reader.read()) == -1) {    // EOF
                return false;
            }
            decommitment = parseLine();
        } while (decommitment == null);
        cryptosystem.add(decommitment);
        ++ballotCount;
        return true;
    }

    /**
     * Returns the number of valid ballots that have been parsed so far.
     * @return the number of valid parsed ballots
     */
    @Override
    public long getParsedBallotCount() {
        return ballotCount;
    }

}
