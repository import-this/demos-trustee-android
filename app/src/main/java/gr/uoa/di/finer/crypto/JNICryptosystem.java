package gr.uoa.di.finer.crypto;

import android.support.annotation.WorkerThread;
import android.util.Log;

import gr.uoa.di.finer.BuildConfig;
import gr.uoa.di.finer.service.Cryptosystem;

/**
 * An efficient cryptosystem implemented with the Java Native Interface.
 * Instances of this class are NOT thread-safe.
 * *CRITICAL NOTE*:
 *      Due to current implementation restrictions, there must be only one instance of this class.
 *      (The C++ backend object is a singleton. I don't know why and I am too afraid to fix it.)
 *
 * @author Vasilis Poulimenos
 */
@WorkerThread
public class JNICryptosystem implements Cryptosystem {

    /*
     * http://developer.android.com/training/articles/perf-jni.html
     */

    static {
        // http://developer.android.com/ndk/guides/cpp-support.html#ic
        // If your app targets a version of Android earlier than Android 4.3 (Android API level 18),
        // and you use the shared library variant of a given C++ runtime, you must load the shared
        // library before any other library that depends on it.
        System.loadLibrary("gnustl_shared");
        System.loadLibrary("cryptosystem");
    }

    private static final String TAG = JNICryptosystem.class.getName();

    public static String toHex(String asciiValue) {
        char[] chars = asciiValue.toCharArray();

        StringBuilder hex = new StringBuilder();
        for (char character : chars) {
            hex.append(Integer.toHexString((int) character));
        }
        return hex.toString();
    }

    /**
     * Converts a byte array to a hex String.
     * @param bytes, the byte array to convert.
     * @return a hex representation of the byte array.
     */
    public static String bytesToHex(byte[] bytes) {
        final char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[bytes.length * 2];

        for ( int j = 0; j < bytes.length; j++ ) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }


    // Commitment functions
    private native void initializeCommitmentBundle(String hexCommitmentKey);
    private native void addToCommitmentBundle(String commitment);
    private native String finalizeCommitmentBundle();

    // Decommitment functions
    private native void initializeDecommitmentBundle(String hexDecommitmentKey);
    private native void addToDecommitmentBundle(String decommitment);
    private native String finalizeDecommitmentBundle();

    private void checkHex(String s) {
        for (int i = 0, len = s.length(); i < len; ++i) {
            char ch = s.charAt(i);
            if (!((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F'))) {
                throw new AssertionError("Non-hex string as key: " + s);
            }
        }
    }

    // TODO: Remove this when we are sure everything works.
    static {
        if (BuildConfig.DEBUG) {
            JNICryptosystem.test();
        }
    }

    // For test().
    private JNICryptosystem() {}

    /**
     *
     * @param hexDecommitmentKey
     */
    public JNICryptosystem(String hexDecommitmentKey) {
        checkHex(hexDecommitmentKey);
        initializeDecommitmentBundle(hexDecommitmentKey);
    }

    @Override
    public void add(String decommitment) {
        addToDecommitmentBundle(decommitment);
    }

    @Override
    public String computeBundle() {
        return finalizeDecommitmentBundle();
    }


    // Verify that the commitment bundle matches the decommitment bundle given the right key.
    private native boolean verifyCommitments(
        String commitmentBundle, String decommitmentBundle, String key);

    // Tally the decommitment bundle.
    private native String tally(String decommitmentBundle, int N, int m);

    private String decryptVoteCode(byte[] key, String encryptedVoteCode) {
        int whitespaceIndex;

        if ((whitespaceIndex = encryptedVoteCode.indexOf(' ')) == -1)
            throw new RuntimeException("Unable to find the whitespace character from the encrypted vote code!");
        return encryptedVoteCode.substring(0, whitespaceIndex);
    }

    public static void test() {
        String commitmentKey = "0;NwJEZB04crK2X6FHKrZSYo4hAA85ZD7i-1";
        String decommitmentKey = "0;t7a6D65Pn0CV4zzp46t3Gn8lRzcRFZuL-0";
        String commitmentValue = "RaYdfE3ztFluSVeH9yG2qUvh7cQsU+aY-1;pgPiXr3dfpz2KmdHWHO9Go9dWMxQXTtn-0";
        String decommitmentValue = "ADkx,SviHq3vl5pSwOfioVODie5yp1ipf5sHd";
        String verifyCommitmentBundle = "hjhoTQDJZH9BZA1M2qcbhCIhY/Su51Z4-1;RBLgjtrLPfm3LZSHyeEo2eUILN6UKRNg-1";
        String verifyDecommitmentBundle = "AnUb,ZIQYHDbuLA1b2gA6Owgbfwxwb2xG6OcZ";
        String verificationKey = "0;un9sDM4uN3XFtGgtCds3+cKNdmQqb6bL-1";
        String tallyDecommitmentBundle = "AAAB,7bVMstb7Ac7iQ7aUHQroZ/XKykRVX0JA";
        JNICryptosystem cryptomachine = new JNICryptosystem();

        Log.d(TAG, "Initializing commitment bundle...");
        cryptomachine.initializeCommitmentBundle(JNICryptosystem.toHex(commitmentKey).toUpperCase());

        for (int i = 0; i < 10; ++i)
            cryptomachine.addToCommitmentBundle(commitmentValue);
        String commitmentBundle = cryptomachine.finalizeCommitmentBundle();
        Log.d(TAG, "Commitment bundle: " + commitmentBundle);

        Log.d(TAG, "Initializing decommitment bundle...");
        cryptomachine.initializeDecommitmentBundle(
                JNICryptosystem.toHex(decommitmentKey).toUpperCase());
        for (int i = 0; i < 10; ++i)
            cryptomachine.addToDecommitmentBundle(decommitmentValue);
        String decommitmentBundle = cryptomachine.finalizeDecommitmentBundle();
        Log.d(TAG, "Decommitment bundle: " + decommitmentBundle);

        Log.d(TAG, "Commitment verification: " + Boolean.toString(cryptomachine.verifyCommitments(
                verifyCommitmentBundle,
                verifyDecommitmentBundle,
                JNICryptosystem.toHex(verificationKey).toUpperCase())));

        String voteTally = cryptomachine.tally(tallyDecommitmentBundle, 10, 6);
        Log.d(TAG, "Vote tally: " + voteTally);
    }

}
