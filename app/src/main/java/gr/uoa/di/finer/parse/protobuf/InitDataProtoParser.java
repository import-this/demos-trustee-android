package gr.uoa.di.finer.parse.protobuf;

import android.util.Log;

import com.google.protobuf.InvalidProtocolBufferException;

import java.io.IOException;
import java.io.InputStream;

import gr.uoa.di.finer.service.InitDataParser;
import gr.uoa.di.finer.service.ParseException;
import gr.uoa.di.finer.service.StoreException;
import gr.uoa.di.finer.parse.EmptyFileException;
import gr.uoa.di.finer.parse.TruncatedFileException;
import gr.uoa.di.finer.service.WritableDataStore;
import gr.uoa.di.finer.parse.ZeroLengthKeyException;
import gr.uoa.di.finer.parse.protobuf.EAMessages.Ballot;
import gr.uoa.di.finer.parse.protobuf.EAMessages.Key;

/**
 * A parser for the initialization data, encoded in Google Protobuf format.
 * Each InitDataProtoParser may be used to read a single stream.
 * Instances of this class are NOT thread-safe.
 *
 * @author Vasilis Poulimenos
 */
public class InitDataProtoParser implements InitDataParser {

    private static final String TAG = InitDataProtoParser.class.getName();

    private final InputStream input;
    private final WritableDataStore store;
    private final String electionId;
    private long ballotCount;

    public InitDataProtoParser(InputStream stream, WritableDataStore store, String electionId) {
        this.input = stream;
        this.store = store;
        this.electionId = electionId;
        this.ballotCount = 0;
    }

    /**
     *
     * @throws IOException
     * @throws ParseException if the Protobuf file format is incorrect
     * @throws EmptyFileException if the file is empty
     * @throws ZeroLengthKeyException if the key is empty
     * @throws StoreException
     */
    @Override
    public void parseKey() throws IOException, ParseException, StoreException {
        try {
            final Key key = Key.parseDelimitedFrom(input);
            final String decommitmentKey;

            // Oops! The user gave us an empty file!
            if (key == null) {
                throw new EmptyFileException();
            }
            decommitmentKey = key.getDecommitmentKey();
            if (decommitmentKey.isEmpty()) {
                throw new ZeroLengthKeyException();
            }

            store.saveKey(electionId, decommitmentKey);
        } catch (InvalidProtocolBufferException e) {
            final String msg = "Protocol Buffer invalid key format";
            Log.e(TAG, msg, e);
            throw new ProtobufParseException(msg, e);
        }
    }

    private void storeBallotPart(Ballot.Side part, String serialNo) throws StoreException {
        for (Ballot.Side.VoteCodeTuple tuple: part.getVoteCodeTuplesList()) {
            store.saveBallot(
                electionId,
                serialNo,
                part.getID(),
                tuple.getVoteCode(),
                tuple.getDecommitment());
        }
    }

    /**
     *
     * @return true if a ballot was parsed or false if the end of the stream was reached
     * @throws IOException
     * @throws ParseException if the Protobuf file format is incorrect
     * @throws TruncatedFileException if the file is incomplete
     * @throws StoreException
     */
    @Override
    public boolean parseBallot() throws IOException, ParseException, StoreException {
        try {
            final Ballot ballot = Ballot.parseDelimitedFrom(input);

            // Oops! No ballots!
            if (ballotCount == 0 && ballot == null) {
                throw new TruncatedFileException("No ballots in data file");
            }
            // EOF
            if (ballot == null) {
                return false;
            }
            // Save part A.
            storeBallotPart(ballot.getPartA(), ballot.getSerialNumber());
            // Save part B.
            storeBallotPart(ballot.getPartB(), ballot.getSerialNumber());
        } catch (InvalidProtocolBufferException e) {
            final String msg = "Protocol Buffer invalid ballot format";
            Log.e(TAG, msg, e);
            throw new ProtobufParseException(msg, e);
        }
        ++ballotCount;
        return true;
    }

    /**
     * Returns the number of ballots that have been parsed so far.
     * @return the number of parsed ballots
     */
    @Override
    public long getParsedBallotCount() {
        return ballotCount;
    }

}
