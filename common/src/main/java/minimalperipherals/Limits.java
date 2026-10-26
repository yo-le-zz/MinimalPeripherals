package minimalperipherals;

/** Fixed limits (the original mod exposed these through a config file). */
public final class Limits {
    /** Maximum size of the data stored on an NFC card / RFID badge. */
    public static final int NFC_MAX_DATA_SIZE = 128;
    /** Range of the RFID scanner, in blocks. */
    public static final int RFID_SCAN_RANGE = 8;

    /** Maximum size of the buffer returned by cryptographic_accelerator.randomBytes. */
    public static final int RANDOM_BYTES_MAX_SIZE = 65536;
    /** Maximum output size of blake3, in bytes. */
    public static final int BLAKE3_MAX_OUTPUT = 4096;
    /** Maximum output size of the key derivation functions (pbkdf2, hkdf, scrypt), in bytes. */
    public static final int KDF_MAX_OUTPUT = 1024;
    /** Maximum iteration count of pbkdf2. */
    public static final int PBKDF2_MAX_ITERATIONS = 1_000_000;
    /** Maximum scrypt cost parameters. */
    public static final int SCRYPT_MAX_N = 1 << 15;
    public static final int SCRYPT_MAX_R = 8;
    public static final int SCRYPT_MAX_P = 4;
    /** Maximum size of the data produced by inflate (protects against decompression bombs). */
    public static final int INFLATE_MAX_SIZE = 8 * 1024 * 1024;

    private Limits() {
    }
}
